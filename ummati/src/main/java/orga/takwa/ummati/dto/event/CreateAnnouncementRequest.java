package orga.takwa.ummati.dto.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAnnouncementRequest(
        @NotBlank @Size(min = 5, max = 1000) String content,
        boolean pinned
) {}
