package orga.takwa.ummati.controller.notification;

import io.swagger.v3.oas.annotations.tags.Tag;
import orga.takwa.ummati.config.security.CurrentUser;
import orga.takwa.ummati.dto.ApiResponse;
import orga.takwa.ummati.dto.PageResponse;
import orga.takwa.ummati.dto.notification.NotificationResponse;
import orga.takwa.ummati.service.NotificationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "Centre de notifications in-app")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> list(
            @CurrentUser UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(notificationService.listNotifications(userId, pageable))));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> unreadCount(@CurrentUser UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("count", notificationService.getUnreadCount(userId))));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@CurrentUser UUID userId, @PathVariable UUID id) {
        notificationService.markAsRead(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@CurrentUser UUID userId) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }
}
