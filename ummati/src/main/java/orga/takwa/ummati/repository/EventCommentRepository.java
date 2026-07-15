package orga.takwa.ummati.repository;

import orga.takwa.ummati.entity.EventComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EventCommentRepository extends JpaRepository<EventComment, UUID> {
    Page<EventComment> findByEventIdOrderByCreatedAtAsc(UUID eventId, Pageable pageable);
    long countByEventId(UUID eventId);
}
