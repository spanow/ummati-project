package orga.takwa.ummati.dto.event;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CreateFeedbackRequest(
        @Min(1) @Max(5) int rating,
        String comment,
        boolean anonymous
) {}

