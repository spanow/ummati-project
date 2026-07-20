package orga.takwa.ummati.entity;

import jakarta.persistence.*;
import orga.takwa.ummati.entity.enums.EventOccurrenceStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Un créneau daté d'un événement (la série). Un événement one-shot possède exactement
 * une occurrence miroir ; un événement multi-créneaux ou récurrent en possède plusieurs.
 * L'inscription, la présence, les heures et le no-show vivent au niveau occurrence.
 */
@Entity
@Table(name = "event_occurrences")
public class EventOccurrence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    /** Libellé optionnel du créneau (ex. « Accueil », « Maraude sam. »). */
    @Column(length = 255)
    private String label;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "registration_deadline")
    private LocalDateTime registrationDeadline;

    /** Capacité propre au créneau (null = illimité). */
    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventOccurrenceStatus status = EventOccurrenceStatus.DRAFT;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /** Deadline effective : la deadline propre, sinon le début du créneau. */
    public LocalDateTime effectiveDeadline() {
        return registrationDeadline != null ? registrationDeadline : startDate;
    }

    // --- Getters & Setters ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

    public LocalDateTime getRegistrationDeadline() { return registrationDeadline; }
    public void setRegistrationDeadline(LocalDateTime registrationDeadline) { this.registrationDeadline = registrationDeadline; }

    public Integer getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(Integer maxParticipants) { this.maxParticipants = maxParticipants; }

    public EventOccurrenceStatus getStatus() { return status; }
    public void setStatus(EventOccurrenceStatus status) { this.status = status; }

    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
