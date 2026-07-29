package orga.takwa.ummati.service;

import orga.takwa.ummati.entity.EventFavorite;
import orga.takwa.ummati.entity.enums.NotificationType;
import orga.takwa.ummati.repository.EventFavoriteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Relance sur les missions mises de côté dont le départ approche.
 *
 * <p>C'est le moment où un favori se transforme — ou non — en inscription. Un
 * bénévole qui a sauvegardé une mission a manifesté une intention ; sans rappel,
 * elle se perd simplement parce qu'il n'est pas revenu à temps.
 *
 * <p>Ne cible que ceux qui ne se sont pas encore inscrits : rappeler à quelqu'un
 * qu'il devrait s'inscrire à une mission où il a déjà sa place serait absurde.
 */
@Component
public class FavoriteClosingJob {

    private static final Logger log = LoggerFactory.getLogger(FavoriteClosingJob.class);

    /** Fenêtre de relance : assez tôt pour s'organiser, assez tard pour être utile. */
    private static final int DAYS_BEFORE = 3;

    private final EventFavoriteRepository favoriteRepository;
    private final NotificationService notificationService;

    public FavoriteClosingJob(EventFavoriteRepository favoriteRepository,
                              NotificationService notificationService) {
        this.favoriteRepository = favoriteRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 30 9 * * *", zone = "Europe/Paris")
    @Transactional
    public void remindFavoritesClosingSoon() {
        LocalDateTime now = LocalDateTime.now();
        // Fenêtre d'une journée pleine : la tâche tournant une fois par jour, un
        // intervalle plus étroit laisserait passer des missions entre deux exécutions.
        LocalDateTime from = now.plusDays(DAYS_BEFORE).toLocalDate().atStartOfDay();
        LocalDateTime to = from.plusDays(1);

        List<EventFavorite> favorites = favoriteRepository.findClosingSoonWithoutSignup(from, to);
        for (EventFavorite favorite : favorites) {
            notificationService.notify(
                    favorite.getUser(),
                    NotificationType.FAVORITE_CLOSING,
                    "« " + favorite.getEvent().getTitle() + " » c'est bientôt",
                    "Cette mission que vous aviez mise de côté a lieu dans " + DAYS_BEFORE
                            + " jours. Il reste des places.",
                    "/events/" + favorite.getEvent().getId());
        }

        if (!favorites.isEmpty()) {
            log.info("Relances sur missions favorites à venir : {}", favorites.size());
        }
    }
}
