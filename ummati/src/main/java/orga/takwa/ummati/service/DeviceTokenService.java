package orga.takwa.ummati.service;

import orga.takwa.ummati.dto.push.RegisterDeviceRequest;
import orga.takwa.ummati.entity.DeviceToken;
import orga.takwa.ummati.entity.User;
import orga.takwa.ummati.repository.DeviceTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Registre des installations natives joignables par notification.
 *
 * <p>Pendant natif de l'abonnement Web Push : l'app envoie ici le jeton que lui a
 * remis FCM, et {@link PushNotificationService} s'en sert comme adresse de
 * livraison.
 */
@Service
public class DeviceTokenService {

    private static final Logger log = LoggerFactory.getLogger(DeviceTokenService.class);

    /** Au-delà, l'app a été désinstallée ou n'est plus utilisée : le jeton est mort. */
    private static final int STALE_AFTER_DAYS = 180;

    private final DeviceTokenRepository repository;

    public DeviceTokenService(DeviceTokenRepository repository) {
        this.repository = repository;
    }

    /**
     * Enregistre ou met à jour le jeton d'une installation.
     *
     * <p>Le jeton est cherché seul, sans filtrer sur l'utilisateur : FCM le rattache à
     * l'installation, pas au compte. Quand un second bénévole se connecte sur le même
     * téléphone, la ligne doit donc changer de propriétaire. Filtrer sur
     * (utilisateur, jeton) créerait une seconde ligne et les notifications du premier
     * compte continueraient d'arriver sur l'appareil du second.
     */
    @Transactional
    public void register(User user, RegisterDeviceRequest request) {
        DeviceToken device = repository.findByToken(request.token())
                .orElseGet(DeviceToken::new);

        if (device.getId() != null && !device.getUser().getId().equals(user.getId())) {
            log.info("Jeton d'appareil réattribué de l'utilisateur {} à {}",
                    device.getUser().getId(), user.getId());
        }

        device.setUser(user);
        device.setToken(request.token());
        device.setPlatform(request.platform());
        device.setDeviceId(request.deviceId());
        device.setDeviceName(request.deviceName());
        device.setAppVersion(request.appVersion());
        device.setLastSeenAt(LocalDateTime.now());
        repository.save(device);
    }

    /** Désenregistrement explicite — déconnexion ou refus des notifications. */
    @Transactional
    public void unregister(UUID userId, String token) {
        repository.deleteByUserIdAndToken(userId, token);
    }

    @Transactional(readOnly = true)
    public List<DeviceToken> findForUser(UUID userId) {
        return repository.findByUserId(userId);
    }

    /** Retire un jeton que FCM a déclaré invalide (app désinstallée). */
    @Transactional
    public void removeInvalid(DeviceToken device) {
        repository.delete(device);
    }

    /**
     * Purge les installations silencieuses depuis six mois. Sans ce ménage, on
     * continuerait indéfiniment à pousser vers des appareils désinstallés dont FCM ne
     * signale pas toujours la disparition.
     */
    @Transactional
    public long purgeStale() {
        return repository.deleteByLastSeenAtBefore(LocalDateTime.now().minusDays(STALE_AFTER_DAYS));
    }
}
