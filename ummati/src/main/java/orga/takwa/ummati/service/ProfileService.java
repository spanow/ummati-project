package orga.takwa.ummati.service;

import orga.takwa.ummati.dto.profile.*;
import orga.takwa.ummati.entity.EventSignup;
import orga.takwa.ummati.entity.Organization;
import orga.takwa.ummati.entity.Skill;
import orga.takwa.ummati.entity.User;
import orga.takwa.ummati.entity.enums.MembershipRole;
import orga.takwa.ummati.entity.enums.MembershipStatus;
import orga.takwa.ummati.entity.enums.SkillCategory;
import orga.takwa.ummati.entity.enums.SignupStatus;
import orga.takwa.ummati.exception.BusinessRuleException;
import orga.takwa.ummati.exception.ForbiddenException;
import orga.takwa.ummati.exception.ResourceNotFoundException;
import orga.takwa.ummati.repository.*;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final MembershipRepository membershipRepository;
    private final EventSignupRepository eventSignupRepository;
    private final PasswordEncoder passwordEncoder;
    private final ImageService imageService;

    public ProfileService(UserRepository userRepository, SkillRepository skillRepository,
                          MembershipRepository membershipRepository, EventSignupRepository eventSignupRepository,
                          PasswordEncoder passwordEncoder, ImageService imageService) {
        this.userRepository = userRepository;
        this.skillRepository = skillRepository;
        this.membershipRepository = membershipRepository;
        this.eventSignupRepository = eventSignupRepository;
        this.passwordEncoder = passwordEncoder;
        this.imageService = imageService;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(UUID userId) {
        User user = findUser(userId);
        return toProfileResponse(user);
    }

    @Transactional
    public ProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findUser(userId);

        if (request.firstName() != null) user.setFirstName(request.firstName().trim());
        if (request.lastName() != null) user.setLastName(request.lastName().trim());
        if (request.phone() != null) user.setPhone(request.phone());
        if (request.dateOfBirth() != null) user.setDateOfBirth(request.dateOfBirth());
        if (request.bio() != null) user.setBio(request.bio());
        if (request.address() != null) {
            var addr = request.address();
            if (addr.street() != null) user.setAddressStreet(addr.street());
            if (addr.city() != null) user.setAddressCity(addr.city());
            if (addr.zip() != null) user.setAddressZip(addr.zip());
            if (addr.country() != null) user.setAddressCountry(addr.country());
        }

        // Skills
        if (request.skillIds() != null) {
            Set<Skill> skills = new HashSet<>(skillRepository.findAllById(request.skillIds()));
            user.setSkills(skills);
        }
        if (request.newSkills() != null) {
            for (String skillName : request.newSkills()) {
                Skill skill = skillRepository.findByNameIgnoreCase(skillName.trim())
                        .orElseGet(() -> {
                            Skill s = new Skill();
                            s.setName(skillName.trim());
                            s.setCategory(SkillCategory.AUTRE);
                            return skillRepository.save(s);
                        });
                user.getSkills().add(skill);
            }
        }

        user = userRepository.save(user);
        return toProfileResponse(user);
    }

    @Transactional
    public String uploadPhoto(UUID userId, MultipartFile file) throws IOException {
        User user = findUser(userId);
        // La validation (taille, type réel déduit des octets) et le stockage sont centralisés
        // dans ImageService — même traitement que les logos, bannières et couvertures.
        user.setPhotoUrl(imageService.replace(file, "users/" + userId, user.getPhotoUrl()));
        userRepository.save(user);
        return user.getPhotoUrl();
    }

    @Transactional
    public void deleteAccount(UUID userId, String password) {
        User user = findUser(userId);
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessRuleException("Mot de passe incorrect");
        }

        // Check if last admin of any org
        var activeMemberships = membershipRepository.findByUserIdAndStatus(
                userId, MembershipStatus.ACTIVE, Pageable.unpaged());
        for (var m : activeMemberships) {
            if (m.getRole() == MembershipRole.ADMIN) {
                long adminCount = membershipRepository.countByOrganizationIdAndRoleAndStatus(
                        m.getOrganization().getId(),
                        MembershipRole.ADMIN,
                        MembershipStatus.ACTIVE);
                if (adminCount <= 1) {
                    throw new BusinessRuleException(
                            "Vous êtes le dernier admin de l'ONG '" + m.getOrganization().getName()
                            + "'. Transférez le rôle admin avant de supprimer votre compte.");
                }
            }
            m.setStatus(MembershipStatus.LEFT);
            membershipRepository.save(m);
        }

        // Anonymize
        user.setFirstName("Utilisateur");
        user.setLastName("Supprimé");
        user.setEmail("deleted_" + userId + "@ummati.org");
        user.setPhone(null);
        user.setBio(null);
        user.setPhotoUrl(null);
        user.setAddressStreet(null);
        user.setAddressCity(null);
        user.setAddressZip(null);
        user.setEnabled(false);
        user.getSkills().clear();
        userRepository.save(user);
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = findUser(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException("Mot de passe actuel incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException("Le nouveau mot de passe doit être différent de l'ancien");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Transactional
    public ProfileResponse onboarding(UUID userId, OnboardingRequest request) {
        User user = findUser(userId);
        if (request.bio() != null) user.setBio(request.bio());
        user.setAddressCity(request.city());
        if (request.skillIds() != null && !request.skillIds().isEmpty()) {
            user.setSkills(new HashSet<>(skillRepository.findAllById(request.skillIds())));
        }
        user.setOnboardingDone(true);
        user = userRepository.save(user);
        return toProfileResponse(user);
    }

    /**
     * T-143 — RGPD : export JSON complet des données utilisateur (droit de portabilité).
     */
    @Transactional(readOnly = true)
    public Map<String, Object> exportProfile(UUID userId) {
        User user = findUser(userId);

        // Basic info
        Map<String, Object> basic = new LinkedHashMap<>();
        basic.put("id", user.getId());
        basic.put("email", user.getEmail());
        basic.put("firstName", user.getFirstName());
        basic.put("lastName", user.getLastName());
        basic.put("phone", user.getPhone());
        basic.put("dateOfBirth", user.getDateOfBirth());
        basic.put("bio", user.getBio());
        basic.put("addressCity", user.getAddressCity());
        basic.put("addressCountry", user.getAddressCountry());
        basic.put("role", user.getRole());
        basic.put("emailVerified", user.isEmailVerified());
        basic.put("onboardingDone", user.isOnboardingDone());
        basic.put("createdAt", user.getCreatedAt());

        // Skills
        List<Map<String, Object>> skills = user.getSkills().stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("name", s.getName());
            m.put("category", s.getCategory());
            return m;
        }).toList();

        // Memberships
        var memberships = membershipRepository.findByUserIdAndStatus(userId, MembershipStatus.ACTIVE, Pageable.unpaged())
                .stream().map(mem -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("organizationName", mem.getOrganization().getName());
                    m.put("role", mem.getRole());
                    m.put("status", mem.getStatus());
                    m.put("joinedAt", mem.getJoinedAt());
                    return m;
                }).toList();

        // Signups
        var signups = eventSignupRepository.findByUserId(userId, Pageable.unpaged())
                .stream().map(sig -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("eventTitle", sig.getEvent().getTitle());
                    m.put("eventDate", sig.getEvent().getStartDate());
                    m.put("status", sig.getStatus());
                    m.put("registeredAt", sig.getRegisteredAt());
                    return m;
                }).toList();

        Map<String, Object> export = new LinkedHashMap<>();
        export.put("exportedAt", java.time.LocalDateTime.now());
        export.put("profile", basic);
        export.put("skills", skills);
        export.put("memberships", memberships);
        export.put("signups", signups);
        return export;
    }

    // --- Passeport bénévole ---

    /**
     * Passeport du bénévole connecté : toujours accessible, nom complet, et indique
     * l'état de l'option de publication.
     */
    @Transactional(readOnly = true)
    public VolunteerPassport getOwnPassport(UUID userId) {
        return buildPassport(findUser(userId), false);
    }

    /**
     * Passeport public d'un bénévole.
     *
     * @throws ResourceNotFoundException si le bénévole n'a pas activé la publication, ou si
     *         son compte est désactivé — un 404 plutôt qu'un 403 pour ne pas révéler
     *         l'existence d'un profil non publié.
     */
    @Transactional(readOnly = true)
    public VolunteerPassport getPublicPassport(UUID userId) {
        User user = findUser(userId);
        if (!user.isProfilePublic() || !user.isEnabled()) {
            throw new ResourceNotFoundException("Profil non trouvé");
        }
        return buildPassport(user, true);
    }

    /** Active ou désactive la visibilité publique du passeport. */
    @Transactional
    public VolunteerPassport setPassportVisibility(UUID userId, boolean makePublic) {
        User user = findUser(userId);
        user.setProfilePublic(makePublic);
        userRepository.save(user);
        return buildPassport(user, false);
    }

    private VolunteerPassport buildPassport(User user, boolean publicView) {
        List<EventSignup> attended = eventSignupRepository.findAttendedWithEventByUserId(user.getId());

        // Causes : domaines des ONG pour lesquelles le bénévole a effectivement participé,
        // pas celles où il est simplement inscrit — l'engagement se mesure aux missions faites.
        Map<String, Integer> missionsByDomain = new LinkedHashMap<>();
        Set<UUID> orgIds = new HashSet<>();
        for (EventSignup signup : attended) {
            Organization org = signup.getEvent().getOrganization();
            orgIds.add(org.getId());
            missionsByDomain.merge(org.getDomain().name(), 1, Integer::sum);
        }

        List<VolunteerPassport.CauseDto> causes = missionsByDomain.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(e -> new VolunteerPassport.CauseDto(e.getKey(), e.getValue()))
                .toList();

        List<VolunteerPassport.MissionDto> recentMissions = attended.stream()
                .sorted(Comparator.comparing(
                        (EventSignup s) -> missionDate(s),
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .map(s -> new VolunteerPassport.MissionDto(
                        s.getEvent().getId(),
                        s.getEvent().getTitle(),
                        s.getEvent().getOrganization().getName(),
                        s.getEvent().getOrganization().getSlug(),
                        missionDate(s)))
                .toList();

        List<VolunteerPassport.SkillDto> skills = user.getSkills().stream()
                .map(s -> new VolunteerPassport.SkillDto(s.getId(), s.getName(), s.getCategory().name()))
                .toList();

        String displayedLastName = publicView ? initial(user.getLastName()) : user.getLastName();

        return new VolunteerPassport(
                user.getId(), user.getFirstName(), displayedLastName,
                user.getPhotoUrl(), user.getBio(), user.getAddressCity(),
                user.getCreatedAt(),
                attended.size(), computeVolunteerHours(user.getId()), orgIds.size(),
                skills, causes, recentMissions, user.isProfilePublic());
    }

    /** Date effective d'une mission : celle du créneau réservé, sinon celle de l'événement. */
    private LocalDateTime missionDate(EventSignup signup) {
        return signup.getOccurrence() != null
                ? signup.getOccurrence().getStartDate()
                : signup.getEvent().getStartDate();
    }

    private String initial(String lastName) {
        if (lastName == null || lastName.isBlank()) {
            return "";
        }
        return lastName.trim().substring(0, 1).toUpperCase() + ".";
    }

    // --- Mapping ---

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
    }

    private ProfileResponse toProfileResponse(User user) {
        var skills = user.getSkills().stream()
                .map(s -> new ProfileResponse.SkillDto(s.getId(), s.getName(), s.getCategory().name()))
                .toList();

        long orgCount = membershipRepository.countByUserIdAndStatus(user.getId(), MembershipStatus.ACTIVE);
        long attendedCount = eventSignupRepository.countByUserIdAndStatus(user.getId(), SignupStatus.ATTENDED);
        double volunteerHours = computeVolunteerHours(user.getId());

        return new ProfileResponse(
                user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                user.getPhone(), user.getDateOfBirth(), user.getPhotoUrl(), user.getBio(),
                new ProfileResponse.AddressDto(user.getAddressStreet(), user.getAddressCity(),
                        user.getAddressZip(), user.getAddressCountry()),
                skills,
                new ProfileResponse.StatsDto(orgCount, attendedCount, volunteerHours),
                user.isOnboardingDone(), user.isEmailVerified(), user.getCreatedAt(),
                user.isProfilePublic()
        );
    }

    /**
     * Total des heures de bénévolat, arrondi à la demi-heure.
     *
     * <p>Priorité aux heures certifiées par l'ONG ({@code hours_validated}) : ce sont les
     * seules qui font foi pour une attestation. Depuis la validation de présence, elles
     * sont systématiquement renseignées ; le repli sur la durée du créneau ne sert donc
     * qu'aux présences enregistrées avant cette bascule, qui seraient sinon comptées à
     * zéro heure.
     */
    private double computeVolunteerHours(UUID userId) {
        double totalMinutes = 0;
        for (EventSignup signup : eventSignupRepository.findAttendedWithEventByUserId(userId)) {
            if (signup.getHoursValidated() != null) {
                totalMinutes += signup.getHoursValidated().doubleValue() * 60;
                continue;
            }
            LocalDateTime start;
            LocalDateTime end;
            if (signup.getOccurrence() != null) {
                start = signup.getOccurrence().getStartDate();
                end = signup.getOccurrence().getEndDate();
            } else {
                start = signup.getEvent().getStartDate();
                end = signup.getEvent().getEndDate();
            }
            if (start != null && end != null) {
                long minutes = java.time.Duration.between(start, end).toMinutes();
                if (minutes > 0) {
                    totalMinutes += minutes;
                }
            }
        }
        return Math.round(totalMinutes / 30.0) / 2.0;
    }
}

