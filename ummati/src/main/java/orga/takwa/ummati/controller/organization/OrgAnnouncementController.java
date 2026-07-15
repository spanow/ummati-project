package orga.takwa.ummati.controller.organization;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import orga.takwa.ummati.config.security.CurrentUser;
import orga.takwa.ummati.dto.ApiResponse;
import orga.takwa.ummati.dto.organization.CreateOrgAnnouncementRequest;
import orga.takwa.ummati.dto.organization.OrgAnnouncementResponse;
import orga.takwa.ummati.service.OrgAnnouncementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations/{orgId}/announcements")
@Tag(name = "Annonces Organisation", description = "Annonces admin au niveau de l'organisation")
public class OrgAnnouncementController {

    private final OrgAnnouncementService announcementService;

    public OrgAnnouncementController(OrgAnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrgAnnouncementResponse>>> list(@PathVariable UUID orgId) {
        return ResponseEntity.ok(ApiResponse.ok(announcementService.list(orgId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrgAnnouncementResponse>> create(
            @PathVariable UUID orgId,
            @Valid @RequestBody CreateOrgAnnouncementRequest request,
            @CurrentUser UUID adminId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(announcementService.create(adminId, orgId, request)));
    }

    @DeleteMapping("/{announcementId}")
    public ResponseEntity<Void> delete(@PathVariable UUID orgId,
                                       @PathVariable UUID announcementId,
                                       @CurrentUser UUID adminId) {
        announcementService.delete(adminId, announcementId);
        return ResponseEntity.noContent().build();
    }
}
