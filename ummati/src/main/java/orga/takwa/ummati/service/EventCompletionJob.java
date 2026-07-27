package orga.takwa.ummati.service;

import orga.takwa.ummati.entity.Event;
import orga.takwa.ummati.entity.EventOccurrence;
import orga.takwa.ummati.entity.Membership;
import orga.takwa.ummati.entity.Organization;
import orga.takwa.ummati.entity.enums.EventOccurrenceStatus;
import orga.takwa.ummati.entity.enums.EventStatus;
import orga.takwa.ummati.entity.enums.MembershipRole;
import orga.takwa.ummati.entity.enums.MembershipStatus;
import orga.takwa.ummati.entity.enums.NotificationType;
import orga.takwa.ummati.repository.EventOccurrenceRepository;
import orga.takwa.ummati.repository.EventPhotoRepository;
import orga.takwa.ummati.repository.EventRepository;
import orga.takwa.ummati.repository.MembershipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class EventCompletionJob {

    private static final Logger log = LoggerFactory.getLogger(EventCompletionJob.class);

    private final EventRepository eventRepository;
    private final EventOccurrenceRepository occurrenceRepository;
    private final EventPhotoRepository eventPhotoRepository;
    private final MembershipRepository membershipRepository;
    private final NotificationService notificationService;

    public EventCompletionJob(EventRepository eventRepository, EventOccurrenceRepository occurrenceRepository,
                              EventPhotoRepository eventPhotoRepository,
                              MembershipRepository membershipRepository,
                              NotificationService notificationService) {
        this.eventRepository = eventRepository;
        this.occurrenceRepository = occurrenceRepository;
        this.eventPhotoRepository = eventPhotoRepository;
        this.membershipRepository = membershipRepository;
        this.notificationService = notificationService;
    }

    // T-082: Auto-complete past occurrences daily at 03:00 UTC.
    // Un créneau publié dont la fin est passée devient COMPLETED ; la série (Event) est
    // marquée COMPLETED lorsqu'il ne lui reste plus aucun créneau publié.
    @Scheduled(cron = "0 0 3 * * *", zone = "UTC")
    @Transactional
    public void completeExpiredEvents() {
        LocalDateTime now = LocalDateTime.now();
        List<EventOccurrence> pastOccurrences =
                occurrenceRepository.findByStatusAndEndDateBefore(EventOccurrenceStatus.PUBLISHED, now);

        Set<UUID> touchedEventIds = new HashSet<>();
        for (EventOccurrence o : pastOccurrences) {
            o.setStatus(EventOccurrenceStatus.COMPLETED);
            touchedEventIds.add(o.getEvent().getId());
        }
        occurrenceRepository.saveAll(pastOccurrences);

        for (UUID eventId : touchedEventIds) {
            if (occurrenceRepository.countByEventIdAndStatus(eventId, EventOccurrenceStatus.PUBLISHED) == 0) {
                eventRepository.findById(eventId).ifPresent(event -> {
                    if (event.getStatus() == EventStatus.PUBLISHED) {
                        event.setStatus(EventStatus.COMPLETED);
                        eventRepository.save(event);
                        log.info("Event '{}' (id={}) auto-completed", event.getTitle(), event.getId());
                        requestPhotosIfGalleryEmpty(event);
                    }
                });
            }
        }

        if (!pastOccurrences.isEmpty()) {
            log.info("Auto-completed {} occurrences", pastOccurrences.size());
        }
    }

    /**
     * Invite les admins de l'ONG à alimenter la galerie de la mission qui vient de se
     * terminer. Sans cette relance, les galeries resteraient vides : personne ne pense
     * spontanément à rouvrir une mission terminée.
     *
     * <p>Silencieux si des photos existent déjà — inutile de relancer une ONG qui a joué
     * le jeu. La notification n'est envoyée qu'une fois, au passage en COMPLETED.
     */
    private void requestPhotosIfGalleryEmpty(Event event) {
        if (eventPhotoRepository.countByEventId(event.getId()) > 0) {
            return;
        }
        Organization org = event.getOrganization();
        List<Membership> admins = membershipRepository.findByOrganizationIdAndRoleAndStatus(
                org.getId(), MembershipRole.ADMIN, MembershipStatus.ACTIVE);

        for (Membership admin : admins) {
            notificationService.saveNotification(
                    admin.getUser(),
                    NotificationType.EVENT_PHOTOS_REQUESTED,
                    "Ajoutez des photos de « " + event.getTitle() + " »",
                    "La mission est terminée. Quelques photos en feront la meilleure vitrine "
                            + "pour recruter des bénévoles la prochaine fois.",
                    "/organizations/" + org.getId() + "/events/manage");
        }
    }
}
