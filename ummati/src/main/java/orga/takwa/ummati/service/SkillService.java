package orga.takwa.ummati.service;

import orga.takwa.ummati.dto.skill.SkillResponse;
import orga.takwa.ummati.entity.enums.SkillCategory;
import orga.takwa.ummati.repository.SkillRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SkillService {

    private final SkillRepository skillRepository;

    public SkillService(SkillRepository skillRepository) {
        this.skillRepository = skillRepository;
    }

    @Cacheable("skills")
    public List<SkillResponse> getAllSkills() {
        return skillRepository.findAll().stream()
                .map(s -> new SkillResponse(s.getId(), s.getName(), s.getCategory().name()))
                .toList();
    }

    public List<SkillResponse> getByCategory(String category) {
        SkillCategory cat = SkillCategory.valueOf(category.toUpperCase());
        return skillRepository.findByCategory(cat).stream()
                .map(s -> new SkillResponse(s.getId(), s.getName(), s.getCategory().name()))
                .toList();
    }

    public List<SkillResponse> search(String query) {
        return skillRepository.findByNameContainingIgnoreCase(query).stream()
                .map(s -> new SkillResponse(s.getId(), s.getName(), s.getCategory().name()))
                .toList();
    }
}

