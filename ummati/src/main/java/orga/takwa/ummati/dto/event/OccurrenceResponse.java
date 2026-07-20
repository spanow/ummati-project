package orga.takwa.ummati.dto.event;

import java.time.LocalDateTime;
import java.util.UUID;

/** Un créneau réservable, avec ses compteurs de places et le statut d'inscription de l'utilisateur courant. */
public record OccurrenceResponse(
        UUID id,
        String label,
        LocalDateTime startDate,
        LocalDateTime endDate,
        LocalDateTime registrationDeadline,
        Integer maxParticipants,
        long registeredCount,
        long waitlistedCount,
        Integer availableSpots,
        String status,
        String currentUserSignupStatus
) {}
