package orga.takwa.ummati.repository;

import orga.takwa.ummati.entity.MembershipQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MembershipQuestionRepository extends JpaRepository<MembershipQuestion, UUID> {
    List<MembershipQuestion> findByOrganizationIdOrderByPositionAsc(UUID organizationId);
    long countByOrganizationId(UUID organizationId);
}
