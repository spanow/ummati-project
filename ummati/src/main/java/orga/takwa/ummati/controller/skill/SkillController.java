package orga.takwa.ummati.controller.skill;

import orga.takwa.ummati.dto.ApiResponse;
import orga.takwa.ummati.dto.skill.SkillResponse;
import orga.takwa.ummati.service.SkillService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/skills")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SkillResponse>>> getSkills(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {
        List<SkillResponse> skills;
        if (category != null) {
            skills = skillService.getByCategory(category);
        } else if (search != null) {
            skills = skillService.search(search);
        } else {
            skills = skillService.getAllSkills();
        }
        return ResponseEntity.ok(ApiResponse.ok(skills));
    }
}

