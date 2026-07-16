package orga.takwa.ummati.repository;

import orga.takwa.ummati.entity.EventAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EventAnnouncementRepository extends JpaRepository<EventAnnouncement, UUID> {
    List<EventAnnouncement> findByEventIdOrderByPinnedDescCreatedAtAsc(UUID eventId);
}
