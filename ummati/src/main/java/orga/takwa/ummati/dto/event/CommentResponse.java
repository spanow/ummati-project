package orga.takwa.ummati.dto.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        UUID eventId,
        UUID authorId,
        String authorFirstName,
        String authorLastName,
        String authorPhotoUrl,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
