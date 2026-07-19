package orga.takwa.ummati.controller.admin;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import orga.takwa.ummati.config.security.CurrentUser;
import orga.takwa.ummati.dto.ApiResponse;
import orga.takwa.ummati.dto.PageResponse;
import orga.takwa.ummati.dto.report.ReportResponse;
import orga.takwa.ummati.dto.report.ResolveReportRequest;
import orga.takwa.ummati.entity.enums.ReportStatus;
import orga.takwa.ummati.service.ReportService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/reports")
@PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
@Tag(name = "Administration — Signalements", description = "Modération des signalements — réservé PLATFORM_ADMIN")
public class AdminReportController {

    private final ReportService reportService;

    public AdminReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ReportResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) ReportStatus status) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(reportService.list(status, pageable))));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<ReportResponse>> resolve(
            @CurrentUser UUID adminId,
            @PathVariable UUID id,
            @Valid @RequestBody ResolveReportRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.resolve(adminId, id, request)));
    }
}
