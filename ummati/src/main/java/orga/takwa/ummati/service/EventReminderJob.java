package orga.takwa.ummati.service;

import orga.takwa.ummati.entity.Event;
import orga.takwa.ummati.entity.EventOccurrence;
import orga.takwa.ummati.entity.EventSignup;
import orga.takwa.ummati.entity.User;
import orga.takwa.ummati.repository.EventSignupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class EventReminderJob {

    private static final Logger log = LoggerFactory.getLogger(EventReminderJob.class);
    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("EEEE d MMMM yyyy 'à' HH'h'mm", java.util.Locale.FRENCH);

    private final EventSignupRepository eventSignupRepository;
    private final EmailService emailService;

    public EventReminderJob(EventSignupRepository eventSignupRepository, EmailService emailService) {
        this.eventSignupRepository = eventSignupRepository;
        this.emailService = emailService;
    }

    // Envoi à 08:00 UTC tous les jours — rappel pour les événements démarrant le lendemain
    @Scheduled(cron = "0 0 8 * * *", zone = "UTC")
    @Transactional(readOnly = true)
    public void sendReminders() {
        LocalDateTime tomorrowStart = LocalDate.now().plusDays(1).atStartOfDay();
        LocalDateTime tomorrowEnd = tomorrowStart.plusDays(1);

        List<EventSignup> signups = eventSignupRepository
                .findRegisteredSignupsForOccurrencesBetween(tomorrowStart, tomorrowEnd);

        if (signups.isEmpty()) {
            return;
        }

        for (EventSignup signup : signups) {
            try {
                User user = signup.getUser();
                Event event = signup.getEvent();
                EventOccurrence occurrence = signup.getOccurrence();
                String location = event.isOnline()
                        ? "En ligne"
                        : buildLocation(event);

                emailService.sendEventReminderEmail(
                        user.getEmail(),
                        user.getFirstName(),
                        event.getTitle(),
                        location,
                        occurrence.getStartDate().format(DISPLAY_FORMAT),
                        event.getOrganization().getName(),
                        event.getId().toString());
            } catch (Exception e) {
                log.error("Failed to send reminder for signup {}: {}", signup.getId(), e.getMessage());
            }
        }

        log.info("Event reminders sent: {} emails for events starting {}", signups.size(), tomorrowStart.toLocalDate());
    }

    private String buildLocation(Event event) {
        StringBuilder sb = new StringBuilder();
        if (event.getLocationName() != null && !event.getLocationName().isBlank()) {
            sb.append(event.getLocationName());
        }
        if (event.getLocationAddress() != null && !event.getLocationAddress().isBlank()) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(event.getLocationAddress());
        }
        if (event.getLocationCity() != null && !event.getLocationCity().isBlank()) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(event.getLocationCity());
        }
        return sb.isEmpty() ? "À confirmer" : sb.toString();
    }
}
