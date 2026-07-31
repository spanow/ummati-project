package orga.takwa.ummati.controller.push;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import orga.takwa.ummati.config.security.CurrentUser;
import orga.takwa.ummati.dto.ApiResponse;
import orga.takwa.ummati.dto.push.RegisterDeviceRequest;
import orga.takwa.ummati.dto.push.SubscribeRequest;
import orga.takwa.ummati.dto.push.VapidPublicKeyResponse;
import orga.takwa.ummati.exception.ResourceNotFoundException;
import orga.takwa.ummati.repository.UserRepository;
import orga.takwa.ummati.service.DeviceTokenService;
import orga.takwa.ummati.service.PushNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/push")
@Tag(name = "Notifications Push", description = "Abonnement aux notifications push navigateur (Web Push)")
public class PushController {

    private final PushNotificationService pushNotificationService;
    private final DeviceTokenService deviceTokenService;
    private final UserRepository userRepository;

    public PushController(PushNotificationService pushNotificationService,
                          DeviceTokenService deviceTokenService,
                          UserRepository userRepository) {
        this.pushNotificationService = pushNotificationService;
        this.deviceTokenService = deviceTokenService;
        this.userRepository = userRepository;
    }

    @GetMapping("/vapid-public-key")
    public ResponseEntity<ApiResponse<VapidPublicKeyResponse>> vapidPublicKey() {
        return ResponseEntity.ok(ApiResponse.ok(new VapidPublicKeyResponse(pushNotificationService.getVapidPublicKey())));
    }

    @PostMapping("/subscribe")
    public ResponseEntity<Void> subscribe(@CurrentUser UUID userId, @Valid @RequestBody SubscribeRequest request) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        pushNotificationService.subscribe(user, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/subscribe")
    public ResponseEntity<Void> unsubscribe(@CurrentUser UUID userId, @RequestParam String endpoint) {
        pushNotificationService.unsubscribe(userId, endpoint);
        return ResponseEntity.noContent().build();
    }

    // ===== Apps natives (FCM / APNs) =====
    // Canal distinct du Web Push : la WKWebView d'iOS ne le supporte pas, une app
    // installée passe donc obligatoirement par un jeton FCM.

    @PostMapping("/device")
    @Operation(summary = "Enregistrer l'appareil pour les notifications natives")
    public ResponseEntity<Void> registerDevice(@CurrentUser UUID userId,
                                               @Valid @RequestBody RegisterDeviceRequest request) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        deviceTokenService.register(user, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/device")
    @Operation(summary = "Désenregistrer l'appareil")
    public ResponseEntity<Void> unregisterDevice(@CurrentUser UUID userId, @RequestParam String token) {
        deviceTokenService.unregister(userId, token);
        return ResponseEntity.noContent().build();
    }
}
