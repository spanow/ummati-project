package orga.takwa.ummati.dto.notification;

/**
 * Ce que le bénévole accepte de recevoir par email.
 * Les emails transactionnels n'y figurent pas : ils ne sont pas désactivables.
 */
public record NotificationPreferencesResponse(
        boolean emailNewMissions,
        boolean emailReminders,
        boolean emailMemberships,
        boolean emailAnnouncements
) {}
