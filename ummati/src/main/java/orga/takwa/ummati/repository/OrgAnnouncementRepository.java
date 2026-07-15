package orga.takwa.ummati.repository;

import orga.takwa.ummati.entity.OrgAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrgAnnouncementRepository extends JpaRepository<OrgAnnouncement, UUID> {
    List<OrgAnnouncement> findByOrganizationIdOrderByPinnedDescCreatedAtDesc(UUID orgId);
}
