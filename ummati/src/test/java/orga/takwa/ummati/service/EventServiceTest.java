package orga.takwa.ummati.service;

import orga.takwa.ummati.dto.event.*;
import orga.takwa.ummati.entity.*;
import orga.takwa.ummati.entity.enums.*;
import orga.takwa.ummati.exception.BusinessRuleException;
import orga.takwa.ummati.exception.ConflictException;
import orga.takwa.ummati.exception.ForbiddenException;
import orga.takwa.ummati.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock private EventRepository eventRepository;
    @Mock private EventSignupRepository eventSignupRepository;
    @Mock private EventFeedbackRepository eventFeedbackRepository;
    @Mock private OrganizationService organizationService;
    @Mock private UserRepository userRepository;
    @Mock private SkillRepository skillRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private NotificationService notificationService;
    @Mock private AuditService auditService;

    @InjectMocks
    private EventService eventService;

    private UUID userId;
    private UUID orgId;
    private UUID eventId;
    private Organization activeOrg;
    private User user;
    private Event publishedEvent;
    private Event draftEvent;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orgId = UUID.randomUUID();
        eventId = UUID.randomUUID();

        activeOrg = new Organization();
        activeOrg.setId(orgId);
        activeOrg.setName("Test Org");
        activeOrg.setSlug("test-org");
        activeOrg.setStatus(OrganizationStatus.ACTIVE);

        user = new User();
        user.setId(userId);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("test@test.com");
        user.setEmailVerified(true);
        user.setDateOfBirth(LocalDate.of(1990, 1, 1));

        draftEvent = new Event();
        draftEvent.setId(eventId);
        draftEvent.setOrganization(activeOrg);
        draftEvent.setTitle("Test Event");
        draftEvent.setDescription("Description");
        draftEvent.setType(EventType.FORMATION);
        draftEvent.setLocationCity("Paris");
        draftEvent.setStartDate(LocalDateTime.now().plusDays(7));
        draftEvent.setEndDate(LocalDateTime.now().plusDays(7).plusHours(3));
        draftEvent.setStatus(EventStatus.DRAFT);
        draftEvent.setCreatedBy(user);
        draftEvent.setRequiredSkills(new HashSet<>());

        publishedEvent = new Event();
        publishedEvent.setId(eventId);
        publishedEvent.setOrganization(activeOrg);
        publishedEvent.setTitle("Published Event");
        publishedEvent.setDescription("Description");
        publishedEvent.setType(EventType.MARAUDE);
        publishedEvent.setLocationCity("Lyon");
        publishedEvent.setStartDate(LocalDateTime.now().plusDays(7));
        publishedEvent.setEndDate(LocalDateTime.now().plusDays(7).plusHours(3));
        publishedEvent.setMaxParticipants(20);
        publishedEvent.setStatus(EventStatus.PUBLISHED);
        publishedEvent.setCreatedBy(user);
        publishedEvent.setRequiredSkills(new HashSet<>());
    }

    // --- T-070: Create Event ---

    @Test
    void createEvent_shouldSucceed_whenAdminAndValidDates() {
        doNothing().when(organizationService).verifyAdmin(userId, orgId);
        when(organizationService.findOrg(orgId)).thenReturn(activeOrg);
        when(userRepository.getReferenceById(userId)).thenReturn(user);
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> {
            Event e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });
        when(eventSignupRepository.countByEventIdAndStatus(any(), any())).thenReturn(0L);

        CreateEventRequest request = new CreateEventRequest(
                "Maraude", "Description", null, "MARAUDE",
                null, null, "Paris", null, null, null, false, null,
                LocalDateTime.now().plusDays(7), LocalDateTime.now().plusDays(7).plusHours(3),
                LocalDateTime.now().plusDays(6), 20, null, null);

        EventDetail result = eventService.createEvent(userId, orgId, request);

        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("Maraude");
        assertThat(result.status()).isEqualTo("DRAFT");
        verify(eventRepository).save(any(Event.class));
        verify(auditService).log(eq(userId), eq("EVENT_CREATED"), eq("Event"), any());
    }

    @Test
    void createEvent_shouldFail_whenEndDateBeforeStartDate() {
        doNothing().when(organizationService).verifyAdmin(userId, orgId);
        when(organizationService.findOrg(orgId)).thenReturn(activeOrg);

        CreateEventRequest request = new CreateEventRequest(
                "Event", "Desc", null, "FORMATION",
                null, null, "Paris", null, null, null, false, null,
                LocalDateTime.now().plusDays(7), LocalDateTime.now().plusDays(6),
                null, null, null, null);

        assertThatThrownBy(() -> eventService.createEvent(userId, orgId, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("date de fin");
    }

    // --- T-075: Signup ---

    @Test
    void signup_shouldRegister_whenSpotsAvailable() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(publishedEvent));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventSignupRepository.findByEventIdAndUserId(eventId, userId)).thenReturn(Optional.empty());
        when(eventSignupRepository.countByEventIdAndStatus(eventId, SignupStatus.REGISTERED)).thenReturn(5L);
        when(eventSignupRepository.save(any(EventSignup.class))).thenAnswer(inv -> {
            EventSignup s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            s.setRegisteredAt(LocalDateTime.now());
            return s;
        });

        SignupResponse result = eventService.signup(userId, eventId);

        assertThat(result.status()).isEqualTo("REGISTERED");
        verify(notificationService).saveNotification(eq(user), eq(NotificationType.SIGNUP_CONFIRMED),
                anyString(), anyString(), anyString());
    }

    @Test
    void signup_shouldWaitlist_whenFull() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(publishedEvent));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventSignupRepository.findByEventIdAndUserId(eventId, userId)).thenReturn(Optional.empty());
        when(eventSignupRepository.countByEventIdAndStatus(eventId, SignupStatus.REGISTERED)).thenReturn(20L);
        when(eventSignupRepository.save(any(EventSignup.class))).thenAnswer(inv -> {
            EventSignup s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            s.setRegisteredAt(LocalDateTime.now());
            return s;
        });

        SignupResponse result = eventService.signup(userId, eventId);

        assertThat(result.status()).isEqualTo("WAITLISTED");
    }

    @Test
    void signup_shouldReuseRow_whenPreviouslyCancelled() {
        EventSignup cancelledSignup = new EventSignup();
        cancelledSignup.setId(UUID.randomUUID());
        cancelledSignup.setUser(user);
        cancelledSignup.setEvent(publishedEvent);
        cancelledSignup.setStatus(SignupStatus.CANCELLED);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(publishedEvent));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventSignupRepository.findByEventIdAndUserId(eventId, userId)).thenReturn(Optional.of(cancelledSignup));
        when(eventSignupRepository.countByEventIdAndStatus(eventId, SignupStatus.REGISTERED)).thenReturn(5L);
        when(eventSignupRepository.save(any(EventSignup.class))).thenAnswer(inv -> inv.getArgument(0));

        SignupResponse result = eventService.signup(userId, eventId);

        assertThat(result.status()).isEqualTo("REGISTERED");
        // Should have reused the same row, not created a new one
        verify(eventSignupRepository, never()).existsByEventIdAndUserId(any(), any());
    }

    @Test
    void signup_shouldFail_whenAlreadyRegistered() {
        EventSignup existing = new EventSignup();
        existing.setStatus(SignupStatus.REGISTERED);
        existing.setUser(user);
        existing.setEvent(publishedEvent);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(publishedEvent));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventSignupRepository.findByEventIdAndUserId(eventId, userId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> eventService.signup(userId, eventId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("déjà inscrit");
    }

    @Test
    void signup_shouldFail_whenDeadlinePassed() {
        publishedEvent.setRegistrationDeadline(LocalDateTime.now().minusDays(1));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(publishedEvent));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> eventService.signup(userId, eventId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("date limite");
    }

    // --- T-076: Cancel signup with FIFO promotion ---

    @Test
    void cancelSignup_shouldPromoteWaitlisted_FIFO() {
        EventSignup registeredSignup = new EventSignup();
        registeredSignup.setId(UUID.randomUUID());
        registeredSignup.setEvent(publishedEvent);
        registeredSignup.setUser(user);
        registeredSignup.setStatus(SignupStatus.REGISTERED);

        User waitlistedUser = new User();
        waitlistedUser.setId(UUID.randomUUID());
        waitlistedUser.setFirstName("Wait");
        waitlistedUser.setLastName("Listed");

        EventSignup waitlistedSignup = new EventSignup();
        waitlistedSignup.setId(UUID.randomUUID());
        waitlistedSignup.setEvent(publishedEvent);
        waitlistedSignup.setUser(waitlistedUser);
        waitlistedSignup.setStatus(SignupStatus.WAITLISTED);

        when(eventSignupRepository.findByEventIdAndUserId(eventId, userId)).thenReturn(Optional.of(registeredSignup));
        when(eventSignupRepository.findFirstByEventIdAndStatusOrderByRegisteredAtAsc(eventId, SignupStatus.WAITLISTED))
                .thenReturn(Optional.of(waitlistedSignup));

        eventService.cancelSignup(userId, eventId);

        assertThat(registeredSignup.getStatus()).isEqualTo(SignupStatus.CANCELLED);
        assertThat(waitlistedSignup.getStatus()).isEqualTo(SignupStatus.REGISTERED);
        verify(notificationService).saveNotification(
                eq(waitlistedUser), eq(NotificationType.SIGNUP_PROMOTED),
                anyString(), anyString(), anyString());
    }

    // --- T-072: Change status ---

    @Test
    void changeStatus_shouldPublish_andNotifyMembers() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(draftEvent));
        doNothing().when(organizationService).verifyAdmin(userId, orgId);
        when(membershipRepository.findByOrganizationIdAndRoleInAndStatus(eq(orgId), any(), eq(MembershipStatus.ACTIVE)))
                .thenReturn(List.of());
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
        when(eventSignupRepository.countByEventIdAndStatus(any(), any())).thenReturn(0L);

        EventDetail result = eventService.changeStatus(userId, eventId, new EventStatusRequest("PUBLISH", null));

        assertThat(result.status()).isEqualTo("PUBLISHED");
        verify(auditService).log(eq(userId), eq("EVENT_PUBLISHED"), eq("Event"), eq(eventId));
    }

    @Test
    void changeStatus_shouldCancel_withReasonMin10Chars() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(publishedEvent));
        doNothing().when(organizationService).verifyAdmin(userId, orgId);
        when(eventSignupRepository.findByEventIdAndStatusIn(eq(eventId), any())).thenReturn(List.of());
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
        when(eventSignupRepository.countByEventIdAndStatus(any(), any())).thenReturn(0L);

        EventDetail result = eventService.changeStatus(userId, eventId,
                new EventStatusRequest("CANCEL", "Météo très défavorable"));

        assertThat(result.status()).isEqualTo("CANCELLED");
    }

    @Test
    void changeStatus_shouldFail_cancelWithReasonTooShort() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(publishedEvent));
        doNothing().when(organizationService).verifyAdmin(userId, orgId);

        assertThatThrownBy(() -> eventService.changeStatus(userId, eventId,
                new EventStatusRequest("CANCEL", "court")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("10 caractères");
    }

    // --- T-080: Feedback ---

    @Test
    void createFeedback_shouldFail_whenNotAttended() {
        EventSignup signup = new EventSignup();
        signup.setStatus(SignupStatus.REGISTERED);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(publishedEvent));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventSignupRepository.findByEventIdAndUserId(eventId, userId)).thenReturn(Optional.of(signup));

        assertThatThrownBy(() -> eventService.createFeedback(userId, eventId,
                new CreateFeedbackRequest(4, "Good", false)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("assisté");
    }

    @Test
    void createFeedback_shouldFail_whenDuplicate() {
        EventSignup signup = new EventSignup();
        signup.setStatus(SignupStatus.ATTENDED);
        signup.setUser(user);
        signup.setEvent(publishedEvent);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(publishedEvent));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventSignupRepository.findByEventIdAndUserId(eventId, userId)).thenReturn(Optional.of(signup));
        when(eventFeedbackRepository.existsByEventIdAndUserId(eventId, userId)).thenReturn(true);

        assertThatThrownBy(() -> eventService.createFeedback(userId, eventId,
                new CreateFeedbackRequest(5, "Great", false)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("déjà laissé");
    }

    // --- listOrgEvents ---

    @Test
    void listOrgEvents_shouldReturnAllStatuses_forOrgAdmin() {
        doNothing().when(organizationService).verifyAdmin(userId, orgId);

        var page = new PageImpl<>(List.of(draftEvent, publishedEvent));
        when(eventRepository.findByOrganizationId(eq(orgId), any(Pageable.class))).thenReturn(page);
        when(eventSignupRepository.countByEventIdAndStatus(any(), any())).thenReturn(0L);

        var result = eventService.listOrgEvents(userId, orgId, PageRequest.of(0, 50));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).extracting(EventSummary::status)
                .containsExactlyInAnyOrder("DRAFT", "PUBLISHED");
    }

    @Test
    void listOrgEvents_shouldRejectNonAdmin() {
        doThrow(new ForbiddenException("Accès refusé"))
                .when(organizationService).verifyAdmin(userId, orgId);

        assertThatThrownBy(() -> eventService.listOrgEvents(userId, orgId, PageRequest.of(0, 50)))
                .isInstanceOf(ForbiddenException.class);
    }

    // --- T-071: Update restrictions ---

    @Test
    void updateEvent_shouldFail_whenPublishedAndChangingDates() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(publishedEvent));
        doNothing().when(organizationService).verifyAdmin(userId, orgId);

        UpdateEventRequest request = new UpdateEventRequest(
                null, null, null, null, null, null, null, null, null, null, null, null,
                LocalDateTime.now().plusDays(10), null,
                null, null, null, null);

        assertThatThrownBy(() -> eventService.updateEvent(userId, eventId, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("publié");
    }
}
