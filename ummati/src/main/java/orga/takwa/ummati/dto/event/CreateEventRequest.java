package orga.takwa.ummati.dto.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record CreateEventRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 5000) String description,
        @Size(max = 5000) String objectives,
        @NotBlank String type,
        String locationName,
        String locationAddress,
        @NotBlank @Size(max = 100) String locationCity,
        @Size(max = 10) String locationZip,
        BigDecimal locationLat,
        BigDecimal locationLng,
        boolean online,
        String onlineLink,
        @NotNull LocalDateTime startDate,
        @NotNull LocalDateTime endDate,
        LocalDateTime registrationDeadline,
        Integer maxParticipants,
        Integer minAge,
        Set<UUID> requiredSkillIds
) {}

