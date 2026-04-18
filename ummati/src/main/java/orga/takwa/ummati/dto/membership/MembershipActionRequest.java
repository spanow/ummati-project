package orga.takwa.ummati.dto.membership;

import jakarta.validation.constraints.NotBlank;

public record MembershipActionRequest(
        @NotBlank String action // APPROVE or REJECT
) {}

