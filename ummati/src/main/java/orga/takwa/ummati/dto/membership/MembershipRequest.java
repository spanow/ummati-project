package orga.takwa.ummati.dto.membership;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

public record MembershipRequest(
        @Size(max = 1000) String motivation,
        // Réponses au questionnaire d'adhésion configuré par l'ONG (optionnel).
        @Valid List<AnswerInput> answers
) {}
