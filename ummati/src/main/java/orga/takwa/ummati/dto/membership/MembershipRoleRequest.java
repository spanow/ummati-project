package orga.takwa.ummati.dto.membership;

import jakarta.validation.constraints.NotBlank;

public record MembershipRoleRequest(
        @NotBlank String role // MEMBER, ADMIN, ACCOUNTANT
) {}

