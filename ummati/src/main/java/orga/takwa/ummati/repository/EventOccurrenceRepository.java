package orga.takwa.ummati.repository;

import orga.takwa.ummati.entity.EventOccurrence;
import orga.takwa.ummati.entity.enums.EventOccurrenceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface EventOccurrenceRepository extends JpaRepository<EventOccurrence, UUID> {

    List<EventOccurrence> findByEventIdOrderByStartDateAsc(UUID eventId);

    long countByEventId(UUID eventId);

    long countByEventIdAndStatus(UUID eventId, EventOccurrenceStatus status);

    // Job d'auto-complétion : créneaux publiés déjà terminés.
    List<EventOccurrence> findByStatusAndEndDateBefore(EventOccurrenceStatus status, LocalDateTime end);
}
