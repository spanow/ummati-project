package orga.takwa.ummati.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import orga.takwa.ummati.dto.push.SubscribeRequest;
import orga.takwa.ummati.entity.DeviceToken;
import orga.takwa.ummati.entity.PushSubscription;
import orga.takwa.ummati.entity.User;
import orga.takwa.ummati.repository.DeviceTokenRepository;
import orga.takwa.ummati.repository.PushSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    private final PushSubscriptionRepository subscriptionRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final PushService pushService;
    private final FirebaseMessaging firebaseMessaging;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.vapid.public-key:}")
    private String vapidPublicKey;

    public PushNotificationService(PushSubscriptionRepository subscriptionRepository,
                                    DeviceTokenRepository deviceTokenRepository,
                                    ObjectProvider<PushService> pushServiceProvider,
                                    ObjectProvider<FirebaseMessaging> firebaseMessagingProvider) {
        this.subscriptionRepository = subscriptionRepository;
        this.deviceTokenRepository = deviceTokenRepository;
        // ObjectProvider et non injection directe : sans clés VAPID, PushNotificationConfig
        // ne fournit pas de client et l'injection obligatoire ferait échouer tout le
        // démarrage pour une fonctionnalité facultative.
        this.pushService = pushServiceProvider.getIfAvailable();
        // Même raisonnement pour FCM : les deux canaux sont indépendamment facultatifs.
        this.firebaseMessaging = firebaseMessagingProvider.getIfAvailable();
    }

    public String getVapidPublicKey() {
        return vapidPublicKey;
    }

    @Transactional
    public void subscribe(User user, SubscribeRequest request) {
        PushSubscription sub = subscriptionRepository.findByEndpoint(request.endpoint())
                .orElseGet(PushSubscription::new);
        sub.setUser(user);
        sub.setEndpoint(request.endpoint());
        sub.setP256dh(request.keys().p256dh());
        sub.setAuth(request.keys().auth());
        subscriptionRepository.save(sub);
    }

    @Transactional
    public void unsubscribe(UUID userId, String endpoint) {
        subscriptionRepository.deleteByUserIdAndEndpoint(userId, endpoint);
    }

    /**
     * Pousse vers tous les canaux du bénévole : navigateurs abonnés au Web Push et
     * appareils enregistrés via FCM.
     *
     * <p>Un même utilisateur peut être joignable par les deux (ordinateur au bureau,
     * téléphone dans la poche) ; les canaux sont donc cumulatifs et non exclusifs.
     * L'ensemble reste au mieux : un échec de push n'interrompt jamais l'appelant, la
     * notification in-app et l'email ayant déjà abouti.
     */
    public void sendToUser(User user, String title, String body, String link) {
        sendWebPush(user, title, body, link);
        sendNativePush(user, title, body, link);
    }

    private void sendWebPush(User user, String title, String body, String link) {
        // pushService est null quand les clés VAPID ne sont pas configurées : le push est
        // alors simplement désactivé, sans incidence sur les notifications in-app.
        if (pushService == null) return;

        List<PushSubscription> subs = subscriptionRepository.findByUserId(user.getId());
        if (subs.isEmpty()) return;

        String payload = buildPayload(title, body, link);
        for (PushSubscription sub : subs) {
            try {
                Subscription subscription = new Subscription(sub.getEndpoint(),
                        new Subscription.Keys(sub.getP256dh(), sub.getAuth()));
                Notification notification = Notification.builder()
                        .subscription(subscription)
                        .payload(payload)
                        .build();
                HttpResponse<Void> response = pushService.send(notification);
                if (response.statusCode() == 404 || response.statusCode() == 410) {
                    subscriptionRepository.delete(sub);
                } else if (response.statusCode() >= 300) {
                    log.warn("Push notification failed with status {} for subscription {}",
                            response.statusCode(), sub.getId());
                }
            } catch (Exception e) {
                log.warn("Push notification failed for subscription {}: {}", sub.getId(), e.getMessage());
            }
        }
    }

    private void sendNativePush(User user, String title, String body, String link) {
        if (firebaseMessaging == null) return;

        List<DeviceToken> devices = deviceTokenRepository.findByUserId(user.getId());
        if (devices.isEmpty()) return;

        String url = link != null ? link : "/";

        for (DeviceToken device : devices) {
            try {
                Message message = Message.builder()
                        .setToken(device.getToken())
                        // Pleinement qualifié : « Notification » désigne déjà le type
                        // Web Push importé plus haut.
                        .setNotification(com.google.firebase.messaging.Notification.builder()
                                .setTitle(title)
                                .setBody(body != null ? body : "")
                                .build())
                        // La donnée est dupliquée hors du bloc notification : c'est le seul
                        // endroit que l'app peut lire au moment du tap pour router vers le
                        // bon écran.
                        .putData("url", url)
                        .setAndroidConfig(AndroidConfig.builder()
                                .setPriority(AndroidConfig.Priority.HIGH)
                                .build())
                        .setApnsConfig(ApnsConfig.builder()
                                .setAps(Aps.builder().setSound("default").build())
                                .build())
                        .build();
                firebaseMessaging.send(message);
            } catch (FirebaseMessagingException e) {
                // UNREGISTERED / INVALID_ARGUMENT : l'app a été désinstallée ou le jeton a
                // été remplacé. On supprime la ligne, sinon on pousserait indéfiniment vers
                // un appareil qui n'existe plus.
                MessagingErrorCode code = e.getMessagingErrorCode();
                if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
                    deviceTokenRepository.delete(device);
                    log.debug("Jeton d'appareil {} retiré ({})", device.getId(), code);
                } else {
                    log.warn("Push natif échoué pour l'appareil {} : {}", device.getId(), e.getMessage());
                }
            } catch (Exception e) {
                log.warn("Push natif échoué pour l'appareil {} : {}", device.getId(), e.getMessage());
            }
        }
    }

    private String buildPayload(String title, String body, String link) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "title", title,
                    "body", body != null ? body : "",
                    "url", link != null ? link : "/"));
        } catch (Exception e) {
            return "{}";
        }
    }
}
