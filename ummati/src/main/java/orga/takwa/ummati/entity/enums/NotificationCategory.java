package orga.takwa.ummati.entity.enums;

/**
 * Catégories d'emails soumises aux préférences du bénévole.
 *
 * <p>{@link #TRANSACTIONAL} est volontairement présente pour que tout envoi soit
 * classé : elle n'est jamais filtrée, mais son existence oblige à choisir une
 * catégorie plutôt qu'à oublier le réglage.
 */
public enum NotificationCategory {
    /** Alertes, nouvelles missions d'une ONG suivie, relance sur un favori. */
    NEW_MISSIONS,
    /** Rappels avant une mission à laquelle on est inscrit. */
    REMINDERS,
    /** Suites d'une demande d'adhésion. */
    MEMBERSHIPS,
    /** Annonces d'une ONG ou d'une mission. */
    ANNOUNCEMENTS,
    /** Réponse à une action explicite : jamais désactivable. */
    TRANSACTIONAL;

    /**
     * Catégorie d'un type de notification, pour décider si l'email correspondant
     * peut être filtré par les préférences.
     *
     * <p>La règle de tri : tout ce qui répond à une action de l'utilisateur ou
     * l'informe d'un changement qui l'affecte directement (mission annulée alors
     * qu'il y est inscrit) reste transactionnel. Tout ce qui relève de la
     * sollicitation est désactivable.
     */
    public static NotificationCategory of(NotificationType type) {
        return switch (type) {
            case MISSION_ALERT, ORG_NEW_EVENT, FAVORITE_CLOSING, EVENT_PUBLISHED -> NEW_MISSIONS;
            case EVENT_REMINDER_7D, EVENT_REMINDER_1D, EVENT_COMPLETED,
                 FEEDBACK_REQUESTED, EVENT_PHOTOS_REQUESTED -> REMINDERS;
            case MEMBERSHIP_REQUESTED, MEMBERSHIP_ACCEPTED, MEMBERSHIP_REJECTED -> MEMBERSHIPS;
            case EVENT_ANNOUNCEMENT, ORG_ANNOUNCEMENT -> ANNOUNCEMENTS;
            // Inscriptions, annulations, validation d'ONG, sécurité du compte :
            // le bénévole doit être prévenu quoi qu'il arrive.
            default -> TRANSACTIONAL;
        };
    }
}
