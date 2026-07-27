package orga.takwa.ummati.dto.membership;

import java.util.UUID;

/** Réponse d'un candidat, telle qu'affichée à l'admin ONG lors de la revue de la demande. */
public record AnswerResponse(
        UUID questionId,
        String questionLabel,
        String type,
        String value
) {}
