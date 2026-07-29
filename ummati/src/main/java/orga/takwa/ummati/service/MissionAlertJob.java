package orga.takwa.ummati.service;

import orga.takwa.ummati.entity.Event;
import orga.takwa.ummati.entity.MissionAlert;
import orga.takwa.ummati.entity.enums.AlertFrequency;
import orga.takwa.ummati.entity.enums.NotificationType;
import orga.takwa.ummati.repository.MissionAlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Envoi périodique des alertes missions.
 *
 * <p>Tourne chaque matin : les alertes quotidiennes partent tous les jours, les
 * hebdomadaires seulement si sept jours se sont écoulés depuis le dernier envoi.
 * Une alerte sans correspondance ne déclenche rien — un email « aucune nouvelle
 * mission » ne rend service à personne et use la patience du destinataire.
 */
@Component
public class MissionAlertJob {

    private static final Logger log = LoggerFactory.getLogger(MissionAlertJob.class);

    private final MissionAlertRepository alertRepository;
    private final MissionAlertService alertService;
    private final NotificationService notificationService;

    public MissionAlertJob(MissionAlertRepository alertRepository,
                           MissionAlertService alertService,
                           NotificationService notificationService) {
        this.alertRepository = alertRepository;
        this.alertService = alertService;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 0 8 * * *", zone = "Europe/Paris")
    @Transactional
    public void sendDueAlerts() {
        LocalDateTime now = LocalDateTime.now();
        List<MissionAlert> alerts = alertRepository.findEnabledWithUser();

        int sent = 0;
        for (MissionAlert alert : alerts) {
            if (!isDue(alert, now)) {
                continue;
            }
            List<Event> matches = alertService.findMatches(alert, now);
            // On repositionne la borne même sans résultat : sinon la fenêtre de
            // recherche s'élargirait indéfiniment et finirait par tout remonter.
            alertService.markSent(alert, now);
            if (matches.isEmpty()) {
                continue;
            }

            notificationService.notify(
                    alert.getUser(),
                    NotificationType.MISSION_ALERT,
                    matches.size() == 1
                            ? "Une nouvelle mission pour « " + alert.getLabel() + " »"
                            : matches.size() + " nouvelles missions pour « " + alert.getLabel() + " »",
                    summarize(matches),
                    "/events");
            sent++;
        }

        if (sent > 0) {
            log.info("Alertes missions envoyées : {} sur {} alertes actives", sent, alerts.size());
        }
    }

    /** Une alerte est due si sa cadence est écoulée depuis le dernier envoi. */
    static boolean isDue(MissionAlert alert, LocalDateTime now) {
        if (alert.getLastSentAt() == null) {
            return true;
        }
        long days = alert.getFrequency() == AlertFrequency.DAILY ? 1 : 7;
        return !alert.getLastSentAt().plusDays(days).isAfter(now);
    }

    /** Titres des missions trouvées, sur une ligne. */
    private static String summarize(List<Event> matches) {
        StringBuilder sb = new StringBuilder();
        for (Event e : matches) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append(e.getTitle());
        }
        return sb.toString();
    }
}
