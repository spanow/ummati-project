package orga.takwa.ummati.dto.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventSummary(
        UUID id,
        String title,
        String type,
        String locationCity,
        boolean online,
        java.math.BigDecimal locationLat,
        java.math.BigDecimal locationLng,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Integer maxParticipants,
        long registeredCount,
        String status,
        String organizationName,
        String organizationSlug,
        String organizationLogoUrl,
        /** Visuel d'annonce de la mission (null si l'ONG n'en a pas défini). */
        String coverUrl,
        // Prochain créneau à venir (null si aucun) + nombre total de créneaux de la série.
        LocalDateTime nextOccurrenceDate,
        int occurrenceCount,
        /** Distance en km depuis le point de référence — null hors recherche géolocalisée. */
        Double distanceKm
) {}
