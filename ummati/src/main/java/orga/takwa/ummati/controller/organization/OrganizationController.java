package orga.takwa.ummati.controller.organization;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import orga.takwa.ummati.config.security.CurrentUser;
import orga.takwa.ummati.dto.ApiResponse;
import orga.takwa.ummati.dto.PageResponse;
import orga.takwa.ummati.dto.organization.*;
import orga.takwa.ummati.entity.enums.OrganizationDomain;
import orga.takwa.ummati.service.OrganizationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations")
@Tag(name = "Organisations", description = "Gestion des ONG")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrganizationDetail>> create(
            @CurrentUser UUID userId, @Valid @RequestBody CreateOrganizationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(organizationService.create(userId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrganizationSummary>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String domain,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "name,asc") String sort) {
        String[] sortParts = sort.split(",");
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(sortParts.length > 1 && "desc".equalsIgnoreCase(sortParts[1])
                        ? Sort.Direction.DESC : Sort.Direction.ASC, sortParts[0]));
        OrganizationDomain domainEnum = domain != null ? OrganizationDomain.valueOf(domain) : null;
        var result = organizationService.list(domainEnum, city, search, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(result)));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<OrganizationDetail>> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.ok(organizationService.getBySlug(slug)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OrganizationDetail>> update(
            @CurrentUser UUID userId, @PathVariable UUID id,
            @Valid @RequestBody UpdateOrganizationRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(organizationService.update(userId, id, request)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<OrganizationDetail>> changeStatus(
            @CurrentUser UUID adminId, @PathVariable UUID id,
            @Valid @RequestBody OrganizationStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(organizationService.changeStatus(adminId, id, request)));
    }
}
