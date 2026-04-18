package orga.takwa.ummati.dto.event;

import jakarta.validation.constraints.NotBlank;

public record EventStatusRequest(
        @NotBlank String status,
        String reason
) {}

