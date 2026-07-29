package orga.takwa.ummati.dto.alert;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MissionAlertResponse(
        UUID id,
        String label,
        String city,
        BigDecimal lat,
        BigDecimal lng,
        Integer radiusKm,
        List<String> domains,
        List<String> types,
        String frequency,
        boolean enabled,
        LocalDateTime lastSentAt,
        LocalDateTime createdAt
) {}
