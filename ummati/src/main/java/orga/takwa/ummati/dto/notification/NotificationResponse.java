package orga.takwa.ummati.dto.notification;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String type,
        String title,
        String message,
        boolean read,
        String link,
        LocalDateTime createdAt
) {}

