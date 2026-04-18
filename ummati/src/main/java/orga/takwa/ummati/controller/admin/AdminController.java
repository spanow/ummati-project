package orga.takwa.ummati.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import orga.takwa.ummati.config.security.CurrentUser;
import orga.takwa.ummati.dto.ApiResponse;
import orga.takwa.ummati.dto.PageResponse;
import orga.takwa.ummati.dto.admin.AdminStatsResponse;
import orga.takwa.ummati.dto.admin.UserStatusRequest;
import orga.takwa.ummati.dto.organization.OrganizationSummary;
import orga.takwa.ummati.dto.user.UserSummary;
import orga.takwa.ummati.entity.enums.OrganizationStatus;
import orga.takwa.ummati.service.AdminService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
@Tag(name = "Administration", description = "Gestion de la plateforme — réservé PLATFORM_ADMIN")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> stats() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getStats()));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PageResponse<UserSummary>>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean enabled) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(adminService.listUsers(search, enabled, pageable))));
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<UserSummary>> changeUserStatus(
            @CurrentUser UUID adminId, @PathVariable UUID id,
            @Valid @RequestBody UserStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.changeUserStatus(adminId, id, request)));
    }

    @GetMapping("/organizations")
    public ResponseEntity<ApiResponse<PageResponse<OrganizationSummary>>> listOrganizations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        Pageable pageable = PageRequest.of(page, size);
        OrganizationStatus statusEnum = status != null ? OrganizationStatus.valueOf(status) : null;
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(adminService.listOrganizations(statusEnum, pageable))));
    }
}
