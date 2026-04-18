package orga.takwa.ummati.dto.organization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrganizationStatusRequest(
        @NotBlank String status,
        @Size(min = 20, message = "Le motif doit faire au moins 20 caractères") String reason
) {}

