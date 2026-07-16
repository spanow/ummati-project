package orga.takwa.ummati.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import orga.takwa.ummati.dto.push.SubscribeRequest;
import orga.takwa.ummati.entity.PushSubscription;
import orga.takwa.ummati.entity.User;
import orga.takwa.ummati.repository.PushSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final PushService pushService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.vapid.public-key}")
    private String vapidPublicKey;

    public PushNotificationService(PushSubscriptionRepository subscriptionRepository,
                                    PushService pushService) {
        this.subscriptionRepository = subscriptionRepository;
        this.pushService = pushService;
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

    // Best-effort: push failures never interrupt the caller (in-app notif + email already succeeded)
    public void sendToUser(User user, String title, String body, String link) {
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
