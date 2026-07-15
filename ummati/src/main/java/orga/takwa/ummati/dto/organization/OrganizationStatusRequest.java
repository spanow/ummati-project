package orga.takwa.ummati.dto.organization;

import jakarta.validation.constraints.NotBlank;

public record OrganizationStatusRequest(
        @NotBlank String status,
        String reason
) {}

