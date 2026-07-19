package orga.takwa.ummati.controller.stats;

import io.swagger.v3.oas.annotations.tags.Tag;
import orga.takwa.ummati.dto.ApiResponse;
import orga.takwa.ummati.dto.stats.PublicStatsResponse;
import orga.takwa.ummati.service.PublicStatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public")
@Tag(name = "Public", description = "Endpoints publics sans authentification")
public class PublicStatsController {

    private final PublicStatsService publicStatsService;

    public PublicStatsController(PublicStatsService publicStatsService) {
        this.publicStatsService = publicStatsService;
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<PublicStatsResponse>> stats() {
        return ResponseEntity.ok(ApiResponse.ok(publicStatsService.getStats()));
    }
}
