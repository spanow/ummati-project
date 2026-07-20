package orga.takwa.ummati.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Réponse d'un candidat à une {@link MembershipQuestion}, liée à sa demande d'adhésion. */
@Entity
@Table(name = "membership_answers",
        uniqueConstraints = @UniqueConstraint(columnNames = {"membership_id", "question_id"}))
public class MembershipAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_id", nullable = false)
    private Membership membership;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private MembershipQuestion question;

    @Column(name = "answer_value", columnDefinition = "TEXT")
    private String value;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // --- Getters & Setters ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Membership getMembership() { return membership; }
    public void setMembership(Membership membership) { this.membership = membership; }

    public MembershipQuestion getQuestion() { return question; }
    public void setQuestion(MembershipQuestion question) { this.question = question; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
