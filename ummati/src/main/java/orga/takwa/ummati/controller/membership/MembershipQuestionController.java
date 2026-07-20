package orga.takwa.ummati.controller.membership;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import orga.takwa.ummati.config.security.CurrentUser;
import orga.takwa.ummati.dto.ApiResponse;
import orga.takwa.ummati.dto.membership.AnswerResponse;
import orga.takwa.ummati.dto.membership.CreateQuestionRequest;
import orga.takwa.ummati.dto.membership.QuestionResponse;
import orga.takwa.ummati.service.MembershipQuestionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Questionnaire d'adhésion", description = "Questions d'adhésion configurables par ONG")
public class MembershipQuestionController {

    private final MembershipQuestionService service;

    public MembershipQuestionController(MembershipQuestionService service) {
        this.service = service;
    }

    // Liste des questions d'une ONG — public (alimente le formulaire d'adhésion)
    @GetMapping("/organizations/{orgId}/membership-questions")
    public ResponseEntity<ApiResponse<List<QuestionResponse>>> list(@PathVariable UUID orgId) {
        return ResponseEntity.ok(ApiResponse.ok(service.listQuestions(orgId)));
    }

    // Créer une question (admin ONG)
    @PostMapping("/organizations/{orgId}/membership-questions")
    public ResponseEntity<ApiResponse<QuestionResponse>> create(
            @CurrentUser UUID userId, @PathVariable UUID orgId,
            @Valid @RequestBody CreateQuestionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(service.createQuestion(userId, orgId, request)));
    }

    // Supprimer une question (admin ONG)
    @DeleteMapping("/membership-questions/{id}")
    public ResponseEntity<Void> delete(@CurrentUser UUID userId, @PathVariable UUID id) {
        service.deleteQuestion(userId, id);
        return ResponseEntity.noContent().build();
    }

    // Consulter les réponses d'un candidat (admin ONG)
    @GetMapping("/memberships/{id}/answers")
    public ResponseEntity<ApiResponse<List<AnswerResponse>>> answers(
            @CurrentUser UUID userId, @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.getAnswers(userId, id)));
    }
}
