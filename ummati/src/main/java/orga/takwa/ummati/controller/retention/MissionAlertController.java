package orga.takwa.ummati.controller.retention;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import orga.takwa.ummati.config.security.CurrentUser;
import orga.takwa.ummati.dto.ApiResponse;
import orga.takwa.ummati.dto.alert.MissionAlertRequest;
import orga.takwa.ummati.dto.alert.MissionAlertResponse;
import orga.takwa.ummati.service.MissionAlertService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Recherches sauvegardées qui préviennent le bénévole des nouvelles missions. */
@RestController
@RequestMapping("/api/v1/profile/alerts")
@Tag(name = "Alertes missions", description = "Recherches sauvegardées et notifications")
public class MissionAlertController {

    private final MissionAlertService alertService;

    public MissionAlertController(MissionAlertService alertService) {
        this.alertService = alertService;
    }

    @Operation(summary = "Mes alertes")
    @GetMapping
    public ResponseEntity<ApiResponse<List<MissionAlertResponse>>> list(@CurrentUser UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(alertService.listMine(userId)));
    }

    @Operation(summary = "Créer une alerte")
    @PostMapping
    public ResponseEntity<ApiResponse<MissionAlertResponse>> create(
            @CurrentUser UUID userId, @Valid @RequestBody MissionAlertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(alertService.create(userId, request)));
    }

    @Operation(summary = "Modifier une alerte")
    @PutMapping("/{alertId}")
    public ResponseEntity<ApiResponse<MissionAlertResponse>> update(
            @CurrentUser UUID userId, @PathVariable UUID alertId,
            @Valid @RequestBody MissionAlertRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(alertService.update(userId, alertId, request)));
    }

    @Operation(summary = "Supprimer une alerte")
    @DeleteMapping("/{alertId}")
    public ResponseEntity<Void> delete(@CurrentUser UUID userId, @PathVariable UUID alertId) {
        alertService.delete(userId, alertId);
        return ResponseEntity.noContent().build();
    }
}
