package orga.takwa.ummati.dto.stats;

public record PublicStatsResponse(
        long totalVolunteers,
        long totalOrganizations,
        long totalEvents,
        long totalParticipations
) {}
