package orga.takwa.ummati.controller.profile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import orga.takwa.ummati.config.security.CurrentUser;
import orga.takwa.ummati.dto.ApiResponse;
import orga.takwa.ummati.dto.PageResponse;
import orga.takwa.ummati.dto.event.SignupResponse;
import orga.takwa.ummati.dto.membership.MembershipResponse;
import orga.takwa.ummati.dto.profile.*;
import orga.takwa.ummati.entity.enums.MembershipStatus;
import orga.takwa.ummati.service.EventService;
import orga.takwa.ummati.service.MembershipService;
import orga.takwa.ummati.service.ProfileService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profile")
@Tag(name = "Profil", description = "Gestion du profil utilisateur")
public class ProfileController {

    private final ProfileService profileService;
    private final MembershipService membershipService;
    private final EventService eventService;

    public ProfileController(ProfileService profileService, MembershipService membershipService,
                             EventService eventService) {
        this.profileService = profileService;
        this.membershipService = membershipService;
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(@CurrentUser UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(profileService.getProfile(userId)));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @CurrentUser UUID userId, @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(profileService.updateProfile(userId, request)));
    }

    @PostMapping("/photo")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadPhoto(
            @CurrentUser UUID userId, @RequestParam("file") MultipartFile file) throws IOException {
        String url = profileService.uploadPhoto(userId, file);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("photoUrl", url)));
    }

    @Operation(summary = "Mon passeport bénévole (missions, heures, causes)")
    @GetMapping("/passport")
    public ResponseEntity<ApiResponse<VolunteerPassport>> getPassport(@CurrentUser UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(profileService.getOwnPassport(userId)));
    }

    @Operation(summary = "Publier ou dépublier mon passeport bénévole")
    @PutMapping("/passport/visibility")
    public ResponseEntity<ApiResponse<VolunteerPassport>> setPassportVisibility(
            @CurrentUser UUID userId, @RequestBody Map<String, Boolean> body) {
        boolean isPublic = Boolean.TRUE.equals(body.get("public"));
        return ResponseEntity.ok(ApiResponse.ok(profileService.setPassportVisibility(userId, isPublic)));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAccount(
            @CurrentUser UUID userId, @RequestBody Map<String, String> body) {
        profileService.deleteAccount(userId, body.get("password"));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @CurrentUser UUID userId, @Valid @RequestBody ChangePasswordRequest request) {
        profileService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.ok("Mot de passe modifié", null));
    }

    @PostMapping("/onboarding")
    public ResponseEntity<ApiResponse<ProfileResponse>> onboarding(
            @CurrentUser UUID userId, @Valid @RequestBody OnboardingRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(profileService.onboarding(userId, request)));
    }

    // T-111: My memberships
    @GetMapping("/memberships")
    public ResponseEntity<ApiResponse<PageResponse<MembershipResponse>>> myMemberships(
            @CurrentUser UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        Pageable pageable = PageRequest.of(page, size);
        MembershipStatus statusEnum = status != null ? MembershipStatus.valueOf(status) : MembershipStatus.ACTIVE;
        var result = membershipService.listByUser(userId, statusEnum, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(result)));
    }

    // T-111: My signups
    @GetMapping("/signups")
    public ResponseEntity<ApiResponse<PageResponse<SignupResponse>>> mySignups(
            @CurrentUser UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        var result = eventService.listUserSignups(userId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(result)));
    }

    // T-143: RGPD — export des données personnelles
    @GetMapping("/export")
    @Operation(summary = "Exporter mes données (RGPD)", description = "Retourne toutes les données personnelles en JSON (droit de portabilité)")
    public ResponseEntity<Map<String, Object>> exportProfile(@CurrentUser UUID userId) {
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"ummati-export.json\"")
                .body(profileService.exportProfile(userId));
    }
}
