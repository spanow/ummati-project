package orga.takwa.ummati.entity;

import jakarta.persistence.*;
import orga.takwa.ummati.entity.enums.AlertFrequency;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Recherche sauvegardée, rejouée périodiquement pour signaler les nouvelles missions.
 *
 * <p>C'est le seul mécanisme de rétention qui va chercher le bénévole : les favoris
 * et le suivi supposent qu'il revienne de lui-même. Une alerte lui écrit.
 */
@Entity
@Table(name = "mission_alerts")
public class MissionAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Nom donné par le bénévole, ex. « Environnement autour de Lyon ». */
    @Column(nullable = false, length = 120)
    private String label;

    @Column(length = 100)
    private String city;

    @Column(precision = 10, scale = 8)
    private BigDecimal lat;

    @Column(precision = 11, scale = 8)
    private BigDecimal lng;

    @Column(name = "radius_km")
    private Integer radiusKm;

    /** Domaines d'ONG retenus, en CSV. Vide = tous. */
    @Column(columnDefinition = "TEXT")
    private String domains;

    /** Types de mission retenus, en CSV. Vide = tous. */
    @Column(columnDefinition = "TEXT")
    private String types;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AlertFrequency frequency = AlertFrequency.WEEKLY;

    @Column(nullable = false)
    private boolean enabled = true;

    /**
     * Borne basse de la prochaine recherche. Garantit qu'une mission déjà signalée
     * ne l'est pas une seconde fois, même si la tâche est rejouée.
     */
    @Column(name = "last_sent_at")
    private LocalDateTime lastSentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /** Vrai si un périmètre géographique exploitable est défini. */
    public boolean hasRadius() {
        return lat != null && lng != null && radiusKm != null && radiusKm > 0;
    }

    public Set<String> domainSet() { return splitCsv(domains); }

    public Set<String> typeSet() { return splitCsv(types); }

    private static Set<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) return Set.of();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public BigDecimal getLat() { return lat; }
    public void setLat(BigDecimal lat) { this.lat = lat; }

    public BigDecimal getLng() { return lng; }
    public void setLng(BigDecimal lng) { this.lng = lng; }

    public Integer getRadiusKm() { return radiusKm; }
    public void setRadiusKm(Integer radiusKm) { this.radiusKm = radiusKm; }

    public String getDomains() { return domains; }
    public void setDomains(String domains) { this.domains = domains; }

    public String getTypes() { return types; }
    public void setTypes(String types) { this.types = types; }

    public AlertFrequency getFrequency() { return frequency; }
    public void setFrequency(AlertFrequency frequency) { this.frequency = frequency; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public LocalDateTime getLastSentAt() { return lastSentAt; }
    public void setLastSentAt(LocalDateTime lastSentAt) { this.lastSentAt = lastSentAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
