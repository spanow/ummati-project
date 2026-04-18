package orga.takwa.ummati.repository;

import orga.takwa.ummati.entity.VerificationToken;
import orga.takwa.ummati.entity.enums.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {
    Optional<VerificationToken> findByToken(String token);

    @Modifying
    @Query("UPDATE VerificationToken t SET t.usedAt = CURRENT_TIMESTAMP " +
           "WHERE t.user.id = :userId AND t.type = :type AND t.usedAt IS NULL")
    void invalidateAllByUserAndType(@Param("userId") UUID userId, @Param("type") TokenType type);
}

