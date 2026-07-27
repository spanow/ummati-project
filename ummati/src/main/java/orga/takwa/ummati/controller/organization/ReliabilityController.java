package orga.takwa.ummati.controller.organization;

import io.swagger.v3.oas.annotations.tags.Tag;
import orga.takwa.ummati.config.security.CurrentUser;
import orga.takwa.ummati.dto.ApiResponse;
import orga.takwa.ummati.dto.event.ReliabilityResponse;
import orga.takwa.ummati.service.EventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations/{orgId}/members/{userId}/reliability")
@Tag(name = "Fiabilité", description = "Fiabilité d'un bénévole — réservée aux admins d'ONG")
public class ReliabilityController {

    private final EventService eventService;

    public ReliabilityController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ReliabilityResponse>> get(
            @CurrentUser UUID callerId, @PathVariable UUID orgId, @PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.getReliability(callerId, orgId, userId)));
    }
}
