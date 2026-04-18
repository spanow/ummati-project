package orga.takwa.ummati.dto.organization;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrganizationSummary(
        UUID id, String name, String slug, String domain,
        String logoUrl, String city, long memberCount, String descriptionExcerpt
) {}

