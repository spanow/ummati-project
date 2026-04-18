package orga.takwa.ummati.dto.document;

import java.time.LocalDateTime;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String name,
        String originalName,
        String contentType,
        long fileSize,
        String ownerType,
        UUID ownerId,
        String uploadedByFirstName,
        String uploadedByLastName,
        LocalDateTime createdAt
) {}

