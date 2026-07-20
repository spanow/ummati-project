package orga.takwa.ummati.entity;

import jakarta.persistence.*;
import orga.takwa.ummati.entity.enums.SignupStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "event_signups", uniqueConstraints = @UniqueConstraint(columnNames = {"occurrence_id", "user_id"}))
public class EventSignup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Série de rattachement (dénormalisé : permet de lister toutes les inscriptions d'un événement).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    // Créneau réellement réservé — porte l'unicité (occurrence_id, user_id), la présence et les heures.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "occurrence_id", nullable = false)
    private EventOccurrence occurrence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SignupStatus status = SignupStatus.REGISTERED;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "attended_at")
    private LocalDateTime attendedAt;

    // Heures de bénévolat certifiées par l'ONG (défaut = durée du créneau, ajustable), puis figées.
    @Column(name = "hours_validated", precision = 5, scale = 2)
    private BigDecimal hoursValidated;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (registeredAt == null) registeredAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }

    public EventOccurrence getOccurrence() { return occurrence; }
    public void setOccurrence(EventOccurrence occurrence) { this.occurrence = occurrence; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public SignupStatus getStatus() { return status; }
    public void setStatus(SignupStatus status) { this.status = status; }

    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }

    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }

    public LocalDateTime getAttendedAt() { return attendedAt; }
    public void setAttendedAt(LocalDateTime attendedAt) { this.attendedAt = attendedAt; }

    public BigDecimal getHoursValidated() { return hoursValidated; }
    public void setHoursValidated(BigDecimal hoursValidated) { this.hoursValidated = hoursValidated; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}

