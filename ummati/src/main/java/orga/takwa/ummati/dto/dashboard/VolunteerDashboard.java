package orga.takwa.ummati.dto.dashboard;

import orga.takwa.ummati.dto.event.EventSummary;
import orga.takwa.ummati.dto.organization.OrganizationSummary;

import java.util.List;

public record VolunteerDashboard(
        String firstName,
        boolean onboardingDone,
        List<EventSummary> upcomingEvents,
        List<OrganizationSummary> myOrganizations,
        List<EventSummary> suggestedEvents,
        StatsDto stats
) {
    public record StatsDto(long eventsAttended, long organizationsJoined) {}
}

