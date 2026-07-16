package orga.takwa.ummati.dto.organization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOrgAnnouncementRequest(
        @NotBlank @Size(min = 3, max = 200) String title,
        @NotBlank @Size(min = 5, max = 2000) String content,
        boolean pinned
) {}
