package orga.takwa.ummati.controller.image;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import orga.takwa.ummati.config.security.CurrentUser;
import orga.takwa.ummati.dto.ApiResponse;
import orga.takwa.ummati.dto.event.EventPhotoResponse;
import orga.takwa.ummati.service.EventService;
import orga.takwa.ummati.service.OrganizationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Visuels des ONG et des événements : logo, bannière, image de couverture et galerie
 * post-mission. Les avatars utilisateurs restent sur {@code /profile/photo}.
 *
 * <p>Toutes les écritures sont réservées aux administrateurs de l'ONG concernée ; la
 * vérification est faite dans les services métier.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Images", description = "Logos, bannières, couvertures et galeries")
public class ImageController {

    private final OrganizationService organizationService;
    private final EventService eventService;

    public ImageController(OrganizationService organizationService, EventService eventService) {
        this.organizationService = organizationService;
        this.eventService = eventService;
    }

    // --- ONG ---

    @Operation(summary = "Téléverser le logo de l'ONG")
    @PostMapping(value = "/organizations/{orgId}/logo", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadLogo(
            @CurrentUser UUID userId,
            @PathVariable UUID orgId,
            @RequestParam("file") MultipartFile file) {
        String url = organizationService.uploadLogo(userId, orgId, file);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("logoUrl", url)));
    }

    @Operation(summary = "Supprimer le logo de l'ONG")
    @DeleteMapping("/organizations/{orgId}/logo")
    public ResponseEntity<Void> deleteLogo(@CurrentUser UUID userId, @PathVariable UUID orgId) {
        organizationService.deleteLogo(userId, orgId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Téléverser la bannière de l'ONG")
    @PostMapping(value = "/organizations/{orgId}/banner", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadBanner(
            @CurrentUser UUID userId,
            @PathVariable UUID orgId,
            @RequestParam("file") MultipartFile file) {
        String url = organizationService.uploadBanner(userId, orgId, file);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("bannerUrl", url)));
    }

    @Operation(summary = "Supprimer la bannière de l'ONG")
    @DeleteMapping("/organizations/{orgId}/banner")
    public ResponseEntity<Void> deleteBanner(@CurrentUser UUID userId, @PathVariable UUID orgId) {
        organizationService.deleteBanner(userId, orgId);
        return ResponseEntity.noContent().build();
    }

    // --- Événements ---

    @Operation(summary = "Téléverser l'image de couverture d'un événement")
    @PostMapping(value = "/events/{eventId}/cover", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadCover(
            @CurrentUser UUID userId,
            @PathVariable UUID eventId,
            @RequestParam("file") MultipartFile file) {
        String url = eventService.uploadCover(userId, eventId, file);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("coverUrl", url)));
    }

    @Operation(summary = "Supprimer l'image de couverture d'un événement")
    @DeleteMapping("/events/{eventId}/cover")
    public ResponseEntity<Void> deleteCover(@CurrentUser UUID userId, @PathVariable UUID eventId) {
        eventService.deleteCover(userId, eventId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Ajouter une photo à la galerie d'un événement")
    @PostMapping(value = "/events/{eventId}/photos", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<EventPhotoResponse>> addPhoto(
            @CurrentUser UUID userId,
            @PathVariable UUID eventId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) @Size(max = 500) String caption) {
        EventPhotoResponse photo = eventService.addPhoto(userId, eventId, file, caption);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(photo));
    }

    @Operation(summary = "Lister les photos d'un événement (public)")
    @GetMapping("/events/{eventId}/photos")
    public ResponseEntity<ApiResponse<List<EventPhotoResponse>>> listPhotos(@PathVariable UUID eventId) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.listPhotos(eventId)));
    }

    @Operation(summary = "Supprimer une photo de galerie")
    @DeleteMapping("/events/photos/{photoId}")
    public ResponseEntity<Void> deletePhoto(@CurrentUser UUID userId, @PathVariable UUID photoId) {
        eventService.deletePhoto(userId, photoId);
        return ResponseEntity.noContent().build();
    }
}
