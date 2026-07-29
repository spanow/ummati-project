package orga.takwa.ummati.repository;

import orga.takwa.ummati.entity.Event;
import orga.takwa.ummati.entity.enums.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID>, JpaSpecificationExecutor<Event> {
    Page<Event> findByOrganizationId(UUID orgId, Pageable pageable);
    long countByOrganizationId(UUID orgId);
    long countByOrganizationIdAndCreatedAtAfter(UUID orgId, LocalDateTime date);
    Page<Event> findByStatusAndStartDateAfter(EventStatus status, LocalDateTime date, Pageable pageable);

    /**
     * Missions publiées depuis une date et pas encore passées, ONG chargée.
     *
     * <p>Base des alertes : sans ce filtre en base, chaque alerte relirait toute la
     * table pour n'en garder qu'une poignée de lignes.
     */
    @Query("""
            SELECT e FROM Event e
            JOIN FETCH e.organization
            WHERE e.status = 'PUBLISHED'
              AND e.createdAt > :since
              AND e.startDate > :now
            ORDER BY e.startDate ASC
            """)
    List<Event> findPublishedSince(@Param("since") LocalDateTime since, @Param("now") LocalDateTime now);

    @Query("SELECT e FROM Event e WHERE e.status = 'PUBLISHED' AND e.endDate < :now")
    List<Event> findPublishedPastEvents(@Param("now") LocalDateTime now);

    Page<Event> findByOrganizationIdAndStatus(UUID orgId, EventStatus status, Pageable pageable);

    long countByCreatedAtAfter(LocalDateTime date);
}
