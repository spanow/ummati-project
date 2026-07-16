package orga.takwa.ummati.controller.event;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import orga.takwa.ummati.config.security.CurrentUser;
import orga.takwa.ummati.dto.ApiResponse;
import orga.takwa.ummati.dto.event.AnnouncementResponse;
import orga.takwa.ummati.dto.event.CreateAnnouncementRequest;
import orga.takwa.ummati.service.EventAnnouncementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events/{eventId}/announcements")
@Tag(name = "Annonces", description = "Annonces admin sur les événements")
public class EventAnnouncementController {

    private final EventAnnouncementService announcementService;

    public EventAnnouncementController(EventAnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AnnouncementResponse>>> list(@PathVariable UUID eventId) {
        return ResponseEntity.ok(ApiResponse.ok(announcementService.list(eventId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AnnouncementResponse>> create(
            @CurrentUser UUID userId, @PathVariable UUID eventId,
            @Valid @RequestBody CreateAnnouncementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(announcementService.create(userId, eventId, request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> update(
            @CurrentUser UUID userId, @PathVariable UUID eventId,
            @PathVariable UUID id, @Valid @RequestBody CreateAnnouncementRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(announcementService.update(userId, id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@CurrentUser UUID userId,
                                       @PathVariable UUID eventId, @PathVariable UUID id) {
        announcementService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
