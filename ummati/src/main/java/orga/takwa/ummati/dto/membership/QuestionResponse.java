package orga.takwa.ummati.dto.membership;

import java.util.List;
import java.util.UUID;

public record QuestionResponse(
        UUID id,
        String label,
        String type,
        List<String> options,
        boolean required,
        int position
) {}
