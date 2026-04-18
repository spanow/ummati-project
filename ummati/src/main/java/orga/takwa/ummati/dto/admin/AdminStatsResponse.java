package orga.takwa.ummati.dto.admin;

public record AdminStatsResponse(
        long totalUsers,
        long totalOrganizations,
        long totalEvents,
        long pendingOrganizations,
        long registrationsThisWeek,
        long eventsThisMonth
) {}

