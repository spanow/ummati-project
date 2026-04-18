package orga.takwa.ummati.dto.organization;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOrganizationRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 5000) String description,
        @Size(max = 5000) String mission,
        @NotBlank String domain,
        String addressStreet,
        @NotBlank @Size(max = 100) String addressCity,
        @NotBlank @Size(max = 10) String addressZip,
        @Email @NotBlank String email,
        String phone,
        String website
) {}

