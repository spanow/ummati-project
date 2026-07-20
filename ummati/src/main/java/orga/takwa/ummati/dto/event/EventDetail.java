package orga.takwa.ummati.dto.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record EventDetail(
        UUID id,
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
        boolean online,
        String onlineLink,
        LocalDateTime startDate,
        LocalDateTime endDate,
        LocalDateTime registrationDeadline,
        Integer maxParticipants,
        Integer minAge,
        String status,
        String cancellationReason,
        UUID organizationId,
        String organizationName,
        String organizationSlug,
        long registeredCount,
        long waitlistedCount,
        Integer availableSpots,
        List<SkillDto> requiredSkills,
        Double feedbackAvgRating,
        LocalDateTime createdAt,
        String currentUserSignupStatus,
        // Créneaux réservables de la série (1 pour un événement one-shot).
        List<OccurrenceResponse> occurrences
) {
    public record SkillDto(UUID id, String name, String category) {}
}
