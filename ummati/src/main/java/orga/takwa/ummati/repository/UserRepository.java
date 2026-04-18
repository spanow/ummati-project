package orga.takwa.ummati.repository;

import orga.takwa.ummati.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    long countByCreatedAtAfter(LocalDateTime date);

    /** T-141: Find anonymized accounts eligible for purge (disabled + updated > 30 days ago) */
    @Query("SELECT u FROM User u WHERE u.enabled = false AND u.email LIKE 'deleted_%' AND u.updatedAt < :cutoff")
    List<User> findAnonymizedAccountsBefore(@Param("cutoff") LocalDateTime cutoff);
}
