package orga.takwa.ummati.dto.membership;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateQuestionRequest(
        @NotBlank @Size(max = 500) String label,
        @NotBlank String type,          // TEXT | BOOLEAN | SINGLE_CHOICE
        List<String> options,           // requis (>= 2) pour SINGLE_CHOICE
        boolean required
) {}
