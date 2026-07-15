package orga.takwa.ummati.dto.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record AnnouncementResponse(
        UUID id,
        UUID eventId,
        UUID authorId,
        String authorFirstName,
        String authorLastName,
        String content,
        boolean pinned,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
