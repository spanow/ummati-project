package orga.takwa.ummati.dto.membership;

import jakarta.validation.constraints.Size;

public record MembershipRequest(
        @Size(max = 1000) String motivation
) {}

