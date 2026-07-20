package orga.takwa.ummati.service;

import orga.takwa.ummati.dto.membership.*;
import orga.takwa.ummati.entity.*;
import orga.takwa.ummati.entity.enums.*;
import orga.takwa.ummati.exception.*;
import orga.takwa.ummati.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class MembershipService {

    private final MembershipRepository membershipRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final OrganizationService organizationService;
    private final MembershipQuestionService membershipQuestionService;

    public MembershipService(MembershipRepository membershipRepository,
                             OrganizationRepository organizationRepository, UserRepository userRepository,
                             NotificationService notificationService, AuditService auditService,
                             OrganizationService organizationService,
                             MembershipQuestionService membershipQuestionService) {
        this.membershipRepository = membershipRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.organizationService = organizationService;
        this.membershipQuestionService = membershipQuestionService;
    }

    // T-051: Request membership
    @Transactional
    public MembershipResponse requestMembership(UUID userId, UUID orgId, MembershipRequest request) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation non trouvée"));
        if (org.getStatus() != OrganizationStatus.ACTIVE) {
            throw new BusinessRuleException("L'organisation n'est pas active");
        }

        var existing = membershipRepository.findByUserIdAndOrganizationId(userId, orgId);
        if (existing.isPresent()) {
            Membership m = existing.get();
            if (m.getStatus() == MembershipStatus.ACTIVE) {
                throw new ConflictException("Vous êtes déjà membre de cette organisation");
            }
            if (m.getStatus() == MembershipStatus.PENDING) {
                throw new ConflictException("Vous avez déjà une demande en cours");
            }
            if (m.getStatus() == MembershipStatus.REJECTED && m.getRejectedAt() != null
                    && m.getRejectedAt().plusDays(30).isAfter(LocalDateTime.now())) {
                throw new BusinessRuleException("Vous pourrez refaire une demande après le "
                        + m.getRejectedAt().plusDays(30).toLocalDate());
            }
            // Re-apply after cooldown: update existing
            m.setStatus(MembershipStatus.PENDING);
            m.setMotivation(request.motivation());
            m.setRejectedAt(null);
            m = membershipRepository.save(m);
            membershipQuestionService.saveAnswers(m, request.answers());
            notifyAdmins(org, userId);
            return toResponse(m);
        }

        User user = userRepository.getReferenceById(userId);
        Membership membership = new Membership();
        membership.setUser(user);
        membership.setOrganization(org);
        membership.setRole(MembershipRole.MEMBER);
        membership.setStatus(MembershipStatus.PENDING);
        membership.setMotivation(request.motivation());
        membership = membershipRepository.save(membership);
        membershipQuestionService.saveAnswers(membership, request.answers());

        notifyAdmins(org, userId);
        auditService.log(userId, "MEMBERSHIP_REQUESTED", "Membership", membership.getId());

        return toResponse(membership);
    }

    // T-052: Approve/Reject
    @Transactional
    public MembershipResponse handleAction(UUID adminUserId, UUID membershipId, MembershipActionRequest request) {
        Membership m = findMembership(membershipId);
        organizationService.verifyAdmin(adminUserId, m.getOrganization().getId());

        if (m.getStatus() != MembershipStatus.PENDING) {
            throw new BusinessRuleException("Cette demande n'est pas en attente");
        }

        String action = request.action().toUpperCase();
        if ("APPROVE".equals(action)) {
            m.setStatus(MembershipStatus.ACTIVE);
            m.setJoinedAt(LocalDateTime.now());
            notificationService.saveNotification(m.getUser(), NotificationType.MEMBERSHIP_ACCEPTED,
                    "Adhésion acceptée !", "Votre demande d'adhésion à '" + m.getOrganization().getName() + "' a été acceptée.",
                    "/organizations/" + m.getOrganization().getSlug());
            auditService.log(adminUserId, "MEMBERSHIP_APPROVED", "Membership", membershipId);
        } else if ("REJECT".equals(action)) {
            m.setStatus(MembershipStatus.REJECTED);
            m.setRejectedAt(LocalDateTime.now());
            notificationService.saveNotification(m.getUser(), NotificationType.MEMBERSHIP_REJECTED,
                    "Adhésion refusée", "Votre demande d'adhésion à '" + m.getOrganization().getName() + "' a été refusée.",
                    "/organizations/" + m.getOrganization().getSlug());
            auditService.log(adminUserId, "MEMBERSHIP_REJECTED", "Membership", membershipId);
        } else {
            throw new BusinessRuleException("Action invalide. Utilisez APPROVE ou REJECT.");
        }

        m = membershipRepository.save(m);
        return toResponse(m);
    }

    // T-053: Leave or exclude
    @Transactional
    public void removeMembership(UUID actorUserId, UUID membershipId) {
        Membership m = findMembership(membershipId);
        UUID orgId = m.getOrganization().getId();
        boolean isSelf = m.getUser().getId().equals(actorUserId);

        if (!isSelf) {
            organizationService.verifyAdmin(actorUserId, orgId);
            if (m.getRole() == MembershipRole.ADMIN) {
                throw new BusinessRuleException("Impossible d'exclure un admin. Rétrograder d'abord.");
            }
        }

        if (m.getRole() == MembershipRole.ADMIN) {
            long adminCount = membershipRepository.countByOrganizationIdAndRoleAndStatus(
                    orgId, MembershipRole.ADMIN, MembershipStatus.ACTIVE);
            if (adminCount <= 1) {
                throw new BusinessRuleException("Vous êtes le dernier admin. Transférez le rôle avant de partir.");
            }
        }

        m.setStatus(MembershipStatus.LEFT);
        membershipRepository.save(m);
        auditService.log(actorUserId, isSelf ? "MEMBERSHIP_LEFT" : "MEMBERSHIP_EXCLUDED", "Membership", membershipId);
    }

    // T-054: Change role
    @Transactional
    public MembershipResponse changeRole(UUID adminUserId, UUID membershipId, MembershipRoleRequest request) {
        Membership m = findMembership(membershipId);
        organizationService.verifyAdmin(adminUserId, m.getOrganization().getId());

        MembershipRole newRole = MembershipRole.valueOf(request.role().toUpperCase());

        if (m.getRole() == MembershipRole.ADMIN && newRole != MembershipRole.ADMIN) {
            long adminCount = membershipRepository.countByOrganizationIdAndRoleAndStatus(
                    m.getOrganization().getId(), MembershipRole.ADMIN, MembershipStatus.ACTIVE);
            if (adminCount <= 1) {
                throw new BusinessRuleException("Impossible : dernier admin de l'organisation.");
            }
        }

        m.setRole(newRole);
        m = membershipRepository.save(m);
        auditService.log(adminUserId, "MEMBERSHIP_ROLE_CHANGED", "Membership", membershipId);
        return toResponse(m);
    }

    // T-055: List members
    @Transactional(readOnly = true)
    public Page<MembershipResponse> listMembers(UUID orgId, MembershipStatus status, Pageable pageable) {
        Page<Membership> page = (status != null)
                ? membershipRepository.findByOrganizationIdAndStatus(orgId, status, pageable)
                : membershipRepository.findByOrganizationId(orgId, pageable);
        return page.map(this::toResponse);
    }

    // T-111: List user memberships
    @Transactional(readOnly = true)
    public Page<MembershipResponse> listByUser(UUID userId, MembershipStatus status, Pageable pageable) {
        return membershipRepository.findByUserIdAndStatus(userId, status, pageable).map(this::toResponse);
    }

    // --- Helpers ---

    private Membership findMembership(UUID id) {
        return membershipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Adhésion non trouvée"));
    }

    @Transactional(readOnly = true)
    public java.util.Optional<MembershipResponse> getMyMembership(UUID userId, UUID orgId) {
        return membershipRepository.findByUserIdAndOrganizationId(userId, orgId)
                .map(this::toResponse);
    }

    private void notifyAdmins(Organization org, UUID applicantUserId) {
        var admins = membershipRepository.findByOrganizationIdAndRoleAndStatus(
                org.getId(), MembershipRole.ADMIN, MembershipStatus.ACTIVE);
        User applicant = userRepository.getReferenceById(applicantUserId);
        for (Membership admin : admins) {
            notificationService.saveNotification(admin.getUser(), NotificationType.MEMBERSHIP_REQUESTED,
                    "Nouvelle demande d'adhésion",
                    applicant.getFirstName() + " " + applicant.getLastName() + " souhaite rejoindre " + org.getName(),
                    "/organizations/" + org.getSlug() + "/manage/members");
        }
    }

    private MembershipResponse toResponse(Membership m) {
        User user = m.getUser();
        return new MembershipResponse(m.getId(), user.getId(), m.getOrganization().getId(),
                m.getOrganization().getName(), m.getOrganization().getSlug(),
                user.getFirstName(), user.getLastName(), user.getPhotoUrl(),
                m.getRole().name(), m.getStatus().name(), m.getMotivation(),
                m.getJoinedAt(), m.getCreatedAt());
    }
}
