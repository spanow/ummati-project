package orga.takwa.ummati.dto.event;

import java.util.UUID;

/**
 * Fiabilité d'un bénévole, calculée sur son historique de présences.
 * Visible uniquement par les admins d'ONG (donnée de profilage — jamais publique).
 */
public record ReliabilityResponse(
        UUID userId,
        long attendedCount,
        long noShowCount,
        long lateCancelCount,
        Double reliabilityRate   // 0..1 ; null si aucun historique exploitable
) {}
