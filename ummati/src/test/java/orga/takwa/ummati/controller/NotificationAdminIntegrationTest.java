package orga.takwa.ummati.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import orga.takwa.ummati.config.security.JwtTokenProvider;
import orga.takwa.ummati.entity.*;
import orga.takwa.ummati.entity.enums.*;
import orga.takwa.ummati.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2test")
@Transactional
class NotificationAdminIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UserRepository userRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private MembershipRepository membershipRepository;
    @Autowired private NotificationRepository notificationRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private User volunteerUser;
    private User adminPlatform;
    private String volunteerToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        volunteerUser = createUser("vol-" + UUID.randomUUID() + "@test.com", UserRole.VOLUNTEER);
        adminPlatform = createUser("adm-" + UUID.randomUUID() + "@test.com", UserRole.PLATFORM_ADMIN);
        volunteerToken = jwtTokenProvider.generateAccessToken(volunteerUser.getId(), volunteerUser.getEmail(), "VOLUNTEER");
        adminToken = jwtTokenProvider.generateAccessToken(adminPlatform.getId(), adminPlatform.getEmail(), "PLATFORM_ADMIN");
    }

    // ===== T-101: Notifications =====

    @Test
    void listNotifications_shouldReturn200_forAuthenticatedUser() throws Exception {
        // Create a notification for this user
        Notification n = new Notification();
        n.setUser(volunteerUser);
        n.setType(NotificationType.WELCOME);
        n.setTitle("Bienvenue");
        n.setMessage("Bienvenue sur Ummati !");
        notificationRepository.save(n);

        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + volunteerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void unreadCount_shouldReturnCount() throws Exception {
        Notification n = new Notification();
        n.setUser(volunteerUser);
        n.setType(NotificationType.GENERAL);
        n.setTitle("Test");
        n.setMessage("Message");
        notificationRepository.save(n);

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + volunteerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(1));
    }

    @Test
    void markAsRead_shouldReturn204() throws Exception {
        Notification n = new Notification();
        n.setUser(volunteerUser);
        n.setType(NotificationType.GENERAL);
        n.setTitle("Test");
        n.setMessage("Message");
        n = notificationRepository.save(n);

        mockMvc.perform(patch("/api/v1/notifications/" + n.getId() + "/read")
                        .header("Authorization", "Bearer " + volunteerToken))
                .andExpect(status().isNoContent());

        Notification updated = notificationRepository.findById(n.getId()).orElseThrow();
        Assertions.assertTrue(updated.isRead());
    }

    @Test
    void markAllAsRead_shouldReturn204() throws Exception {
        for (int i = 0; i < 3; i++) {
            Notification n = new Notification();
            n.setUser(volunteerUser);
            n.setType(NotificationType.GENERAL);
            n.setTitle("Test " + i);
            n.setMessage("Message");
            notificationRepository.save(n);
        }

        mockMvc.perform(patch("/api/v1/notifications/read-all")
                        .header("Authorization", "Bearer " + volunteerToken))
                .andExpect(status().isNoContent());

        long unread = notificationRepository.countByUserIdAndReadFalse(volunteerUser.getId());
        Assertions.assertEquals(0, unread);
    }

    @Test
    void markAsRead_shouldReturn403_forOtherUserNotification() throws Exception {
        User other = createUser("other-" + UUID.randomUUID() + "@test.com", UserRole.VOLUNTEER);
        Notification n = new Notification();
        n.setUser(other);
        n.setType(NotificationType.GENERAL);
        n.setTitle("Test");
        n.setMessage("Msg");
        n = notificationRepository.save(n);

        mockMvc.perform(patch("/api/v1/notifications/" + n.getId() + "/read")
                        .header("Authorization", "Bearer " + volunteerToken))
                .andExpect(status().isForbidden());
    }

    // ===== T-106-T-109: Admin =====

    @Test
    void adminStats_shouldReturn200_forPlatformAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalUsers").isNumber())
                .andExpect(jsonPath("$.data.totalOrganizations").isNumber());
    }

    @Test
    void adminStats_shouldReturn403_forRegularUser() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats")
                        .header("Authorization", "Bearer " + volunteerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void listUsers_shouldReturn200_forAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void listUsers_searchByEmail_shouldFilter() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .param("search", volunteerUser.getEmail())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void changeUserStatus_shouldDisableUser() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("enabled", false));
        mockMvc.perform(patch("/api/v1/admin/users/" + volunteerUser.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));

        User updated = userRepository.findById(volunteerUser.getId()).orElseThrow();
        Assertions.assertFalse(updated.isEnabled());
    }

    @Test
    void changeUserStatus_shouldReturn403_forRegularUser() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("enabled", false));
        mockMvc.perform(patch("/api/v1/admin/users/" + volunteerUser.getId() + "/status")
                        .header("Authorization", "Bearer " + volunteerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void listOrganizations_shouldReturn200_forAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/organizations")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void listOrganizations_shouldFilterByStatus() throws Exception {
        // Create a pending org
        Organization org = new Organization();
        org.setName("Test Org " + UUID.randomUUID().toString().substring(0, 8));
        org.setSlug("test-" + UUID.randomUUID().toString().substring(0, 8));
        org.setDescription("Description");
        org.setDomain(OrganizationDomain.SOCIAL);
        org.setAddressCity("Paris");
        org.setAddressZip("75001");
        org.setEmail("org@test.com");
        org.setStatus(OrganizationStatus.PENDING);
        org.setCreatedBy(volunteerUser);
        organizationRepository.save(org);

        mockMvc.perform(get("/api/v1/admin/organizations")
                        .param("status", "PENDING")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    // ===== T-111: Profile memberships & signups =====

    @Test
    void profileMemberships_shouldReturn200() throws Exception {
        Organization org = createActiveOrg();
        Membership m = new Membership();
        m.setUser(volunteerUser);
        m.setOrganization(org);
        m.setRole(MembershipRole.MEMBER);
        m.setStatus(MembershipStatus.ACTIVE);
        m.setJoinedAt(LocalDateTime.now());
        membershipRepository.save(m);

        mockMvc.perform(get("/api/v1/profile/memberships")
                        .header("Authorization", "Bearer " + volunteerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void profileSignups_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/v1/profile/signups")
                        .header("Authorization", "Bearer " + volunteerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    // ===== Helpers =====

    private User createUser(String email, UserRole role) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode("Password1!"));
        u.setFirstName("Test");
        u.setLastName("User");
        u.setRole(role);
        u.setEmailVerified(true);
        u.setEnabled(true);
        return userRepository.save(u);
    }

    private Organization createActiveOrg() {
        Organization org = new Organization();
        org.setName("Active Org " + UUID.randomUUID().toString().substring(0, 8));
        org.setSlug("active-" + UUID.randomUUID().toString().substring(0, 8));
        org.setDescription("Description");
        org.setDomain(OrganizationDomain.SOCIAL);
        org.setAddressCity("Lyon");
        org.setAddressZip("69001");
        org.setEmail("active@test.com");
        org.setStatus(OrganizationStatus.ACTIVE);
        org.setCreatedBy(volunteerUser);
        return organizationRepository.save(org);
    }
}

