package orga.takwa.ummati.dto.organization;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrgAnnouncementResponse(
        UUID id,
        UUID orgId,
        UUID authorId,
        String authorFirstName,
        String authorLastName,
        String title,
        String content,
        boolean pinned,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
