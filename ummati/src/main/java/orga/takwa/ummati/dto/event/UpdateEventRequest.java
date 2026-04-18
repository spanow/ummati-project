package orga.takwa.ummati.dto.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record UpdateEventRequest(
        String title,
        String description,
        String objectives,
        String type,
        String locationName,
        String locationAddress,
        String locationCity,
        String locationZip,
        BigDecimal locationLat,
        BigDecimal locationLng,
        Boolean online,
        String onlineLink,
        LocalDateTime startDate,
        LocalDateTime endDate,
        LocalDateTime registrationDeadline,
        Integer maxParticipants,
        Integer minAge,
        Set<UUID> requiredSkillIds
) {}

