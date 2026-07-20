package orga.takwa.ummati.dto.organization;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrganizationDetail(
        UUID id, String name, String slug, String description, String mission,
        String domain, String logoUrl, String bannerUrl,
        String addressStreet, String addressCity, String addressZip, String addressCountry,
        BigDecimal addressLat, BigDecimal addressLng,
        String phone, String email, String website,
        String status, String rejectionReason,
        StatsDto stats, LocalDateTime createdAt
) {
    public record StatsDto(long memberCount, long eventCount, Double averageRating) {}
}

