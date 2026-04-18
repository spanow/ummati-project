package orga.takwa.ummati.dto.organization;

import jakarta.validation.constraints.Size;

public record UpdateOrganizationRequest(
        @Size(max = 5000) String description,
        @Size(max = 5000) String mission,
        String domain,
        String addressStreet,
        String addressCity,
        String addressZip,
        String email,
        String phone,
        String website
) {}

