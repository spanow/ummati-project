package orga.takwa.ummati.entity;

import jakarta.persistence.*;
import orga.takwa.ummati.entity.enums.NotificationCategory;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Ce que le bénévole accepte de recevoir par email.
 *
 * <p>Ne couvre que les emails <strong>non transactionnels</strong>. La vérification
 * d'adresse, la réinitialisation de mot de passe ou la confirmation d'inscription à
 * une mission ne sont pas réglables : ils répondent à une action explicite de
 * l'utilisateur et doivent partir dans tous les cas.
 *
 * <p>Les notifications dans l'application, elles, ne sont jamais filtrées : elles ne
 * dérangent personne et restent consultables.
 */
@Entity
@Table(name = "notification_preferences")
public class NotificationPreferences {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /** Alertes missions, nouvelles missions d'une ONG suivie, relance sur un favori. */
    @Column(name = "email_new_missions", nullable = false)
    private boolean emailNewMissions = true;

    /** Rappels avant une mission à laquelle le bénévole est inscrit. */
    @Column(name = "email_reminders", nullable = false)
    private boolean emailReminders = true;

    /** Suites données à une demande d'adhésion. */
    @Column(name = "email_memberships", nullable = false)
    private boolean emailMemberships = true;

    /** Annonces publiées par une ONG ou sur une mission. */
    @Column(name = "email_announcements", nullable = false)
    private boolean emailAnnouncements = true;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /** Vrai si un email de cette catégorie peut être envoyé à ce bénévole. */
    public boolean allows(NotificationCategory category) {
        return switch (category) {
            case NEW_MISSIONS -> emailNewMissions;
            case REMINDERS -> emailReminders;
            case MEMBERSHIPS -> emailMemberships;
            case ANNOUNCEMENTS -> emailAnnouncements;
            // Un email transactionnel n'est jamais filtré.
            case TRANSACTIONAL -> true;
        };
    }

    public void set(NotificationCategory category, boolean value) {
        switch (category) {
            case NEW_MISSIONS -> emailNewMissions = value;
            case REMINDERS -> emailReminders = value;
            case MEMBERSHIPS -> emailMemberships = value;
            case ANNOUNCEMENTS -> emailAnnouncements = value;
            case TRANSACTIONAL -> { /* non réglable, volontairement ignoré */ }
        }
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public boolean isEmailNewMissions() { return emailNewMissions; }
    public void setEmailNewMissions(boolean v) { this.emailNewMissions = v; }

    public boolean isEmailReminders() { return emailReminders; }
    public void setEmailReminders(boolean v) { this.emailReminders = v; }

    public boolean isEmailMemberships() { return emailMemberships; }
    public void setEmailMemberships(boolean v) { this.emailMemberships = v; }

    public boolean isEmailAnnouncements() { return emailAnnouncements; }
    public void setEmailAnnouncements(boolean v) { this.emailAnnouncements = v; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
