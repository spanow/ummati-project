package orga.takwa.ummati.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Abonnement d'un bénévole à une association.
 *
 * <p>Distinct de l'adhésion ({@link Membership}) : suivre n'engage à rien et ne
 * demande aucune validation. C'est le moyen de dire « prévenez-moi quand cette
 * association publie une mission » sans devoir en devenir membre.
 */
@Entity
@Table(name = "organization_follows",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "organization_id"}))
public class OrganizationFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

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

    public Organization getOrganization() { return organization; }
    public void setOrganization(Organization organization) { this.organization = organization; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
