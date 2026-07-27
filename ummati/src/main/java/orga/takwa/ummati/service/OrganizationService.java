package orga.takwa.ummati.service;

import orga.takwa.ummati.dto.organization.*;
import orga.takwa.ummati.entity.*;
import orga.takwa.ummati.entity.enums.*;
import orga.takwa.ummati.exception.*;
import orga.takwa.ummati.repository.*;
import orga.takwa.ummati.util.SlugUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final EventRepository eventRepository;
    private final ImageService imageService;

    public OrganizationService(OrganizationRepository organizationRepository, UserRepository userRepository,
                               MembershipRepository membershipRepository, NotificationService notificationService,
                               AuditService auditService, EventRepository eventRepository,
                               ImageService imageService) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.eventRepository = eventRepository;
        this.imageService = imageService;
    }

    // T-046: Create organization
    @Transactional
    public OrganizationDetail create(UUID userId, CreateOrganizationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        if (!user.isEmailVerified()) {
            throw new ForbiddenException("Email non vérifié");
        }
        if (organizationRepository.existsByName(request.name())) {
            throw new ConflictException("Une organisation avec ce nom existe déjà");
        }

        String slug = SlugUtil.toSlug(request.name());
        String baseSlug = slug;
        int suffix = 1;
        while (organizationRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + suffix++;
        }

        Organization org = new Organization();
        org.setName(request.name().trim());
        org.setSlug(slug);
        org.setDescription(request.description());
        org.setMission(request.mission());
        org.setDomain(OrganizationDomain.valueOf(request.domain()));
        org.setAddressStreet(request.addressStreet());
        org.setAddressCity(request.addressCity());
        org.setAddressZip(request.addressZip());
        org.setAddressLat(request.addressLat());
        org.setAddressLng(request.addressLng());
        org.setEmail(request.email());
        org.setPhone(request.phone());
        org.setWebsite(request.website());
        org.setStatus(OrganizationStatus.PENDING);
        org.setCreatedBy(user);
        org = organizationRepository.save(org);
        final Organization savedOrg = org;

        // Auto membership ADMIN
        Membership membership = new Membership();
        membership.setUser(user);
        membership.setOrganization(savedOrg);
        membership.setRole(MembershipRole.ADMIN);
        membership.setStatus(MembershipStatus.ACTIVE);
        membership.setJoinedAt(LocalDateTime.now());
        membershipRepository.save(membership);

        notificationService.saveNotification(user, NotificationType.ONG_SUBMITTED,
                "ONG soumise", "Votre organisation '" + savedOrg.getName() + "' est en attente de validation.",
                "/organizations/" + savedOrg.getSlug());

        userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.PLATFORM_ADMIN)
                .forEach(admin -> notificationService.saveNotification(admin, NotificationType.ONG_SUBMITTED,
                        "Nouvelle ONG en attente", "L'organisation '" + savedOrg.getName() + "' attend validation.",
                        "/admin/organizations/" + savedOrg.getSlug() + "/validate"));

        auditService.log(userId, "ONG_CREATED", "Organization", savedOrg.getId());

        return toDetail(savedOrg);
    }

    // T-047: List organizations
    @Transactional(readOnly = true)
    public Page<OrganizationSummary> list(OrganizationDomain domain, String city, String search, Pageable pageable) {
        Page<Organization> page = organizationRepository.findAll(
                OrganizationSpecification.search(OrganizationStatus.ACTIVE, domain, city, search),
                pageable);
        return page.map(this::toSummary);
    }

    // T-048: Get by slug
    @Transactional(readOnly = true)
    public OrganizationDetail getBySlug(String slug) {
        Organization org = organizationRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation non trouvée"));
        return toDetail(org);
    }

    // T-049: Update organization
    @Transactional
    public OrganizationDetail update(UUID userId, UUID orgId, UpdateOrganizationRequest request) {
        Organization org = findOrg(orgId);
        verifyAdmin(userId, orgId);

        if (request.description() != null) org.setDescription(request.description());
        if (request.mission() != null) org.setMission(request.mission());
        if (request.domain() != null) org.setDomain(OrganizationDomain.valueOf(request.domain()));
        if (request.addressStreet() != null) org.setAddressStreet(request.addressStreet());
        if (request.addressCity() != null) org.setAddressCity(request.addressCity());
        if (request.addressZip() != null) org.setAddressZip(request.addressZip());
        if (request.addressLat() != null) org.setAddressLat(request.addressLat());
        if (request.addressLng() != null) org.setAddressLng(request.addressLng());
        if (request.email() != null) org.setEmail(request.email());
        if (request.phone() != null) org.setPhone(request.phone());
        if (request.website() != null) org.setWebsite(request.website());

        org = organizationRepository.save(org);
        auditService.log(userId, "ONG_UPDATED", "Organization", orgId);
        return toDetail(org);
    }

    // T-050: Change status (PLATFORM_ADMIN)
    @Transactional
    public OrganizationDetail changeStatus(UUID adminId, UUID orgId, OrganizationStatusRequest request) {
        Organization org = findOrg(orgId);
        OrganizationStatus newStatus = OrganizationStatus.valueOf(request.status());

        if (newStatus == OrganizationStatus.ACTIVE) {
            org.setStatus(OrganizationStatus.ACTIVE);
            org.setValidatedBy(userRepository.getReferenceById(adminId));
            org.setValidatedAt(LocalDateTime.now());
            notificationService.saveNotification(org.getCreatedBy(), NotificationType.ONG_VALIDATED,
                    "ONG validée !", "Votre organisation '" + org.getName() + "' a été validée.",
                    "/organizations/" + org.getSlug());
            auditService.log(adminId, "ONG_VALIDATED", "Organization", orgId);
        } else if (newStatus == OrganizationStatus.REJECTED
                || newStatus == OrganizationStatus.SUSPENDED
                || newStatus == OrganizationStatus.ARCHIVED) {
            if (request.reason() == null || request.reason().isBlank() || request.reason().length() < 20) {
                throw new BusinessRuleException("Le motif doit faire au moins 20 caractères");
            }
            org.setStatus(newStatus);
            org.setRejectionReason(request.reason());
            String notifTitle = switch (newStatus) {
                case REJECTED -> "ONG rejetée";
                case SUSPENDED -> "ONG suspendue";
                default -> "ONG archivée";
            };
            notificationService.saveNotification(org.getCreatedBy(), NotificationType.ONG_REJECTED,
                    notifTitle, "Motif : " + request.reason(), "/organizations/" + org.getSlug());
            auditService.log(adminId, "ONG_" + newStatus.name(), "Organization", orgId);
        }

        org = organizationRepository.save(org);
        return toDetail(org);
    }

    // --- Visuels (logo & bannière) ---

    /** Remplace le logo de l'ONG. Réservé aux admins de l'ONG. */
    @Transactional
    public String uploadLogo(UUID userId, UUID orgId, MultipartFile file) {
        verifyAdmin(userId, orgId);
        Organization org = findOrg(orgId);
        org.setLogoUrl(imageService.replace(file, "organizations/" + orgId, org.getLogoUrl()));
        organizationRepository.save(org);
        return org.getLogoUrl();
    }

    @Transactional
    public void deleteLogo(UUID userId, UUID orgId) {
        verifyAdmin(userId, orgId);
        Organization org = findOrg(orgId);
        imageService.deleteByPublicUrl(org.getLogoUrl());
        org.setLogoUrl(null);
        organizationRepository.save(org);
    }

    /** Remplace la bannière de l'ONG. Réservé aux admins de l'ONG. */
    @Transactional
    public String uploadBanner(UUID userId, UUID orgId, MultipartFile file) {
        verifyAdmin(userId, orgId);
        Organization org = findOrg(orgId);
        org.setBannerUrl(imageService.replace(file, "organizations/" + orgId, org.getBannerUrl()));
        organizationRepository.save(org);
        return org.getBannerUrl();
    }

    @Transactional
    public void deleteBanner(UUID userId, UUID orgId) {
        verifyAdmin(userId, orgId);
        Organization org = findOrg(orgId);
        imageService.deleteByPublicUrl(org.getBannerUrl());
        org.setBannerUrl(null);
        organizationRepository.save(org);
    }

    // --- Helpers ---

    public void verifyAdmin(UUID userId, UUID orgId) {
        Membership m = membershipRepository.findByUserIdAndOrganizationId(userId, orgId)
                .orElseThrow(() -> new ForbiddenException("Vous n'êtes pas membre de cette organisation"));
        if (m.getRole() != MembershipRole.ADMIN || m.getStatus() != MembershipStatus.ACTIVE) {
            throw new ForbiddenException("Vous n'êtes pas admin de cette organisation");
        }
    }

    Organization findOrg(UUID orgId) {
        return organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation non trouvée"));
    }

    OrganizationSummary toSummary(Organization organization) {
        long memberCount = membershipRepository.countByOrganizationIdAndStatus(
                organization.getId(), MembershipStatus.ACTIVE);
        String excerpt = organization.getDescription().length() > 150
                ? organization.getDescription().substring(0, 150) + "..."
                : organization.getDescription();
        return new OrganizationSummary(organization.getId(), organization.getName(), organization.getSlug(),
                organization.getDomain().name(), organization.getLogoUrl(), organization.getAddressCity(), memberCount, excerpt,
                organization.getStatus().name());
    }

    OrganizationDetail toDetail(Organization organization) {
        long memberCount = membershipRepository.countByOrganizationIdAndStatus(
                organization.getId(), MembershipStatus.ACTIVE);
        long eventCount = eventRepository.findByOrganizationId(organization.getId(),
                Pageable.unpaged()).getTotalElements();

        return new OrganizationDetail(organization.getId(), organization.getName(), organization.getSlug(),
                organization.getDescription(), organization.getMission(), organization.getDomain().name(),
                organization.getLogoUrl(), organization.getBannerUrl(),
                organization.getAddressStreet(), organization.getAddressCity(), organization.getAddressZip(), organization.getAddressCountry(),
                organization.getAddressLat(), organization.getAddressLng(),
                organization.getPhone(), organization.getEmail(), organization.getWebsite(),
                organization.getStatus().name(), organization.getRejectionReason(),
                new OrganizationDetail.StatsDto(memberCount, eventCount, null),
                organization.getCreatedAt());
    }
}
