package orga.takwa.ummati.repository;

import orga.takwa.ummati.entity.EventFeedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface EventFeedbackRepository extends JpaRepository<EventFeedback, UUID> {
    Page<EventFeedback> findByEventId(UUID eventId, Pageable pageable);
    boolean existsByEventIdAndUserId(UUID eventId, UUID userId);

    @Query("SELECT AVG(f.rating) FROM EventFeedback f WHERE f.event.id = :eventId")
    Double findAverageRatingByEventId(@Param("eventId") UUID eventId);

    @Query("SELECT AVG(f.rating) FROM EventFeedback f WHERE f.event.organization.id = :orgId")
    Double findAverageRatingByOrganizationId(@Param("orgId") UUID orgId);
}

