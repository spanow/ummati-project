package orga.takwa.ummati.controller.retention;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import orga.takwa.ummati.config.security.CurrentUser;
import orga.takwa.ummati.dto.ApiResponse;
import orga.takwa.ummati.dto.PageResponse;
import orga.takwa.ummati.dto.event.EventSummary;
import orga.takwa.ummati.dto.organization.OrganizationSummary;
import orga.takwa.ummati.service.FavoriteService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Missions mises de côté et associations suivies.
 *
 * <p>Les bascules sont idempotentes : rappuyer sur le cœur ne provoque ni doublon ni
 * erreur, ce qui rend l'interface tolérante au double-clic et aux reprises réseau.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Favoris & suivis", description = "Missions sauvegardées et associations suivies")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    // --- Missions ---

    @Operation(summary = "Mettre une mission de côté")
    @PutMapping("/events/{eventId}/favorite")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> addFavorite(
            @CurrentUser UUID userId, @PathVariable UUID eventId) {
        favoriteService.addFavorite(userId, eventId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("favorite", true)));
    }

    @Operation(summary = "Retirer une mission des favoris")
    @DeleteMapping("/events/{eventId}/favorite")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> removeFavorite(
            @CurrentUser UUID userId, @PathVariable UUID eventId) {
        favoriteService.removeFavorite(userId, eventId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("favorite", false)));
    }

    @Operation(summary = "Mes missions mises de côté")
    @GetMapping("/profile/favorites")
    public ResponseEntity<ApiResponse<PageResponse<EventSummary>>> listFavorites(
            @CurrentUser UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(favoriteService.listFavorites(userId, pageable))));
    }

    // --- Associations ---

    @Operation(summary = "Suivre une association")
    @PutMapping("/organizations/{orgId}/follow")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> follow(
            @CurrentUser UUID userId, @PathVariable UUID orgId) {
        favoriteService.followOrganization(userId, orgId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("following", true)));
    }

    @Operation(summary = "Ne plus suivre une association")
    @DeleteMapping("/organizations/{orgId}/follow")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> unfollow(
            @CurrentUser UUID userId, @PathVariable UUID orgId) {
        favoriteService.unfollowOrganization(userId, orgId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("following", false)));
    }

    @Operation(summary = "Suis-je cette association ?")
    @GetMapping("/organizations/{orgId}/follow/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> followState(
            @CurrentUser UUID userId, @PathVariable UUID orgId) {
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "following", favoriteService.isFollowing(userId, orgId),
                "followerCount", favoriteService.followerCount(orgId))));
    }

    @Operation(summary = "Les associations que je suis")
    @GetMapping("/profile/following")
    public ResponseEntity<ApiResponse<PageResponse<OrganizationSummary>>> listFollowed(
            @CurrentUser UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(favoriteService.listFollowed(userId, pageable))));
    }
}
