package orga.takwa.ummati.service;

import orga.takwa.ummati.entity.EventOccurrence;
import orga.takwa.ummati.entity.enums.EventOccurrenceStatus;
import orga.takwa.ummati.entity.enums.EventStatus;
import orga.takwa.ummati.repository.EventOccurrenceRepository;
import orga.takwa.ummati.repository.EventRepository;
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

    public EventCompletionJob(EventRepository eventRepository, EventOccurrenceRepository occurrenceRepository) {
        this.eventRepository = eventRepository;
        this.occurrenceRepository = occurrenceRepository;
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
                    }
                });
            }
        }

        if (!pastOccurrences.isEmpty()) {
            log.info("Auto-completed {} occurrences", pastOccurrences.size());
        }
    }
}
