package orga.takwa.ummati.repository;

import orga.takwa.ummati.entity.MissionAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface MissionAlertRepository extends JpaRepository<MissionAlert, UUID> {

    List<MissionAlert> findByUserIdOrderByCreatedAtDesc(UUID userId);

    long countByUserId(UUID userId);

    /** Alertes actives, utilisateur chargé — parcourues par la tâche d'envoi. */
    @Query("SELECT a FROM MissionAlert a JOIN FETCH a.user WHERE a.enabled = true")
    List<MissionAlert> findEnabledWithUser();
}
