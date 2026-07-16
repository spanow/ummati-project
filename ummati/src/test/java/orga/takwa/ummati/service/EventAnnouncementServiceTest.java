package orga.takwa.ummati.service;

import orga.takwa.ummati.dto.event.AnnouncementResponse;
import orga.takwa.ummati.dto.event.CreateAnnouncementRequest;
import orga.takwa.ummati.entity.*;
import orga.takwa.ummati.entity.enums.*;
import orga.takwa.ummati.exception.BusinessRuleException;
import orga.takwa.ummati.exception.ForbiddenException;
import orga.takwa.ummati.repository.EventAnnouncementRepository;
import orga.takwa.ummati.repository.EventSignupRepository;
import orga.takwa.ummati.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventAnnouncementServiceTest {

    @Mock private EventAnnouncementRepository announcementRepository;
    @Mock private EventSignupRepository eventSignupRepository;
    @Mock private UserRepository userRepository;
    @Mock private EventService eventService;
    @Mock private OrganizationService organizationService;
    @Mock private NotificationService notificationService;
    @Mock private AuditService auditService;

    @InjectMocks
    private EventAnnouncementService announcementService;

    private UUID adminId;
    private UUID eventId;
    private Event publishedEvent;
    private Organization org;
    private User admin;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        eventId = UUID.randomUUID();

        org = new Organization();
        org.setId(UUID.randomUUID());
        org.setName("Test ONG");
        org.setSlug("test-ong");

        admin = new User();
        admin.setId(adminId);
        admin.setFirstName("Admin");
        admin.setLastName("User");

        publishedEvent = new Event();
        publishedEvent.setId(eventId);
        publishedEvent.setOrganization(org);
        publishedEvent.setTitle("Maraude 75");
        publishedEvent.setStatus(EventStatus.PUBLISHED);
        publishedEvent.setRequiredSkills(new java.util.HashSet<>());
    }

    @Test
    void create_shouldSucceed_andNotifyRegistered() {
        EventSignup signup = new EventSignup();
        User participant = new User();
        participant.setId(UUID.randomUUID());
        participant.setFirstName("Alice");
        signup.setUser(participant);
        signup.setStatus(SignupStatus.REGISTERED);

        when(eventService.findEvent(eventId)).thenReturn(publishedEvent);
        doNothing().when(organizationService).verifyAdmin(adminId, org.getId());
        when(userRepository.getReferenceById(adminId)).thenReturn(admin);
        when(announcementRepository.save(any())).thenAnswer(inv -> {
            EventAnnouncement a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });
        when(eventSignupRepository.findByEventIdAndStatusIn(eq(eventId), any())).thenReturn(List.of(signup));

        AnnouncementResponse result = announcementService.create(adminId, eventId,
                new CreateAnnouncementRequest("Rendez-vous à 18h30 devant la gare", false));

        assertThat(result.content()).isEqualTo("Rendez-vous à 18h30 devant la gare");
        assertThat(result.pinned()).isFalse();
        verify(notificationService).saveNotification(eq(participant), eq(NotificationType.EVENT_ANNOUNCEMENT),
                anyString(), anyString(), anyString());
        verify(auditService).log(eq(adminId), eq("EVENT_ANNOUNCEMENT_CREATED"), eq("EventAnnouncement"), any());
    }

    @Test
    void create_shouldTruncateNotifContent_whenContentExceeds100Chars() {
        String longContent = "A".repeat(150);
        EventSignup signup = new EventSignup();
        User participant = new User();
        participant.setId(UUID.randomUUID());
        signup.setUser(participant);
        signup.setStatus(SignupStatus.REGISTERED);

        when(eventService.findEvent(eventId)).thenReturn(publishedEvent);
        doNothing().when(organizationService).verifyAdmin(adminId, org.getId());
        when(userRepository.getReferenceById(adminId)).thenReturn(admin);
        when(announcementRepository.save(any())).thenAnswer(inv -> {
            EventAnnouncement a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });
        when(eventSignupRepository.findByEventIdAndStatusIn(eq(eventId), any())).thenReturn(List.of(signup));

        announcementService.create(adminId, eventId, new CreateAnnouncementRequest(longContent, false));

        verify(notificationService).saveNotification(any(), any(), anyString(),
                argThat(msg -> msg.length() <= 101 && msg.endsWith("…")), anyString());
    }

    @Test
    void create_shouldFail_whenEventNotPublished() {
        publishedEvent.setStatus(EventStatus.DRAFT);
        when(eventService.findEvent(eventId)).thenReturn(publishedEvent);
        doNothing().when(organizationService).verifyAdmin(adminId, org.getId());

        assertThatThrownBy(() -> announcementService.create(adminId, eventId,
                new CreateAnnouncementRequest("Annonce test", false)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("publié");
    }

    @Test
    void create_shouldFail_whenNotAdmin() {
        when(eventService.findEvent(eventId)).thenReturn(publishedEvent);
        doThrow(new ForbiddenException("Accès refusé"))
                .when(organizationService).verifyAdmin(adminId, org.getId());

        assertThatThrownBy(() -> announcementService.create(adminId, eventId,
                new CreateAnnouncementRequest("Annonce test", false)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void list_shouldReturnPinnedFirst() {
        EventAnnouncement pinned = buildAnnouncement(true, LocalDateTime.now().minusHours(2));
        EventAnnouncement regular = buildAnnouncement(false, LocalDateTime.now().minusHours(1));

        when(eventService.findEvent(eventId)).thenReturn(publishedEvent);
        when(announcementRepository.findByEventIdOrderByPinnedDescCreatedAtAsc(eventId))
                .thenReturn(List.of(pinned, regular));

        List<AnnouncementResponse> results = announcementService.list(eventId);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).pinned()).isTrue();
    }

    @Test
    void delete_shouldSucceed_whenAdmin() {
        EventAnnouncement a = buildAnnouncement(false, LocalDateTime.now());
        UUID announcementId = a.getId();

        when(announcementRepository.findById(announcementId)).thenReturn(Optional.of(a));
        doNothing().when(organizationService).verifyAdmin(adminId, org.getId());

        announcementService.delete(adminId, announcementId);

        verify(announcementRepository).delete(a);
        verify(auditService).log(eq(adminId), eq("EVENT_ANNOUNCEMENT_DELETED"), eq("EventAnnouncement"), eq(announcementId));
    }

    private EventAnnouncement buildAnnouncement(boolean pinned, LocalDateTime createdAt) {
        EventAnnouncement a = new EventAnnouncement();
        a.setId(UUID.randomUUID());
        a.setEvent(publishedEvent);
        a.setAuthor(admin);
        a.setContent("Contenu test");
        a.setPinned(pinned);
        return a;
    }
}
