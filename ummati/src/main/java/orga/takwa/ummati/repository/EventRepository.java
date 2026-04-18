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

    @Query("SELECT e FROM Event e WHERE e.status = 'PUBLISHED' AND e.endDate < :now")
    List<Event> findPublishedPastEvents(@Param("now") LocalDateTime now);

    Page<Event> findByOrganizationIdAndStatus(UUID orgId, EventStatus status, Pageable pageable);

    long countByCreatedAtAfter(LocalDateTime date);
}
