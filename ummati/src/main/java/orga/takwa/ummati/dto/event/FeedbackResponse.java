package orga.takwa.ummati.dto.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record FeedbackResponse(
        UUID id,
        UUID eventId,
        int rating,
        String comment,
        boolean anonymous,
        String userFirstName,
        String userLastName,
        LocalDateTime createdAt
) {}

