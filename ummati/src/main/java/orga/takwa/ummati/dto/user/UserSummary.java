package orga.takwa.ummati.dto.user;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserSummary(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String role,
        boolean emailVerified,
        boolean enabled,
        LocalDateTime createdAt
) {}

