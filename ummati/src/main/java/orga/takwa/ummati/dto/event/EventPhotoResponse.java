package orga.takwa.ummati.dto.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventPhotoResponse(
        UUID id,
        String url,
        String caption,
        int position,
        LocalDateTime createdAt
) {}
