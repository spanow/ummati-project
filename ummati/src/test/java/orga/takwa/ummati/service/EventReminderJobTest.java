package orga.takwa.ummati.service;

import orga.takwa.ummati.entity.Event;
import orga.takwa.ummati.entity.EventOccurrence;
import orga.takwa.ummati.entity.EventSignup;
import orga.takwa.ummati.entity.Organization;
import orga.takwa.ummati.entity.User;
import orga.takwa.ummati.entity.enums.EventOccurrenceStatus;
import orga.takwa.ummati.entity.enums.SignupStatus;
import orga.takwa.ummati.repository.EventSignupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventReminderJobTest {

    @Mock private EventSignupRepository eventSignupRepository;
    @Mock private EmailService emailService;

    @InjectMocks
    private EventReminderJob job;

    private EventSignup signup;
    private User user;
    private Event event;
    private Organization org;
    private EventOccurrence occurrence;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("volunteer@test.com");
        user.setFirstName("Alice");

        org = new Organization();
        org.setName("ONG Solidarité");

        event = new Event();
        event.setId(UUID.randomUUID());
        event.setTitle("Maraude Paris 13e");
        event.setOnline(false);
        event.setLocationName("Gare d'Austerlitz");
        event.setLocationCity("Paris");
        event.setStartDate(LocalDateTime.now().plusDays(1).withHour(9).withMinute(0));
        event.setOrganization(org);

        occurrence = new EventOccurrence();
        occurrence.setId(UUID.randomUUID());
        occurrence.setEvent(event);
        occurrence.setStartDate(event.getStartDate());
        occurrence.setStatus(EventOccurrenceStatus.PUBLISHED);

        signup = new EventSignup();
        signup.setId(UUID.randomUUID());
        signup.setUser(user);
        signup.setEvent(event);
        signup.setOccurrence(occurrence);
        signup.setStatus(SignupStatus.REGISTERED);
    }

    @Test
    void sendReminders_sendsOneEmailPerRegisteredSignup() {
        when(eventSignupRepository.findRegisteredSignupsForOccurrencesBetween(any(), any()))
                .thenReturn(List.of(signup));

        job.sendReminders();

        verify(emailService, times(1)).sendEventReminderEmail(
                eq("volunteer@test.com"),
                eq("Alice"),
                eq("Maraude Paris 13e"),
                anyString(),
                anyString(),
                eq("ONG Solidarité"),
                eq(event.getId().toString()));
    }

    @Test
    void sendReminders_noSignups_sendsNoEmail() {
        when(eventSignupRepository.findRegisteredSignupsForOccurrencesBetween(any(), any()))
                .thenReturn(List.of());

        job.sendReminders();

        verifyNoInteractions(emailService);
    }

    @Test
    void sendReminders_usesCorrectTimeWindow() {
        when(eventSignupRepository.findRegisteredSignupsForOccurrencesBetween(any(), any()))
                .thenReturn(List.of());

        job.sendReminders();

        ArgumentCaptor<LocalDateTime> fromCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> toCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(eventSignupRepository).findRegisteredSignupsForOccurrencesBetween(fromCaptor.capture(), toCaptor.capture());

        LocalDateTime from = fromCaptor.getValue();
        LocalDateTime to = toCaptor.getValue();
        assertThat(from.getHour()).isZero();
        assertThat(from.getMinute()).isZero();
        assertThat(to).isEqualTo(from.plusDays(1));
        assertThat(from.toLocalDate()).isEqualTo(java.time.LocalDate.now().plusDays(1));
    }

    @Test
    void sendReminders_onlineEvent_displaysEnLigne() {
        event.setOnline(true);
        when(eventSignupRepository.findRegisteredSignupsForOccurrencesBetween(any(), any()))
                .thenReturn(List.of(signup));

        job.sendReminders();

        verify(emailService).sendEventReminderEmail(
                any(), any(), any(),
                eq("En ligne"),
                any(), any(), any());
    }

    @Test
    void sendReminders_emailFailure_continuesWithOtherSignups() {
        EventSignup signup2 = new EventSignup();
        signup2.setId(UUID.randomUUID());
        signup2.setUser(user);
        signup2.setEvent(event);
        signup2.setOccurrence(occurrence);
        signup2.setStatus(SignupStatus.REGISTERED);

        when(eventSignupRepository.findRegisteredSignupsForOccurrencesBetween(any(), any()))
                .thenReturn(List.of(signup, signup2));
        doThrow(new RuntimeException("SMTP error"))
                .doNothing()
                .when(emailService).sendEventReminderEmail(any(), any(), any(), any(), any(), any(), any());

        job.sendReminders();

        // Both calls were attempted despite the first failure
        verify(emailService, times(2)).sendEventReminderEmail(any(), any(), any(), any(), any(), any(), any());
    }
}
