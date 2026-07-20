package orga.takwa.ummati.dto.membership;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Réponse d'un candidat à une question, soumise avec la demande d'adhésion. */
public record AnswerInput(
        @NotNull UUID questionId,
        String value
) {}
