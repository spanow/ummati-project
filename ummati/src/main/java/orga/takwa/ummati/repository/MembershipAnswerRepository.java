package orga.takwa.ummati.repository;

import orga.takwa.ummati.entity.MembershipAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MembershipAnswerRepository extends JpaRepository<MembershipAnswer, UUID> {
    List<MembershipAnswer> findByMembershipId(UUID membershipId);
    void deleteByMembershipId(UUID membershipId);
}
