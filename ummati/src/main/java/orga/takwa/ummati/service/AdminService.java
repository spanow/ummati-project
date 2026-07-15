package orga.takwa.ummati.service;

import orga.takwa.ummati.dto.admin.AdminStatsResponse;
import orga.takwa.ummati.dto.admin.UserStatusRequest;
import orga.takwa.ummati.dto.organization.OrganizationSummary;
import orga.takwa.ummati.dto.user.UserSummary;
import orga.takwa.ummati.entity.Organization;
import orga.takwa.ummati.entity.User;
import orga.takwa.ummati.entity.enums.OrganizationStatus;
import orga.takwa.ummati.exception.ResourceNotFoundException;
import orga.takwa.ummati.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final EventRepository eventRepository;
    private final AuditService auditService;
    private final OrganizationService organizationService;

    public AdminService(UserRepository userRepository, OrganizationRepository organizationRepository,
                        EventRepository eventRepository, AuditService auditService,
                        OrganizationService organizationService) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.eventRepository = eventRepository;
        this.auditService = auditService;
        this.organizationService = organizationService;
    }

    // T-106: Platform admin stats
    @Transactional(readOnly = true)
    @Cacheable("admin-stats")
    public AdminStatsResponse getStats() {
        long totalUsers = userRepository.count();
        long totalOrgs = organizationRepository.count();
        long totalEvents = eventRepository.count();
        long pendingOrgs = organizationRepository.countByStatus(OrganizationStatus.PENDING);
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        long registrationsThisWeek = userRepository.countByCreatedAtAfter(weekAgo);
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        long eventsThisMonth = eventRepository.countByCreatedAtAfter(monthStart);
        return new AdminStatsResponse(totalUsers, totalOrgs, totalEvents, pendingOrgs,
                registrationsThisWeek, eventsThisMonth);
    }

    // T-107: List users
    @Transactional(readOnly = true)
    public Page<UserSummary> listUsers(String search, Boolean enabled, Pageable pageable) {
        Page<User> page = userRepository.findAll(UserSpecification.search(search, enabled), pageable);
        return page.map(u -> new UserSummary(u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(),
                u.getRole().name(), u.isEmailVerified(), u.isEnabled(), u.getCreatedAt()));
    }

    // T-108: Enable/disable user
    @Transactional
    public UserSummary changeUserStatus(UUID adminId, UUID userId, UserStatusRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        user.setEnabled(request.enabled());
        user = userRepository.save(user);
        auditService.log(adminId, request.enabled() ? "USER_ENABLED" : "USER_DISABLED", "User", userId);
        return new UserSummary(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                user.getRole().name(), user.isEmailVerified(), user.isEnabled(), user.getCreatedAt());
    }

    // T-109: List organizations by status
    @Transactional(readOnly = true)
    public Page<OrganizationSummary> listOrganizations(OrganizationStatus status, Pageable pageable) {
        Page<Organization> page = (status != null)
                ? organizationRepository.findByStatus(status, pageable)
                : organizationRepository.findAll(pageable);
        return page.map(organizationService::toSummary);
    }

}

