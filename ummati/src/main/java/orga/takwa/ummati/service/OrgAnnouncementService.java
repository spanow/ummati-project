package orga.takwa.ummati.service;

import orga.takwa.ummati.dto.organization.CreateOrgAnnouncementRequest;
import orga.takwa.ummati.dto.organization.OrgAnnouncementResponse;
import orga.takwa.ummati.entity.OrgAnnouncement;
import orga.takwa.ummati.entity.Organization;
import orga.takwa.ummati.entity.User;
import orga.takwa.ummati.entity.enums.MembershipStatus;
import orga.takwa.ummati.entity.enums.NotificationType;
import orga.takwa.ummati.exception.ResourceNotFoundException;
import orga.takwa.ummati.repository.MembershipRepository;
import orga.takwa.ummati.repository.OrgAnnouncementRepository;
import orga.takwa.ummati.repository.OrganizationRepository;
import orga.takwa.ummati.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class OrgAnnouncementService {

    private final OrgAnnouncementRepository announcementRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final OrganizationService organizationService;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public OrgAnnouncementService(OrgAnnouncementRepository announcementRepository,
                                  OrganizationRepository organizationRepository,
                                  UserRepository userRepository,
                                  MembershipRepository membershipRepository,
                                  OrganizationService organizationService,
                                  NotificationService notificationService,
                                  AuditService auditService) {
        this.announcementRepository = announcementRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.organizationService = organizationService;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    @Transactional
    public OrgAnnouncementResponse create(UUID adminId, UUID orgId, CreateOrgAnnouncementRequest request) {
        Organization org = findOrg(orgId);
        organizationService.verifyAdmin(adminId, orgId);

        User author = userRepository.getReferenceById(adminId);
        OrgAnnouncement announcement = new OrgAnnouncement();
        announcement.setOrganization(org);
        announcement.setAuthor(author);
        announcement.setTitle(request.title().trim());
        announcement.setContent(request.content().trim());
        announcement.setPinned(request.pinned());
        announcement = announcementRepository.save(announcement);

        notifyActiveMembers(org, announcement);
        auditService.log(adminId, "ORG_ANNOUNCEMENT_CREATED", "OrgAnnouncement", announcement.getId());

        return toResponse(announcement);
    }

    @Transactional(readOnly = true)
    public List<OrgAnnouncementResponse> list(UUID orgId) {
        findOrg(orgId);
        return announcementRepository.findByOrganizationIdOrderByPinnedDescCreatedAtDesc(orgId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public void delete(UUID adminId, UUID announcementId) {
        OrgAnnouncement announcement = findAnnouncement(announcementId);
        organizationService.verifyAdmin(adminId, announcement.getOrganization().getId());

        announcementRepository.delete(announcement);
        auditService.log(adminId, "ORG_ANNOUNCEMENT_DELETED", "OrgAnnouncement", announcementId);
    }

    private void notifyActiveMembers(Organization org, OrgAnnouncement announcement) {
        membershipRepository.findByOrganizationIdAndStatus(org.getId(), MembershipStatus.ACTIVE, PageRequest.of(0, 500))
                .forEach(m -> notificationService.saveNotification(
                        m.getUser(), NotificationType.ORG_ANNOUNCEMENT,
                        org.getName() + " : " + announcement.getTitle(),
                        announcement.getContent().length() > 100
                                ? announcement.getContent().substring(0, 100) + "…"
                                : announcement.getContent(),
                        "/organizations/" + org.getSlug()));
    }

    private Organization findOrg(UUID orgId) {
        return organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation non trouvée"));
    }

    private OrgAnnouncement findAnnouncement(UUID id) {
        return announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Annonce non trouvée"));
    }

    private OrgAnnouncementResponse toResponse(OrgAnnouncement a) {
        User author = a.getAuthor();
        return new OrgAnnouncementResponse(a.getId(), a.getOrganization().getId(),
                author.getId(), author.getFirstName(), author.getLastName(),
                a.getTitle(), a.getContent(), a.isPinned(), a.getCreatedAt(), a.getUpdatedAt());
    }
}
