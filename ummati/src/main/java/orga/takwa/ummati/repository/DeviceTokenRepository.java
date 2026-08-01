package orga.takwa.ummati.repository;

import orga.takwa.ummati.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {

    List<DeviceToken> findByUserId(UUID userId);

    Optional<DeviceToken> findByToken(String token);

    void deleteByUserIdAndToken(UUID userId, String token);

    /** Purge des installations désinstallées ou inactives depuis longtemps. */
    long deleteByLastSeenAtBefore(LocalDateTime cutoff);
}
