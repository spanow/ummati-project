package orga.takwa.ummati.repository;

import orga.takwa.ummati.entity.EventSignup;
import orga.takwa.ummati.entity.enums.SignupStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventSignupRepository extends JpaRepository<EventSignup, UUID> {
    Optional<EventSignup> findByEventIdAndUserId(UUID eventId, UUID userId);
    boolean existsByEventIdAndUserId(UUID eventId, UUID userId);
    boolean existsByEventIdAndUserIdAndStatusIn(UUID eventId, UUID userId, Collection<SignupStatus> statuses);
    long countByEventIdAndStatus(UUID eventId, SignupStatus status);
    Page<EventSignup> findByEventId(UUID eventId, Pageable pageable);
    Page<EventSignup> findByUserId(UUID userId, Pageable pageable);
    long countByUserIdAndStatus(UUID userId, SignupStatus status);
    long countByUserId(UUID userId);
    Optional<EventSignup> findFirstByEventIdAndStatusOrderByRegisteredAtAsc(UUID eventId, SignupStatus status);
    List<EventSignup> findByEventIdAndStatusIn(UUID eventId, Collection<SignupStatus> statuses);

    @Query("""
            SELECT s FROM EventSignup s
            WHERE s.status = 'REGISTERED'
              AND s.event.status = 'PUBLISHED'
              AND s.event.startDate >= :from
              AND s.event.startDate < :to
            """)
    List<EventSignup> findRegisteredSignupsForEventsBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
