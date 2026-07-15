package orga.takwa.ummati.service;

import orga.takwa.ummati.dto.organization.CreateOrgAnnouncementRequest;
import orga.takwa.ummati.dto.organization.OrgAnnouncementResponse;
import orga.takwa.ummati.entity.*;
import orga.takwa.ummati.entity.enums.MembershipStatus;
import orga.takwa.ummati.entity.enums.NotificationType;
import orga.takwa.ummati.exception.ForbiddenException;
import orga.takwa.ummati.exception.ResourceNotFoundException;
import orga.takwa.ummati.repository.MembershipRepository;
import orga.takwa.ummati.repository.OrgAnnouncementRepository;
import orga.takwa.ummati.repository.OrganizationRepository;
import orga.takwa.ummati.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrgAnnouncementServiceTest {

    @Mock private OrgAnnouncementRepository announcementRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private UserRepository userRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private OrganizationService organizationService;
    @Mock private NotificationService notificationService;
    @Mock private AuditService auditService;

    @InjectMocks
    private OrgAnnouncementService announcementService;

    private UUID adminId;
    private UUID orgId;
    private Organization org;
    private User admin;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        orgId = UUID.randomUUID();

        org = new Organization();
        org.setId(orgId);
        org.setName("Asso Solidarité");
        org.setSlug("asso-solidarite");

        admin = new User();
        admin.setId(adminId);
        admin.setFirstName("Admin");
        admin.setLastName("Test");
    }

    @Test
    void create_shouldSucceed_andNotifyActiveMembers() {
        User member = new User();
        member.setId(UUID.randomUUID());
        member.setFirstName("Alice");
        Membership membership = new Membership();
        membership.setUser(member);
        membership.setStatus(MembershipStatus.ACTIVE);

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
        doNothing().when(organizationService).verifyAdmin(adminId, orgId);
        when(userRepository.getReferenceById(adminId)).thenReturn(admin);
        when(announcementRepository.save(any())).thenAnswer(inv -> {
            OrgAnnouncement a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });
        when(membershipRepository.findByOrganizationIdAndStatus(eq(orgId), eq(MembershipStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(membership)));

        OrgAnnouncementResponse result = announcementService.create(adminId, orgId,
                new CreateOrgAnnouncementRequest("Réunion mensuelle", "Rdv vendredi à 18h", false));

        assertThat(result.title()).isEqualTo("Réunion mensuelle");
        assertThat(result.content()).isEqualTo("Rdv vendredi à 18h");
        assertThat(result.pinned()).isFalse();
        verify(notificationService).saveNotification(eq(member), eq(NotificationType.ORG_ANNOUNCEMENT),
                anyString(), anyString(), anyString());
        verify(auditService).log(eq(adminId), eq("ORG_ANNOUNCEMENT_CREATED"), eq("OrgAnnouncement"), any());
    }

    @Test
    void create_shouldTruncateNotifContent_whenContentExceeds100Chars() {
        String longContent = "B".repeat(150);
        User member = new User();
        member.setId(UUID.randomUUID());
        Membership membership = new Membership();
        membership.setUser(member);
        membership.setStatus(MembershipStatus.ACTIVE);

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
        doNothing().when(organizationService).verifyAdmin(adminId, orgId);
        when(userRepository.getReferenceById(adminId)).thenReturn(admin);
        when(announcementRepository.save(any())).thenAnswer(inv -> {
            OrgAnnouncement a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });
        when(membershipRepository.findByOrganizationIdAndStatus(eq(orgId), eq(MembershipStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(membership)));

        announcementService.create(adminId, orgId,
                new CreateOrgAnnouncementRequest("Titre", longContent, false));

        verify(notificationService).saveNotification(any(), any(), anyString(),
                argThat(msg -> msg.length() <= 101 && msg.endsWith("…")), anyString());
    }

    @Test
    void create_shouldFail_whenOrgNotFound() {
        when(organizationRepository.findById(orgId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> announcementService.create(adminId, orgId,
                new CreateOrgAnnouncementRequest("Titre", "Contenu", false)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_shouldFail_whenNotAdmin() {
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
        doThrow(new ForbiddenException("Accès refusé")).when(organizationService).verifyAdmin(adminId, orgId);

        assertThatThrownBy(() -> announcementService.create(adminId, orgId,
                new CreateOrgAnnouncementRequest("Titre", "Contenu", false)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void list_shouldReturnPinnedFirst() {
        OrgAnnouncement pinned = buildAnnouncement(true);
        OrgAnnouncement regular = buildAnnouncement(false);

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
        when(announcementRepository.findByOrganizationIdOrderByPinnedDescCreatedAtDesc(orgId))
                .thenReturn(List.of(pinned, regular));

        List<OrgAnnouncementResponse> results = announcementService.list(orgId);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).pinned()).isTrue();
    }

    @Test
    void list_shouldReturnEmpty_whenNoAnnouncements() {
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
        when(announcementRepository.findByOrganizationIdOrderByPinnedDescCreatedAtDesc(orgId))
                .thenReturn(List.of());

        assertThat(announcementService.list(orgId)).isEmpty();
    }

    @Test
    void delete_shouldSucceed_whenAdmin() {
        OrgAnnouncement a = buildAnnouncement(false);
        UUID announcementId = a.getId();

        when(announcementRepository.findById(announcementId)).thenReturn(Optional.of(a));
        doNothing().when(organizationService).verifyAdmin(adminId, orgId);

        announcementService.delete(adminId, announcementId);

        verify(announcementRepository).delete(a);
        verify(auditService).log(eq(adminId), eq("ORG_ANNOUNCEMENT_DELETED"), eq("OrgAnnouncement"), eq(announcementId));
    }

    @Test
    void delete_shouldFail_whenNotAdmin() {
        OrgAnnouncement a = buildAnnouncement(false);
        UUID announcementId = a.getId();

        when(announcementRepository.findById(announcementId)).thenReturn(Optional.of(a));
        doThrow(new ForbiddenException("Accès refusé")).when(organizationService).verifyAdmin(adminId, orgId);

        assertThatThrownBy(() -> announcementService.delete(adminId, announcementId))
                .isInstanceOf(ForbiddenException.class);
        verify(announcementRepository, never()).delete(any());
    }

    private OrgAnnouncement buildAnnouncement(boolean pinned) {
        OrgAnnouncement a = new OrgAnnouncement();
        a.setId(UUID.randomUUID());
        a.setOrganization(org);
        a.setAuthor(admin);
        a.setTitle("Annonce test");
        a.setContent("Contenu de l'annonce");
        a.setPinned(pinned);
        return a;
    }
}
