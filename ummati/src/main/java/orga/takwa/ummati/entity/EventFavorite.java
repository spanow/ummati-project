package orga.takwa.ummati.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mission mise de côté par un bénévole.
 *
 * <p>Geste peu engageant — bien moins qu'une inscription — mais c'est le signal
 * d'intention le plus fort dont on dispose avant l'inscription elle-même : il sert
 * à relancer le bénévole avant la clôture (cf. FavoriteClosingJob).
 */
@Entity
@Table(name = "event_favorites",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "event_id"}))
public class EventFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
