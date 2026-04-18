package orga.takwa.ummati.dto.dashboard;

import java.util.List;

public record OrgAdminDashboard(
        String organizationName,
        long activeMembers,
        long pendingRequests,
        long eventsThisMonth,
        long totalEvents,
        Double averageFeedbackRating,
        List<MemberSummary> recentMembers
) {
    public record MemberSummary(String firstName, String lastName, String role, String joinedAt) {}
}

