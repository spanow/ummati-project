package orga.takwa.ummati.entity;

import jakarta.persistence.*;
import orga.takwa.ummati.entity.enums.DevicePlatform;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Session persistante d'un appareil.
 *
 * <p>Complète — sans le remplacer — le refresh JWT sans état des clients web
 * historiques. Ce modèle stocké apporte les trois propriétés qu'un JWT ne peut pas
 * offrir et dont le mobile a besoin : une durée longue sans allonger la fenêtre de
 * vol, la rotation, et la révocation immédiate d'un téléphone perdu.
 *
 * <p>Seule l'empreinte SHA-256 du jeton est conservée : la valeur en clair n'existe
 * que dans la réponse HTTP et sur l'appareil.
 */
@Entity
@Table(name = "refresh_tokens",
       uniqueConstraints = @UniqueConstraint(columnNames = "token_hash"))
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    /**
     * Relie toutes les rotations successives d'une même connexion. Rejouer un jeton
     * déjà consommé révoque la famille entière : le client légitime détient le
     * dernier maillon, donc un maillon antérieur qui resurgit a été volé.
     */
    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DevicePlatform platform = DevicePlatform.WEB;

    @Column(name = "device_id", length = 100)
    private String deviceId;

    @Column(name = "device_name", length = 120)
    private String deviceName;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    /** Horodate la rotation : un jeton consommé ne doit plus jamais être accepté. */
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /** Utilisable : ni révoqué, ni déjà consommé par une rotation, ni expiré. */
    public boolean isUsable() {
        return revokedAt == null
                && usedAt == null
                && expiresAt.isAfter(LocalDateTime.now());
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public UUID getFamilyId() { return familyId; }
    public void setFamilyId(UUID familyId) { this.familyId = familyId; }

    public DevicePlatform getPlatform() { return platform; }
    public void setPlatform(DevicePlatform platform) { this.platform = platform; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }

    public LocalDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(LocalDateTime usedAt) { this.usedAt = usedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
