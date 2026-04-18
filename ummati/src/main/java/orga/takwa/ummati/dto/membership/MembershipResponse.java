package orga.takwa.ummati.dto.membership;

import java.time.LocalDateTime;
import java.util.UUID;

public record MembershipResponse(
        UUID id, UUID userId, UUID organizationId,
        String firstName, String lastName, String photoUrl,
        String role, String status, String motivation,
        LocalDateTime joinedAt, LocalDateTime createdAt
) {}

