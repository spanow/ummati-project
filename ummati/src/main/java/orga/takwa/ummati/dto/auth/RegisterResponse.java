package orga.takwa.ummati.dto.auth;

import java.time.LocalDateTime;
import java.util.UUID;

public record RegisterResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String role,
        boolean emailVerified,
        LocalDateTime createdAt
) {}

