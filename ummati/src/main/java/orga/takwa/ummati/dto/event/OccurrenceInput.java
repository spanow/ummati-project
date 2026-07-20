package orga.takwa.ummati.dto.event;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** Un créneau explicite lors de la création d'un événement multi-créneaux. */
public record OccurrenceInput(
        @Size(max = 255) String label,
        @NotNull LocalDateTime startDate,
        @NotNull LocalDateTime endDate,
        LocalDateTime registrationDeadline,
        Integer maxParticipants
) {}
