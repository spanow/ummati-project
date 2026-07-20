package orga.takwa.ummati.dto.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record SignupResponse(
        UUID id,
        UUID eventId,
        String eventTitle,
        UUID occurrenceId,
        LocalDateTime occurrenceStartDate,
        LocalDateTime occurrenceEndDate,
        UUID userId,
        String userFirstName,
        String userLastName,
        String userEmail,
        String status,
        LocalDateTime registeredAt,
        LocalDateTime attendedAt,
        BigDecimal hoursValidated
) {}
