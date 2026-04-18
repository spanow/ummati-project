package orga.takwa.ummati.dto.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventSummary(
        UUID id,
        String title,
        String type,
        String locationCity,
        boolean online,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Integer maxParticipants,
        long registeredCount,
        String status,
        String organizationName,
        String organizationSlug
) {}

