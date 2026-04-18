package orga.takwa.ummati.service;

import orga.takwa.ummati.dto.dashboard.OrgAdminDashboard;
import orga.takwa.ummati.dto.dashboard.VolunteerDashboard;
import orga.takwa.ummati.dto.event.EventSummary;
import orga.takwa.ummati.dto.organization.OrganizationSummary;
import orga.takwa.ummati.entity.*;
import orga.takwa.ummati.entity.enums.*;
import orga.takwa.ummati.exception.ResourceNotFoundException;
import orga.takwa.ummati.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class DashboardService {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final EventRepository eventRepository;
    private final EventSignupRepository eventSignupRepository;
    private final EventFeedbackRepository eventFeedbackRepository;
    private final OrganizationService organizationService;
    private final EventService eventService;

    public DashboardService(UserRepository userRepository, MembershipRepository membershipRepository,
                            EventRepository eventRepository, EventSignupRepository eventSignupRepository,
                            EventFeedbackRepository eventFeedbackRepository,
                            OrganizationService organizationService, EventService eventService) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.eventRepository = eventRepository;
        this.eventSignupRepository = eventSignupRepository;
        this.eventFeedbackRepository = eventFeedbackRepository;
        this.organizationService = organizationService;
        this.eventService = eventService;
    }

    // T-104: Volunteer dashboard
    @Transactional(readOnly = true)
    public VolunteerDashboard getVolunteerDashboard(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        // My organizations
        var myMemberships = membershipRepository.findByUserIdAndStatus(userId, MembershipStatus.ACTIVE,
                PageRequest.of(0, 10));
        List<OrganizationSummary> myOrgs = myMemberships.getContent().stream()
                .map(m -> organizationService.toSummary(m.getOrganization()))
                .toList();

        // Upcoming events I'm signed up for
        var mySignups = eventSignupRepository.findByUserId(userId, PageRequest.of(0, 10));
        List<EventSummary> upcomingEvents = mySignups.getContent().stream()
                .filter(s -> s.getStatus() == SignupStatus.REGISTERED || s.getStatus() == SignupStatus.WAITLISTED)
                .filter(s -> s.getEvent().getStartDate().isAfter(LocalDateTime.now()))
                .map(s -> eventService.toSummaryPublic(s.getEvent()))
                .limit(5)
                .toList();

        // Suggested events (same city, published, future)
        Pageable pageable = PageRequest.of(0, 4, Sort.by("startDate"));
        var suggested = eventRepository.findAll(
                EventSpecification.search(EventStatus.PUBLISHED, LocalDateTime.now(),
                        null, user.getAddressCity(), null, null, null, null, null),
                pageable);
        List<EventSummary> suggestedEvents = suggested.getContent().stream()
                .map(eventService::toSummaryPublic)
                .toList();

        // Stats — count queries directes, pas de chargement en mémoire
        long eventsAttended = eventSignupRepository.countByUserIdAndStatus(userId, SignupStatus.ATTENDED);
        long orgsJoined = myMemberships.getTotalElements();

        return new VolunteerDashboard(user.getFirstName(), user.isOnboardingDone(),
                upcomingEvents, myOrgs, suggestedEvents,
                new VolunteerDashboard.StatsDto(eventsAttended, orgsJoined));
    }

    // T-105: Org admin dashboard
    @Transactional(readOnly = true)
    public OrgAdminDashboard getOrgAdminDashboard(UUID userId, UUID orgId) {
        organizationService.verifyAdmin(userId, orgId);
        Organization org = organizationService.findOrg(orgId);

        long activeMembers = membershipRepository.countByOrganizationIdAndRoleAndStatus(orgId, MembershipRole.MEMBER, MembershipStatus.ACTIVE)
                + membershipRepository.countByOrganizationIdAndRoleAndStatus(orgId, MembershipRole.ADMIN, MembershipStatus.ACTIVE);
        long pendingRequests = membershipRepository.countByOrganizationIdAndStatus(orgId, MembershipStatus.PENDING);

        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0);
        long eventsThisMonth = eventRepository.countByOrganizationIdAndCreatedAtAfter(orgId, monthStart);
        long totalEvents = eventRepository.countByOrganizationId(orgId);

        // Average feedback — une seule requête SQL agrégée
        Double avgRating = eventFeedbackRepository.findAverageRatingByOrganizationId(orgId);

        // Recent members
        var recentMembers = membershipRepository.findByOrganizationIdAndStatus(orgId, MembershipStatus.ACTIVE,
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "joinedAt")));
        List<OrgAdminDashboard.MemberSummary> members = recentMembers.getContent().stream()
                .map(m -> new OrgAdminDashboard.MemberSummary(
                        m.getUser().getFirstName(), m.getUser().getLastName(),
                        m.getRole().name(), m.getJoinedAt() != null ? m.getJoinedAt().toString() : null))
                .toList();

        return new OrgAdminDashboard(org.getName(), activeMembers, pendingRequests,
                eventsThisMonth, totalEvents, avgRating != null && avgRating != 0 ? avgRating : null, members);
    }
}

