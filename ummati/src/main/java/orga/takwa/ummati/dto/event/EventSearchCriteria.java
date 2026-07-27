package orga.takwa.ummati.dto.event;

import orga.takwa.ummati.entity.enums.EventType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Critères de recherche des missions publiées.
 *
 * @param q          recherche plein texte (titre, description, objectifs, ville, nom de l'ONG)
 * @param lat        latitude du point de référence (« autour de moi »)
 * @param lng        longitude du point de référence
 * @param radiusKm   rayon en kilomètres autour de ce point
 * @param sortByDistance trier du plus proche au plus lointain (exige un point de référence)
 */
public record EventSearchCriteria(
        EventType type,
        String city,
        UUID orgId,
        Boolean online,
        LocalDateTime startAfter,
        LocalDateTime startBefore,
        UUID skillId,
        String q,
        Double lat,
        Double lng,
        Double radiusKm,
        boolean sortByDistance
) {
    /** Vrai si un point de référence exploitable est fourni. */
    public boolean hasOrigin() {
        return lat != null && lng != null;
    }

    /** Vrai si un filtre par rayon doit être appliqué. */
    public boolean hasRadius() {
        return hasOrigin() && radiusKm != null && radiusKm > 0;
    }
}
