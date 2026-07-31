package orga.takwa.ummati.service;

import com.google.firebase.messaging.FirebaseMessaging;
import nl.martijndwars.webpush.PushService;
import org.springframework.beans.factory.ObjectProvider;
import orga.takwa.ummati.dto.push.SubscribeRequest;
import orga.takwa.ummati.entity.PushSubscription;
import orga.takwa.ummati.entity.User;
import orga.takwa.ummati.repository.DeviceTokenRepository;
import orga.takwa.ummati.repository.PushSubscriptionRepository;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.net.http.HttpResponse;
import java.security.Security;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PushNotificationServiceTest {

    // Valid uncompressed EC (P-256) point, needed because the library parses p256dh as a real public key
    private static final String VALID_P256DH =
            "BPLSJxSB3QAA1XsJVFED-6HGGyJmFios6ETEo9MJ-7RmbPOXioh_0WsOUPorDEpCbP_PaEJ3wGyo0t3f93y2sU4";
    private static final String VALID_AUTH =
            Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[16]);

    @Mock private PushSubscriptionRepository subscriptionRepository;
    @Mock private DeviceTokenRepository deviceTokenRepository;
    @Mock private PushService pushService;
    @Mock private HttpResponse<Void> httpResponse;

    private PushNotificationService pushNotificationService;

    private UUID userId;
    private User user;

    /** Le service reçoit un ObjectProvider : le push est facultatif et peut être absent. */
    private static <T> ObjectProvider<T> providerOf(T service) {
        return new ObjectProvider<>() {
            @Override public T getObject() { return service; }
            @Override public T getObject(Object... args) { return service; }
            @Override public T getIfAvailable() { return service; }
            @Override public T getIfUnique() { return service; }
        };
    }

    @BeforeEach
    void setUp() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        // Canal natif absent : ces cas ne couvrent que le Web Push, et un FCM non
        // configuré doit rester sans effet sur lui.
        pushNotificationService = new PushNotificationService(
                subscriptionRepository, deviceTokenRepository,
                providerOf(pushService), providerOf((FirebaseMessaging) null));
        Field vapidField = PushNotificationService.class.getDeclaredField("vapidPublicKey");
        vapidField.setAccessible(true);
        vapidField.set(pushNotificationService, "test-public-key");

        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setFirstName("Jean");
    }

    @Test
    void subscribe_shouldCreateNewSubscription_whenEndpointUnknown() {
        SubscribeRequest request = new SubscribeRequest("https://push.example.com/abc",
                new SubscribeRequest.Keys("p256dh-value", "auth-value"));
        when(subscriptionRepository.findByEndpoint(request.endpoint())).thenReturn(Optional.empty());

        pushNotificationService.subscribe(user, request);

        verify(subscriptionRepository).save(argThat(sub ->
                sub.getUser() == user && sub.getEndpoint().equals(request.endpoint())
                        && sub.getP256dh().equals("p256dh-value") && sub.getAuth().equals("auth-value")));
    }

    @Test
    void subscribe_shouldReassignExistingSubscription_whenEndpointAlreadyKnown() {
        PushSubscription existing = new PushSubscription();
        existing.setEndpoint("https://push.example.com/abc");
        SubscribeRequest request = new SubscribeRequest("https://push.example.com/abc",
                new SubscribeRequest.Keys("new-p256dh", "new-auth"));
        when(subscriptionRepository.findByEndpoint(request.endpoint())).thenReturn(Optional.of(existing));

        pushNotificationService.subscribe(user, request);

        verify(subscriptionRepository).save(existing);
        assertThat(existing.getUser()).isEqualTo(user);
        assertThat(existing.getP256dh()).isEqualTo("new-p256dh");
    }

    @Test
    void unsubscribe_shouldDeleteByUserAndEndpoint() {
        pushNotificationService.unsubscribe(userId, "https://push.example.com/abc");
        verify(subscriptionRepository).deleteByUserIdAndEndpoint(userId, "https://push.example.com/abc");
    }

    @Test
    void sendToUser_shouldSkip_whenNoSubscriptions() throws Exception {
        when(subscriptionRepository.findByUserId(userId)).thenReturn(List.of());

        pushNotificationService.sendToUser(user, "Titre", "Message", "/link");

        verifyNoInteractions(pushService);
    }

    @Test
    void sendToUser_shouldDeleteSubscription_whenEndpointGone() throws Exception {
        PushSubscription sub = new PushSubscription();
        sub.setId(UUID.randomUUID());
        sub.setEndpoint("https://push.example.com/abc");
        sub.setP256dh(VALID_P256DH);
        sub.setAuth(VALID_AUTH);
        when(subscriptionRepository.findByUserId(userId)).thenReturn(List.of(sub));
        when(httpResponse.statusCode()).thenReturn(410);
        when(pushService.send(any())).thenReturn(httpResponse);

        pushNotificationService.sendToUser(user, "Titre", "Message", "/link");

        verify(subscriptionRepository).delete(sub);
    }

    @Test
    void sendToUser_shouldKeepSubscription_whenSendSucceeds() throws Exception {
        PushSubscription sub = new PushSubscription();
        sub.setId(UUID.randomUUID());
        sub.setEndpoint("https://push.example.com/abc");
        sub.setP256dh(VALID_P256DH);
        sub.setAuth(VALID_AUTH);
        when(subscriptionRepository.findByUserId(userId)).thenReturn(List.of(sub));
        when(httpResponse.statusCode()).thenReturn(201);
        when(pushService.send(any())).thenReturn(httpResponse);

        pushNotificationService.sendToUser(user, "Titre", "Message", "/link");

        verify(subscriptionRepository, never()).delete(any());
    }

    @Test
    void getVapidPublicKey_shouldReturnConfiguredKey() {
        assertThat(pushNotificationService.getVapidPublicKey()).isEqualTo("test-public-key");
    }
}
