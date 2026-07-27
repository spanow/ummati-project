package orga.takwa.ummati.service;

import orga.takwa.ummati.dto.event.AttendanceRequest;
import orga.takwa.ummati.dto.event.EventStatusRequest;
import orga.takwa.ummati.dto.event.SignupResponse;
import orga.takwa.ummati.entity.*;
import orga.takwa.ummati.entity.enums.*;
import orga.takwa.ummati.exception.BusinessRuleException;
import orga.takwa.ummati.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Règles d'inscription et de changement d'état corrigées lors de l'audit.
 */
@ExtendWith(MockitoExtension.class)
class EventServiceSignupRulesTest {

    @Mock private EventRepository eventRepository;
    @Mock private EventOccurrenceRepository occurrenceRepository;
    @Mock private EventSignupRepository eventSignupRepository;
    @Mock private EventFeedbackRepository eventFeedbackRepository;
    @Mock private OrganizationService organizationService;
    @Mock private UserRepository userRepository;
    @Mock private SkillRepository skillRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private NotificationService notificationService;
    @Mock private AuditService auditService;
    @Mock private EventPhotoRepository eventPhotoRepository;
    @Mock private ImageService imageService;

    @InjectMocks private EventService eventService;

    private UUID userId;
    private UUID eventId;
    private UUID occurrenceId;
    private User user;
    private Event event;
    private EventOccurrence occurrence;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        eventId = UUID.randomUUID();
        occurrenceId = UUID.randomUUID();

        Organization org = new Organization();
        org.setId(UUID.randomUUID());
        org.setName("Croix-Rouge");
        org.setSlug("croix-rouge");
        org.setStatus(OrganizationStatus.ACTIVE);

        user = new User();
        user.setId(userId);
        user.setFirstName("Amina");
        user.setLastName("B");
        user.setEmail("amina@test.com");

        event = new Event();
        event.setId(eventId);
        event.setOrganization(org);
        event.setTitle("Maraude");
        event.setType(EventType.MARAUDE);
        event.setStatus(EventStatus.PUBLISHED);
        event.setStartDate(LocalDateTime.now().plusDays(5));
        event.setEndDate(LocalDateTime.now().plusDays(5).plusHours(3));

        occurrence = new EventOccurrence();
        occurrence.setId(occurrenceId);
        occurrence.setEvent(event);
        occurrence.setStartDate(event.getStartDate());
        occurrence.setEndDate(event.getEndDate());
        occurrence.setStatus(EventOccurrenceStatus.PUBLISHED);
    }

    private void givenSoleOccurrence() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(occurrenceRepository.findByEventIdOrderByStartDateAsc(eventId))
                .thenReturn(List.of(occurrence));
        lenient().when(occurrenceRepository.findByIdForUpdate(occurrenceId))
                .thenReturn(Optional.of(occurrence));
    }

    // --- Âge minimum ---

    @Test
    void signup_shouldRefuse_whenEventHasMinAgeAndBirthDateIsMissing() {
        // Auparavant le contrôle était sauté : ne pas renseigner sa date de naissance
        // suffisait à s'inscrire à une mission réservée aux majeurs.
        event.setMinAge(18);
        user.setDateOfBirth(null);
        givenSoleOccurrence();

        assertThatThrownBy(() -> eventService.signup(userId, eventId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("date de naissance");

        verify(eventSignupRepository, never()).save(any());
    }

    @Test
    void signup_shouldRefuse_whenVolunteerIsTooYoung() {
        event.setMinAge(18);
        user.setDateOfBirth(LocalDate.now().minusYears(15));
        givenSoleOccurrence();

        assertThatThrownBy(() -> eventService.signup(userId, eventId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("au moins 18 ans");
    }

    @Test
    void signup_shouldAllow_whenNoMinAgeIsSetAndBirthDateIsMissing() {
        event.setMinAge(null);
        user.setDateOfBirth(null);
        givenSoleOccurrence();
        when(eventSignupRepository.findByOccurrenceIdAndUserId(occurrenceId, userId)).thenReturn(Optional.empty());
        when(eventSignupRepository.countByOccurrenceIdAndStatus(occurrenceId, SignupStatus.REGISTERED)).thenReturn(0L);
        when(eventSignupRepository.save(any(EventSignup.class))).thenAnswer(inv -> {
            EventSignup s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            s.setRegisteredAt(LocalDateTime.now());
            return s;
        });

        assertThat(eventService.signup(userId, eventId).status()).isEqualTo("REGISTERED");
    }

    // --- Anti-surréservation ---

    @Test
    void signup_shouldReloadTheOccurrenceUnderLock_beforeCountingSeats() {
        givenSoleOccurrence();
        when(eventSignupRepository.findByOccurrenceIdAndUserId(occurrenceId, userId)).thenReturn(Optional.empty());
        when(eventSignupRepository.countByOccurrenceIdAndStatus(occurrenceId, SignupStatus.REGISTERED)).thenReturn(0L);
        when(eventSignupRepository.save(any(EventSignup.class))).thenAnswer(inv -> {
            EventSignup s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            s.setRegisteredAt(LocalDateTime.now());
            return s;
        });

        eventService.signup(userId, eventId);

        // Le verrou de ligne est ce qui empêche deux inscriptions simultanées de lire
        // la même dernière place.
        verify(occurrenceRepository).findByIdForUpdate(occurrenceId);
    }

    // --- Transitions d'état ---

    @Test
    void changeStatus_shouldRefuseCancellingACompletedEvent() {
        event.setStatus(EventStatus.COMPLETED);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(occurrenceRepository.findByEventIdOrderByStartDateAsc(eventId)).thenReturn(List.of(occurrence));

        assertThatThrownBy(() -> eventService.changeStatus(userId, eventId,
                new EventStatusRequest("CANCEL", "Météo défavorable et route coupée")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("terminé");

        assertThat(event.getStatus()).isEqualTo(EventStatus.COMPLETED);
    }

    @Test
    void changeStatus_shouldRefuseCancellingATwiceCancelledEvent() {
        event.setStatus(EventStatus.CANCELLED);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(occurrenceRepository.findByEventIdOrderByStartDateAsc(eventId)).thenReturn(List.of(occurrence));

        assertThatThrownBy(() -> eventService.changeStatus(userId, eventId,
                new EventStatusRequest("CANCEL", "Motif suffisamment long")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("annulé");
    }

    // --- Inscription affichée ---

    @Test
    void getMySignup_shouldPreferTheActiveSignupOverACancelledOne() {
        // Sur une série, une inscription annulée sur un créneau ne doit pas masquer une
        // inscription active sur un autre : le front affichait « S'inscrire » à un
        // bénévole déjà inscrit.
        EventSignup cancelled = signup(SignupStatus.CANCELLED, LocalDateTime.now().minusDays(3));
        EventSignup registered = signup(SignupStatus.REGISTERED, LocalDateTime.now().minusDays(1));
        when(eventSignupRepository.findByEventIdAndUserId(eventId, userId))
                .thenReturn(List.of(cancelled, registered));

        Optional<SignupResponse> result = eventService.getMySignup(userId, eventId);

        assertThat(result).isPresent();
        assertThat(result.get().status()).isEqualTo("REGISTERED");
    }

    @Test
    void getMySignup_shouldFallBackToTheCancelledSignup_whenNoneIsActive() {
        EventSignup cancelled = signup(SignupStatus.CANCELLED, LocalDateTime.now().minusDays(3));
        when(eventSignupRepository.findByEventIdAndUserId(eventId, userId)).thenReturn(List.of(cancelled));

        assertThat(eventService.getMySignup(userId, eventId))
                .isPresent()
                .get()
                .extracting(SignupResponse::status).isEqualTo("CANCELLED");
    }

    @Test
    void getMySignup_shouldPreferRegisteredOverWaitlisted() {
        EventSignup waitlisted = signup(SignupStatus.WAITLISTED, LocalDateTime.now().minusDays(2));
        EventSignup registered = signup(SignupStatus.REGISTERED, LocalDateTime.now().minusDays(4));
        when(eventSignupRepository.findByEventIdAndUserId(eventId, userId))
                .thenReturn(List.of(waitlisted, registered));

        assertThat(eventService.getMySignup(userId, eventId))
                .get().extracting(SignupResponse::status).isEqualTo("REGISTERED");
    }

    // --- Demande d'avis ---

    @Test
    void markAttendance_shouldRequestFeedbackOnlyOnce_acrossASeries() {
        // Le bénévole a déjà été marqué présent sur un autre créneau de la même série :
        // un seul avis est possible par événement, donc une seule notification.
        EventSignup toMark = signup(SignupStatus.REGISTERED, LocalDateTime.now().minusDays(1));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(occurrenceRepository.findByEventIdOrderByStartDateAsc(eventId)).thenReturn(List.of(occurrence));
        when(eventSignupRepository.findByOccurrenceIdAndUserId(occurrenceId, userId))
                .thenReturn(Optional.of(toMark));
        when(eventSignupRepository.existsByEventIdAndUserIdAndStatus(eventId, userId, SignupStatus.ATTENDED))
                .thenReturn(true);

        eventService.markAttendance(userId, eventId, new AttendanceRequest(List.of(userId)));

        assertThat(toMark.getStatus()).isEqualTo(SignupStatus.ATTENDED);
        verify(notificationService, never()).saveNotification(
                any(), eq(NotificationType.FEEDBACK_REQUESTED), anyString(), anyString(), anyString());
    }

    @Test
    void markAttendance_shouldRequestFeedback_onTheFirstAttendance() {
        EventSignup toMark = signup(SignupStatus.REGISTERED, LocalDateTime.now().minusDays(1));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(occurrenceRepository.findByEventIdOrderByStartDateAsc(eventId)).thenReturn(List.of(occurrence));
        when(eventSignupRepository.findByOccurrenceIdAndUserId(occurrenceId, userId))
                .thenReturn(Optional.of(toMark));
        when(eventSignupRepository.existsByEventIdAndUserIdAndStatus(eventId, userId, SignupStatus.ATTENDED))
                .thenReturn(false);

        eventService.markAttendance(userId, eventId, new AttendanceRequest(List.of(userId)));

        verify(notificationService).saveNotification(
                eq(user), eq(NotificationType.FEEDBACK_REQUESTED), anyString(), anyString(), anyString());
    }

    private EventSignup signup(SignupStatus status, LocalDateTime registeredAt) {
        EventSignup s = new EventSignup();
        s.setId(UUID.randomUUID());
        s.setEvent(event);
        s.setOccurrence(occurrence);
        s.setUser(user);
        s.setStatus(status);
        s.setRegisteredAt(registeredAt);
        return s;
    }
}
