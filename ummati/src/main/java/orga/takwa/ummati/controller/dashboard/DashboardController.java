package orga.takwa.ummati.controller.dashboard;

import orga.takwa.ummati.config.security.CurrentUser;
import orga.takwa.ummati.dto.ApiResponse;
import orga.takwa.ummati.dto.dashboard.OrgAdminDashboard;
import orga.takwa.ummati.dto.dashboard.VolunteerDashboard;
import orga.takwa.ummati.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/volunteer")
    public ResponseEntity<ApiResponse<VolunteerDashboard>> volunteerDashboard(@CurrentUser UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getVolunteerDashboard(userId)));
    }

    @GetMapping("/org-admin/{orgId}")
    public ResponseEntity<ApiResponse<OrgAdminDashboard>> orgAdminDashboard(
            @CurrentUser UUID userId, @PathVariable UUID orgId) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getOrgAdminDashboard(userId, orgId)));
    }
}

