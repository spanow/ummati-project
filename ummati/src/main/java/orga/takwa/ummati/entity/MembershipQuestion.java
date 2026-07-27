package orga.takwa.ummati.entity;

import jakarta.persistence.*;
import orga.takwa.ummati.entity.enums.QuestionType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Question du questionnaire d'adhésion, définie par une ONG. Les candidats y répondent
 * lors de leur demande d'adhésion ({@link MembershipAnswer}).
 */
@Entity
@Table(name = "membership_questions")
public class MembershipQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, length = 500)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionType type;

    /** Choix possibles (une par ligne) — uniquement pour SINGLE_CHOICE. */
    @Column(columnDefinition = "TEXT")
    private String options;

    @Column(nullable = false)
    private boolean required = false;

    @Column(nullable = false)
    private int position = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // --- Getters & Setters ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Organization getOrganization() { return organization; }
    public void setOrganization(Organization organization) { this.organization = organization; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public QuestionType getType() { return type; }
    public void setType(QuestionType type) { this.type = type; }

    public String getOptions() { return options; }
    public void setOptions(String options) { this.options = options; }

    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
