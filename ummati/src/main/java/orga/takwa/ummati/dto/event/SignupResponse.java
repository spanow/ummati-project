package orga.takwa.ummati.dto.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record SignupResponse(
        UUID id,
        UUID eventId,
        UUID userId,
        String userFirstName,
        String userLastName,
        String userEmail,
        String status,
        LocalDateTime registeredAt,
        LocalDateTime attendedAt
) {}

