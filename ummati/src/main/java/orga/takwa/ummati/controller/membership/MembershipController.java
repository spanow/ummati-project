package orga.takwa.ummati.controller.membership;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import orga.takwa.ummati.config.security.CurrentUser;
import orga.takwa.ummati.dto.ApiResponse;
import orga.takwa.ummati.dto.PageResponse;
import orga.takwa.ummati.dto.membership.*;
import orga.takwa.ummati.entity.enums.MembershipStatus;
import orga.takwa.ummati.service.MembershipService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Memberships", description = "Adhésions aux organisations")
public class MembershipController {

    private final MembershipService membershipService;

    public MembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @PostMapping("/organizations/{orgId}/memberships")
    public ResponseEntity<ApiResponse<MembershipResponse>> requestMembership(
            @CurrentUser UUID userId, @PathVariable UUID orgId,
            @Valid @RequestBody(required = false) MembershipRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(membershipService.requestMembership(userId, orgId,
                        request != null ? request : new MembershipRequest(null))));
    }

    @GetMapping("/organizations/{orgId}/memberships")
    public ResponseEntity<ApiResponse<PageResponse<MembershipResponse>>> listMembers(
            @PathVariable UUID orgId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        MembershipStatus statusEnum = status != null ? MembershipStatus.valueOf(status) : null;
        var result = membershipService.listMembers(orgId, statusEnum, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(result)));
    }

    @PatchMapping("/memberships/{id}")
    public ResponseEntity<ApiResponse<MembershipResponse>> handleAction(
            @CurrentUser UUID userId, @PathVariable UUID id,
            @Valid @RequestBody MembershipActionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(membershipService.handleAction(userId, id, request)));
    }

    @PatchMapping("/memberships/{id}/role")
    public ResponseEntity<ApiResponse<MembershipResponse>> changeRole(
            @CurrentUser UUID userId, @PathVariable UUID id,
            @Valid @RequestBody MembershipRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(membershipService.changeRole(userId, id, request)));
    }

    @DeleteMapping("/memberships/{id}")
    public ResponseEntity<Void> removeMembership(@CurrentUser UUID userId, @PathVariable UUID id) {
        membershipService.removeMembership(userId, id);
        return ResponseEntity.noContent().build();
    }
}
