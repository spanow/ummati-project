package orga.takwa.ummati.entity.enums;

public enum NotificationType {
    WELCOME,
    EMAIL_VERIFIED,
    ONG_SUBMITTED,
    ONG_VALIDATED,
    ONG_REJECTED,
    MEMBERSHIP_REQUESTED,
    MEMBERSHIP_ACCEPTED,
    MEMBERSHIP_REJECTED,
    EVENT_PUBLISHED,
    EVENT_REMINDER_7D,
    EVENT_REMINDER_1D,
    EVENT_CANCELLED,
    EVENT_COMPLETED,
    SIGNUP_CONFIRMED,
    SIGNUP_WAITLISTED,
    SIGNUP_PROMOTED,
    FEEDBACK_REQUESTED,
    EVENT_ANNOUNCEMENT,
    ORG_ANNOUNCEMENT,
    /** Relance de l'ONG pour alimenter la galerie d'une mission terminée. */
    EVENT_PHOTOS_REQUESTED,
    /** Nouvelles missions correspondant à une alerte enregistrée. */
    MISSION_ALERT,
    /** Nouvelle mission publiée par une association suivie. */
    ORG_NEW_EVENT,
    /** Les inscriptions d'une mission mise en favori vont fermer. */
    FAVORITE_CLOSING,
    GENERAL
}

