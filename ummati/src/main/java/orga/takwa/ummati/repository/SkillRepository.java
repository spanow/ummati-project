package orga.takwa.ummati.repository;

import orga.takwa.ummati.entity.Skill;
import orga.takwa.ummati.entity.enums.SkillCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SkillRepository extends JpaRepository<Skill, UUID> {
    List<Skill> findByCategory(SkillCategory category);
    List<Skill> findByNameContainingIgnoreCase(String name);
    Optional<Skill> findByNameIgnoreCase(String name);
}

