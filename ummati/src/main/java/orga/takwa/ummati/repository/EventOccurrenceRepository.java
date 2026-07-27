package orga.takwa.ummati.repository;

import jakarta.persistence.LockModeType;
import orga.takwa.ummati.entity.EventOccurrence;
import orga.takwa.ummati.entity.enums.EventOccurrenceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventOccurrenceRepository extends JpaRepository<EventOccurrence, UUID> {

    List<EventOccurrence> findByEventIdOrderByStartDateAsc(UUID eventId);

    /**
     * Charge un créneau en verrouillant sa ligne jusqu'à la fin de la transaction.
     *
     * <p>Utilisé à l'inscription : le comptage des places puis l'écriture de
     * l'inscription doivent être atomiques, sinon deux inscriptions simultanées lisent
     * toutes les deux « il reste une place » et l'événement se retrouve en surnombre.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM EventOccurrence o WHERE o.id = :id")
    Optional<EventOccurrence> findByIdForUpdate(@Param("id") UUID id);

    long countByEventId(UUID eventId);

    long countByEventIdAndStatus(UUID eventId, EventOccurrenceStatus status);

    // Job d'auto-complétion : créneaux publiés déjà terminés.
    List<EventOccurrence> findByStatusAndEndDateBefore(EventOccurrenceStatus status, LocalDateTime end);
}
