package orga.takwa.ummati.repository;

import orga.takwa.ummati.entity.OrganizationFollow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OrganizationFollowRepository extends JpaRepository<OrganizationFollow, UUID> {

    boolean existsByUserIdAndOrganizationId(UUID userId, UUID organizationId);

    void deleteByUserIdAndOrganizationId(UUID userId, UUID organizationId);

    Page<OrganizationFollow> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    long countByOrganizationId(UUID organizationId);

    /** Abonnés d'une ONG, utilisateur chargé — pour notifier à la publication. */
    @Query("SELECT f FROM OrganizationFollow f JOIN FETCH f.user WHERE f.organization.id = :orgId")
    List<OrganizationFollow> findFollowersWithUser(@Param("orgId") UUID orgId);
}
