package orga.takwa.ummati.controller.push;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import orga.takwa.ummati.config.security.CurrentUser;
import orga.takwa.ummati.dto.ApiResponse;
import orga.takwa.ummati.dto.push.SubscribeRequest;
import orga.takwa.ummati.dto.push.VapidPublicKeyResponse;
import orga.takwa.ummati.exception.ResourceNotFoundException;
import orga.takwa.ummati.repository.UserRepository;
import orga.takwa.ummati.service.PushNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/push")
@Tag(name = "Notifications Push", description = "Abonnement aux notifications push navigateur (Web Push)")
public class PushController {

    private final PushNotificationService pushNotificationService;
    private final UserRepository userRepository;

    public PushController(PushNotificationService pushNotificationService, UserRepository userRepository) {
        this.pushNotificationService = pushNotificationService;
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
}
