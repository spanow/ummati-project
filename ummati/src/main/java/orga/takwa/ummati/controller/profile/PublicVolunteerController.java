package orga.takwa.ummati.controller.profile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import orga.takwa.ummati.dto.ApiResponse;
import orga.takwa.ummati.dto.profile.VolunteerPassport;
import orga.takwa.ummati.service.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Vitrine publique d'un bénévole. Sous {@code /api/v1/public/**}, donc accessible sans
 * authentification — la protection repose entièrement sur l'opt-in {@code profilePublic}
 * vérifié dans le service.
 */
@RestController
@RequestMapping("/api/v1/public/volunteers")
@Tag(name = "Passeport bénévole", description = "Vitrine publique d'un bénévole")
public class PublicVolunteerController {

    private final ProfileService profileService;

    public PublicVolunteerController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @Operation(summary = "Passeport public d'un bénévole (404 si non publié)")
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<VolunteerPassport>> getPassport(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(profileService.getPublicPassport(userId)));
    }
}
