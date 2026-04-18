package orga.takwa.ummati.service;

import orga.takwa.ummati.entity.Event;
import orga.takwa.ummati.entity.enums.EventStatus;
import orga.takwa.ummati.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class EventCompletionJob {

    private static final Logger log = LoggerFactory.getLogger(EventCompletionJob.class);

    private final EventRepository eventRepository;

    public EventCompletionJob(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    // T-082: Auto-complete past events daily at 03:00 UTC
    @Scheduled(cron = "0 0 3 * * *", zone = "UTC")
    @Transactional
    public void completeExpiredEvents() {
        List<Event> pastEvents = eventRepository.findPublishedPastEvents(LocalDateTime.now());
        for (Event event : pastEvents) {
            event.setStatus(EventStatus.COMPLETED);
            eventRepository.save(event);
            log.info("Event '{}' (id={}) auto-completed", event.getTitle(), event.getId());
        }
        if (!pastEvents.isEmpty()) {
            log.info("Auto-completed {} events", pastEvents.size());
        }
    }
}

