package orga.takwa.ummati.repository;

import orga.takwa.ummati.entity.EventPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventPhotoRepository extends JpaRepository<EventPhoto, UUID> {

    List<EventPhoto> findByEventIdOrderByPositionAscCreatedAtAsc(UUID eventId);

    long countByEventId(UUID eventId);
}
