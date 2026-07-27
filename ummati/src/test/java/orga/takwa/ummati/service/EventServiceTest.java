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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock private EventRepository eventRepository;
    @Mock private EventOccurrenceRepository eventOccurrenceRepository;
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
    private UUID occurrenceId;
    private Organization activeOrg;
    private User user;
    private Event publishedEvent;
    private Event draftEvent;
    private EventOccurrence publishedOccurrence;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orgId = UUID.randomUUID();
        eventId = UUID.randomUUID();
        occurrenceId = UUID.randomUUID();

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

        // Créneau unique miroir de l'événement publié (cas mono-créneau).
        publishedOccurrence = new EventOccurrence();
        publishedOccurrence.setId(occurrenceId);
        publishedOccurrence.setEvent(publishedEvent);
        publishedOccurrence.setStartDate(publishedEvent.getStartDate());
        publishedOccurrence.setEndDate(publishedEvent.getEndDate());
        publishedOccurrence.setMaxParticipants(20);
        publishedOccurrence.setStatus(EventOccurrenceStatus.PUBLISHED);
    }

    /**
     * L'inscription recharge le créneau avec un verrou de ligne (anti-surréservation) :
     * les tests d'inscription doivent donc câbler ce chargement en plus de la résolution
     * du créneau depuis l'événement.
     */
    private void givenOccurrenceLockable() {
        lenient().when(eventOccurrenceRepository.findByIdForUpdate(occurrenceId))
                .thenReturn(Optional.of(publishedOccurrence));
    }

    // --- T-070: Create Event ---

    @Test
    void createEvent_shouldSucceed_andGenerateOnePrimaryOccurrence() {
        doNothing().when(organizationService).verifyAdmin(userId, orgId);
        when(organizationService.findOrg(orgId)).thenReturn(activeOrg);
        when(userRepository.getReferenceById(userId)).thenReturn(user);
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> {
            Event e = inv.getArgument(0);
            if (e.getId() == null) e.setId(UUID.randomUUID());
            return e;
        });

        CreateEventRequest request = new CreateEventRequest(
                "Maraude", "Description", null, "MARAUDE",
                null, null, "Paris", null, null, null, false, null,
                LocalDateTime.now().plusDays(7), LocalDateTime.now().plusDays(7).plusHours(3),
                LocalDateTime.now().plusDays(6), 20, null, null, null, null);

        EventDetail result = eventService.createEvent(userId, orgId, request);

        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("Maraude");
        assertThat(result.status()).isEqualTo("DRAFT");
        // Un créneau principal est généré et persisté.
        verify(eventOccurrenceRepository).saveAll(argThat((Iterable<EventOccurrence> occ) ->
                occ.iterator().hasNext()));
        verify(auditService).log(eq(userId), eq("EVENT_CREATED"), eq("Event"), any());
    }

    @Test
    void createEvent_shouldGenerateWeeklyRecurrence() {
        doNothing().when(organizationService).verifyAdmin(userId, orgId);
        when(organizationService.findOrg(orgId)).thenReturn(activeOrg);
        when(userRepository.getReferenceById(userId)).thenReturn(user);
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> {
            Event e = inv.getArgument(0);
            if (e.getId() == null) e.setId(UUID.randomUUID());
            return e;
        });

        LocalDateTime start = LocalDateTime.now().plusDays(3);
        RecurrenceInput recurrence = new RecurrenceInput("WEEKLY", 1, start.toLocalDate().plusWeeks(3));
        CreateEventRequest request = new CreateEventRequest(
                "Maraude hebdo", "Description", null, "MARAUDE",
                null, null, "Paris", null, null, null, false, null,
                start, start.plusHours(3), null, 10, null, null, null, recurrence);

        eventService.createEvent(userId, orgId, request);

        // 1 créneau principal + 3 générés (semaines +1, +2, +3) = 4.
        verify(eventOccurrenceRepository).saveAll(argThat((Iterable<EventOccurrence> occ) -> {
            int count = 0;
            for (EventOccurrence ignored : occ) count++;
            return count == 4;
        }));
    }

    @Test
    void createEvent_shouldFail_whenEndDateBeforeStartDate() {
        doNothing().when(organizationService).verifyAdmin(userId, orgId);
        when(organizationService.findOrg(orgId)).thenReturn(activeOrg);

        CreateEventRequest request = new CreateEventRequest(
                "Event", "Desc", null, "FORMATION",
                null, null, "Paris", null, null, null, false, null,
                LocalDateTime.now().plusDays(7), LocalDateTime.now().plusDays(6),
                null, null, null, null, null, null);

        assertThatThrownBy(() -> eventService.createEvent(userId, orgId, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("date de fin");
    }

    // --- T-075: Signup (occurrence-based) ---

    @Test
    void signup_shouldRegister_whenSpotsAvailable() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(publishedEvent));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventOccurrenceRepository.findByEventIdOrderByStartDateAsc(eventId))
                .thenReturn(List.of(publishedOccurrence));
        givenOccurrenceLockable();
        when(eventSignupRepository.findByOccurrenceIdAndUserId(occurrenceId, userId)).thenReturn(Optional.empty());
        when(eventSignupRepository.countByOccurrenceIdAndStatus(occurrenceId, SignupStatus.REGISTERED)).thenReturn(5L);
        when(eventSignupRepository.save(any(EventSignup.class))).thenAnswer(inv -> {
            EventSignup s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            s.setRegisteredAt(LocalDateTime.now());
            return s;
        });

        SignupResponse result = eventService.signup(userId, eventId);

        assertThat(result.status()).isEqualTo("REGISTERED");
        assertThat(result.occurrenceId()).isEqualTo(occurrenceId);
        verify(notificationService).saveNotification(eq(user), eq(NotificationType.SIGNUP_CONFIRMED),
                anyString(), anyString(), anyString());
    }

    @Test
    void signup_shouldWaitlist_whenFull() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(publishedEvent));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventOccurrenceRepository.findByEventIdOrderByStartDateAsc(eventId))
                .thenReturn(List.of(publishedOccurrence));
        givenOccurrenceLockable();
        when(eventSignupRepository.findByOccurrenceIdAndUserId(occurrenceId, userId)).thenReturn(Optional.empty());
        when(eventSignupRepository.countByOccurrenceIdAndStatus(occurrenceId, SignupStatus.REGISTERED)).thenReturn(20L);
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
        cancelledSignup.setOccurrence(publishedOccurrence);
        cancelledSignup.setStatus(SignupStatus.CANCELLED);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(publishedEvent));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventOccurrenceRepository.findByEventIdOrderByStartDateAsc(eventId))
                .thenReturn(List.of(publishedOccurrence));
        givenOccurrenceLockable();
        when(eventSignupRepository.findByOccurrenceIdAndUserId(occurrenceId, userId))
                .thenReturn(Optional.of(cancelledSignup));
        when(eventSignupRepository.countByOccurrenceIdAndStatus(occurrenceId, SignupStatus.REGISTERED)).thenReturn(5L);
        when(eventSignupRepository.save(any(EventSignup.class))).thenAnswer(inv -> inv.getArgument(0));

        SignupResponse result = eventService.signup(userId, eventId);

        assertThat(result.status()).isEqualTo("REGISTERED");
        // La ligne annulée est réutilisée (pas de vérif d'existence event-level).
        verify(eventSignupRepository, never()).existsByEventIdAndUserId(any(), any());
    }

    @Test
    void signup_shouldFail_whenAlreadyRegistered() {
        EventSignup existing = new EventSignup();
        existing.setStatus(SignupStatus.REGISTERED);
        existing.setUser(user);
        existing.setEvent(publishedEvent);
        existing.setOccurrence(publishedOccurrence);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(publishedEvent));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventOccurrenceRepository.findByEventIdOrderByStartDateAsc(eventId))
                .thenReturn(List.of(publishedOccurrence));
        givenOccurrenceLockable();
        when(eventSignupRepository.findByOccurrenceIdAndUserId(occurrenceId, userId))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> eventService.signup(userId, eventId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("déjà inscrit");
    }

    @Test
    void signup_shouldFail_whenDeadlinePassed() {
        publishedOccurrence.setRegistrationDeadline(LocalDateTime.now().minusDays(1));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(publishedEvent));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventOccurrenceRepository.findByEventIdOrderByStartDateAsc(eventId))
                .thenReturn(List.of(publishedOccurrence));
        givenOccurrenceLockable();

        assertThatThrownBy(() -> eventService.signup(userId, eventId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("date limite");
    }

    @Test
    void signup_shouldFail_whenEventHasMultipleOccurrences_withoutOccurrenceId() {
        EventOccurrence second = new EventOccurrence();
        second.setId(UUID.randomUUID());
        second.setEvent(publishedEvent);
        second.setStatus(EventOccurrenceStatus.PUBLISHED);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(publishedEvent));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventOccurrenceRepository.findByEventIdOrderByStartDateAsc(eventId))
                .thenReturn(List.of(publishedOccurrence, second));

        assertThatThrownBy(() -> eventService.signup(userId, eventId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("plusieurs créneaux");
    }

    // --- T-076: Cancel signup with FIFO promotion (per occurrence) ---

    @Test
    void cancelSignup_shouldPromoteWaitlisted_FIFO() {
        EventSignup registeredSignup = new EventSignup();
        registeredSignup.setId(UUID.randomUUID());
        registeredSignup.setEvent(publishedEvent);
        registeredSignup.setOccurrence(publishedOccurrence);
        registeredSignup.setUser(user);
        registeredSignup.setStatus(SignupStatus.REGISTERED);

        User waitlistedUser = new User();
        waitlistedUser.setId(UUID.randomUUID());
        waitlistedUser.setFirstName("Wait");
        waitlistedUser.setLastName("Listed");

        EventSignup waitlistedSignup = new EventSignup();
        waitlistedSignup.setId(UUID.randomUUID());
        waitlistedSignup.setEvent(publishedEvent);
        waitlistedSignup.setOccurrence(publishedOccurrence);
        waitlistedSignup.setUser(waitlistedUser);
        waitlistedSignup.setStatus(SignupStatus.WAITLISTED);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(publishedEvent));
        when(eventOccurrenceRepository.findByEventIdOrderByStartDateAsc(eventId))
                .thenReturn(List.of(publishedOccurrence));
        givenOccurrenceLockable();
        when(eventSignupRepository.findByOccurrenceIdAndUserId(occurrenceId, userId))
                .thenReturn(Optional.of(registeredSignup));
        when(eventSignupRepository.findFirstByOccurrenceIdAndStatusOrderByRegisteredAtAsc(occurrenceId, SignupStatus.WAITLISTED))
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

    // --- Occurrence status: cancel a single créneau ---

    @Test
    void changeOccurrenceStatus_shouldCancelOneOccurrence_andNotifyOnlyItsSignups() {
        UUID occId = publishedOccurrence.getId();
        EventSignup s = new EventSignup();
        s.setId(UUID.randomUUID());
        s.setEvent(publishedEvent);
        s.setOccurrence(publishedOccurrence);
        s.setUser(user);
        s.setStatus(SignupStatus.REGISTERED);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(publishedEvent));
        doNothing().when(organizationService).verifyAdmin(userId, orgId);
        when(eventOccurrenceRepository.findById(occId)).thenReturn(Optional.of(publishedOccurrence));
        when(eventSignupRepository.findByOccurrenceIdAndStatusIn(eq(occId), any())).thenReturn(List.of(s));
        when(eventOccurrenceRepository.findByEventIdOrderByStartDateAsc(eventId))
                .thenReturn(List.of(publishedOccurrence));
        givenOccurrenceLockable();
        when(eventSignupRepository.countByEventIdAndStatus(any(), any())).thenReturn(0L);

        eventService.changeOccurrenceStatus(userId, eventId, occId,
                new EventStatusRequest("CANCEL", "Créneau annulé pour cause de météo"));

        assertThat(publishedOccurrence.getStatus()).isEqualTo(EventOccurrenceStatus.CANCELLED);
        assertThat(s.getStatus()).isEqualTo(SignupStatus.CANCELLED);
        verify(notificationService).saveNotification(eq(user), eq(NotificationType.EVENT_CANCELLED),
                anyString(), anyString(), anyString());
        verify(auditService).log(eq(userId), eq("EVENT_OCCURRENCE_CANCELLED"), eq("EventOccurrence"), eq(occId));
    }

    // --- T-080: Feedback (série : avoir assisté à un créneau) ---

    @Test
    void createFeedback_shouldFail_whenNotAttended() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(publishedEvent));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventSignupRepository.existsByEventIdAndUserIdAndStatus(eventId, userId, SignupStatus.ATTENDED))
                .thenReturn(false);

        assertThatThrownBy(() -> eventService.createFeedback(userId, eventId,
                new CreateFeedbackRequest(4, "Good", false)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("assisté");
    }

    @Test
    void createFeedback_shouldFail_whenDuplicate() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(publishedEvent));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventSignupRepository.existsByEventIdAndUserIdAndStatus(eventId, userId, SignupStatus.ATTENDED))
                .thenReturn(true);
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

    // --- PR2 : heures de bénévolat ---

    @Test
    void markAttendance_shouldPrefillHoursFromOccurrenceDuration() {
        // publishedOccurrence dure 3h (start +7j, end +7j+3h)
        EventSignup s = new EventSignup();
        s.setId(UUID.randomUUID());
        s.setEvent(publishedEvent);
        s.setOccurrence(publishedOccurrence);
        s.setUser(user);
        s.setStatus(SignupStatus.REGISTERED);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(publishedEvent));
        doNothing().when(organizationService).verifyAdmin(userId, orgId);
        when(eventOccurrenceRepository.findByEventIdOrderByStartDateAsc(eventId))
                .thenReturn(List.of(publishedOccurrence));
        when(eventSignupRepository.findByOccurrenceIdAndUserId(occurrenceId, userId))
                .thenReturn(Optional.of(s));

        eventService.markAttendance(userId, eventId, new AttendanceRequest(List.of(userId)));

        assertThat(s.getStatus()).isEqualTo(SignupStatus.ATTENDED);
        assertThat(s.getHoursValidated()).isEqualByComparingTo(new BigDecimal("3.00"));
    }

    @Test
    void adjustSignupHours_shouldUpdate_whenAttended() {
        UUID signupId = UUID.randomUUID();
        EventSignup s = new EventSignup();
        s.setId(signupId);
        s.setEvent(publishedEvent);
        s.setOccurrence(publishedOccurrence);
        s.setUser(user);
        s.setStatus(SignupStatus.ATTENDED);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(publishedEvent));
        doNothing().when(organizationService).verifyAdmin(userId, orgId);
        when(eventOccurrenceRepository.findById(occurrenceId)).thenReturn(Optional.of(publishedOccurrence));
        when(eventSignupRepository.findById(signupId)).thenReturn(Optional.of(s));

        SignupResponse res = eventService.adjustSignupHours(userId, eventId, occurrenceId, signupId, new BigDecimal("2.5"));

        assertThat(s.getHoursValidated()).isEqualByComparingTo(new BigDecimal("2.50"));
        assertThat(res.hoursValidated()).isEqualByComparingTo(new BigDecimal("2.50"));
    }

    @Test
    void adjustSignupHours_shouldFail_whenNotAttended() {
        UUID signupId = UUID.randomUUID();
        EventSignup s = new EventSignup();
        s.setId(signupId);
        s.setEvent(publishedEvent);
        s.setOccurrence(publishedOccurrence);
        s.setUser(user);
        s.setStatus(SignupStatus.REGISTERED);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(publishedEvent));
        doNothing().when(organizationService).verifyAdmin(userId, orgId);
        when(eventOccurrenceRepository.findById(occurrenceId)).thenReturn(Optional.of(publishedOccurrence));
        when(eventSignupRepository.findById(signupId)).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> eventService.adjustSignupHours(userId, eventId, occurrenceId, signupId, new BigDecimal("2.5")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("présence validée");
    }

    // --- PR3 : fiabilité ---

    @Test
    void markNoShow_shouldFlagRegisteredAsNoShow() {
        EventSignup s = new EventSignup();
        s.setId(UUID.randomUUID());
        s.setEvent(publishedEvent);
        s.setOccurrence(publishedOccurrence);
        s.setUser(user);
        s.setStatus(SignupStatus.REGISTERED);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(publishedEvent));
        doNothing().when(organizationService).verifyAdmin(userId, orgId);
        when(eventOccurrenceRepository.findById(occurrenceId)).thenReturn(Optional.of(publishedOccurrence));
        when(eventSignupRepository.findByOccurrenceIdAndUserId(occurrenceId, userId)).thenReturn(Optional.of(s));

        eventService.markNoShow(userId, eventId, occurrenceId, new AttendanceRequest(List.of(userId)));

        assertThat(s.getStatus()).isEqualTo(SignupStatus.NO_SHOW);
    }

    @Test
    void markAttendance_shouldCorrectNoShowToAttended() {
        EventSignup s = new EventSignup();
        s.setId(UUID.randomUUID());
        s.setEvent(publishedEvent);
        s.setOccurrence(publishedOccurrence);
        s.setUser(user);
        s.setStatus(SignupStatus.NO_SHOW);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(publishedEvent));
        doNothing().when(organizationService).verifyAdmin(userId, orgId);
        when(eventOccurrenceRepository.findByEventIdOrderByStartDateAsc(eventId))
                .thenReturn(List.of(publishedOccurrence));
        when(eventSignupRepository.findByOccurrenceIdAndUserId(occurrenceId, userId)).thenReturn(Optional.of(s));

        eventService.markAttendance(userId, eventId, new AttendanceRequest(List.of(userId)));

        assertThat(s.getStatus()).isEqualTo(SignupStatus.ATTENDED);
        assertThat(s.getHoursValidated()).isEqualByComparingTo(new BigDecimal("3.00"));
    }

    @Test
    void cancelSignup_shouldFlagLateCancel_whenWithin24h() {
        publishedOccurrence.setStartDate(LocalDateTime.now().plusHours(2));
        EventSignup s = new EventSignup();
        s.setId(UUID.randomUUID());
        s.setEvent(publishedEvent);
        s.setOccurrence(publishedOccurrence);
        s.setUser(user);
        s.setStatus(SignupStatus.REGISTERED);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(publishedEvent));
        when(eventOccurrenceRepository.findByEventIdOrderByStartDateAsc(eventId))
                .thenReturn(List.of(publishedOccurrence));
        when(eventSignupRepository.findByOccurrenceIdAndUserId(occurrenceId, userId)).thenReturn(Optional.of(s));

        eventService.cancelSignup(userId, eventId);

        assertThat(s.getStatus()).isEqualTo(SignupStatus.CANCELLED);
        assertThat(s.isLateCancel()).isTrue();
    }

    @Test
    void getReliability_shouldComputeRate() {
        UUID volunteerId = UUID.randomUUID();
        doNothing().when(organizationService).verifyAdmin(userId, orgId);
        when(eventSignupRepository.countByUserIdAndStatus(volunteerId, SignupStatus.ATTENDED)).thenReturn(8L);
        when(eventSignupRepository.countByUserIdAndStatus(volunteerId, SignupStatus.NO_SHOW)).thenReturn(1L);
        when(eventSignupRepository.countByUserIdAndLateCancelTrue(volunteerId)).thenReturn(1L);

        ReliabilityResponse r = eventService.getReliability(userId, orgId, volunteerId);

        assertThat(r.attendedCount()).isEqualTo(8L);
        assertThat(r.noShowCount()).isEqualTo(1L);
        assertThat(r.lateCancelCount()).isEqualTo(1L);
        assertThat(r.reliabilityRate()).isEqualTo(0.8);
    }
}
