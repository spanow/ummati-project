package orga.takwa.ummati.repository;

import orga.takwa.ummati.entity.Membership;
import orga.takwa.ummati.entity.enums.MembershipRole;
import orga.takwa.ummati.entity.enums.MembershipStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {
    Optional<Membership> findByUserIdAndOrganizationId(UUID userId, UUID orgId);
    boolean existsByUserIdAndOrganizationId(UUID userId, UUID orgId);
    Page<Membership> findByOrganizationIdAndStatus(UUID orgId, MembershipStatus status, Pageable pageable);
    Page<Membership> findByOrganizationId(UUID orgId, Pageable pageable);
    Page<Membership> findByUserIdAndStatus(UUID userId, MembershipStatus status, Pageable pageable);
    long countByOrganizationIdAndRoleAndStatus(UUID orgId, MembershipRole role, MembershipStatus status);
    long countByOrganizationIdAndStatus(UUID orgId, MembershipStatus status);
    long countByUserIdAndStatus(UUID userId, MembershipStatus status);
    List<Membership> findByOrganizationIdAndRoleAndStatus(UUID orgId, MembershipRole role, MembershipStatus status);
    List<Membership> findByOrganizationIdAndRoleInAndStatus(UUID orgId, Collection<MembershipRole> roles, MembershipStatus status);
}

