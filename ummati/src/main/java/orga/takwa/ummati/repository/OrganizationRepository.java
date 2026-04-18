package orga.takwa.ummati.repository;

import orga.takwa.ummati.entity.Organization;
import orga.takwa.ummati.entity.enums.OrganizationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, UUID>, JpaSpecificationExecutor<Organization> {
    Optional<Organization> findBySlug(String slug);
    boolean existsByName(String name);
    boolean existsBySlug(String slug);
    Page<Organization> findByStatus(OrganizationStatus status, Pageable pageable);
    long countByStatus(OrganizationStatus status);
}
