package orga.takwa.ummati.entity.enums;

/**
 * Statut d'un créneau/occurrence (unité réservable d'un événement).
 * Distinct de {@link EventStatus} (statut de la série) : on peut annuler une seule
 * date d'une série récurrente sans annuler toute la série.
 */
public enum EventOccurrenceStatus {
    DRAFT,
    PUBLISHED,
    CANCELLED,
    COMPLETED
}
