package orga.takwa.ummati.entity;

import jakarta.persistence.*;
import orga.takwa.ummati.entity.enums.DevicePlatform;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Jeton de notification d'une installation native (FCM côté Android, APNs relayé
 * par FCM côté iOS).
 *
 * <p>Équivalent natif de {@link PushSubscription}, qui reste réservé au Web Push :
 * une WKWebView (le conteneur iOS de l'app) ne supporte pas le Web Push, l'app
 * installée passe donc obligatoirement par ce canal.
 *
 * <p>Le jeton est unique au niveau base et non par couple (utilisateur, jeton) :
 * il identifie un téléphone, pas un compte. Si un second bénévole se connecte sur
 * le même appareil, la ligne change de propriétaire — voir
 * {@code DeviceTokenService#register}.
 */
@Entity
@Table(name = "device_tokens",
       uniqueConstraints = @UniqueConstraint(columnNames = "token"))
public class DeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 512)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DevicePlatform platform;

    @Column(name = "device_id", length = 100)
    private String deviceId;

    @Column(name = "device_name", length = 120)
    private String deviceName;

    @Column(name = "app_version", length = 20)
    private String appVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (lastSeenAt == null) lastSeenAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public DevicePlatform getPlatform() { return platform; }
    public void setPlatform(DevicePlatform platform) { this.platform = platform; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(LocalDateTime lastSeenAt) { this.lastSeenAt = lastSeenAt; }
}
