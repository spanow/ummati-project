package orga.takwa.ummati.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import orga.takwa.ummati.config.security.JwtTokenProvider;
import orga.takwa.ummati.entity.Organization;
import orga.takwa.ummati.entity.User;
import orga.takwa.ummati.entity.enums.OrganizationDomain;
import orga.takwa.ummati.entity.enums.OrganizationStatus;
import orga.takwa.ummati.entity.enums.UserRole;
import orga.takwa.ummati.repository.OrganizationRepository;
import orga.takwa.ummati.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2test")
@Transactional
class ReportControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UserRepository userRepository;
    @Autowired private OrganizationRepository organizationRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private User volunteerUser;
    private User adminPlatform;
    private String volunteerToken;
    private String adminToken;
    private Organization org;

    @BeforeEach
    void setUp() {
        volunteerUser = createUser("vol-" + UUID.randomUUID() + "@test.com", UserRole.VOLUNTEER);
        adminPlatform = createUser("adm-" + UUID.randomUUID() + "@test.com", UserRole.PLATFORM_ADMIN);
        volunteerToken = jwtTokenProvider.generateAccessToken(volunteerUser.getId(), volunteerUser.getEmail(), "VOLUNTEER");
        adminToken = jwtTokenProvider.generateAccessToken(adminPlatform.getId(), adminPlatform.getEmail(), "PLATFORM_ADMIN");
        org = createActiveOrg();
    }

    @Test
    void createReport_shouldReturn201_forAuthenticatedUser() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "targetType", "ORGANIZATION", "targetId", org.getId().toString(),
                "reason", "SPAM", "description", "Contenu suspect"));

        mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + volunteerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.targetLabel").value(org.getName()));
    }

    @Test
    void createReport_shouldReturn403_whenNotAuthenticated() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "targetType", "ORGANIZATION", "targetId", org.getId().toString(), "reason", "SPAM"));

        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void createReport_shouldReturn404_whenTargetDoesNotExist() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "targetType", "ORGANIZATION", "targetId", UUID.randomUUID().toString(), "reason", "SPAM"));

        mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + volunteerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void createReport_shouldReturn400_onDuplicatePendingReport() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "targetType", "ORGANIZATION", "targetId", org.getId().toString(), "reason", "SPAM"));

        mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + volunteerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + volunteerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminListReports_shouldReturn403_forRegularUser() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports")
                        .header("Authorization", "Bearer " + volunteerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminListReports_shouldReturn200_andResolveReport() throws Exception {
        String createBody = objectMapper.writeValueAsString(Map.of(
                "targetType", "ORGANIZATION", "targetId", org.getId().toString(), "reason", "FRAUD"));
        String createResponse = mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + volunteerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String reportId = objectMapper.readTree(createResponse).get("data").get("id").asText();

        mockMvc.perform(get("/api/v1/admin/reports")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        String resolveBody = objectMapper.writeValueAsString(Map.of(
                "status", "ACTION_TAKEN", "resolutionNote", "ONG suspendue suite au signalement"));
        mockMvc.perform(patch("/api/v1/admin/reports/" + reportId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resolveBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTION_TAKEN"))
                .andExpect(jsonPath("$.data.reviewedByName").isNotEmpty());
    }

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
        Organization o = new Organization();
        o.setName("Active Org " + UUID.randomUUID().toString().substring(0, 8));
        o.setSlug("active-" + UUID.randomUUID().toString().substring(0, 8));
        o.setDescription("Description");
        o.setDomain(OrganizationDomain.SOCIAL);
        o.setAddressCity("Lyon");
        o.setAddressZip("69001");
        o.setEmail("active@test.com");
        o.setStatus(OrganizationStatus.ACTIVE);
        o.setCreatedBy(volunteerUser);
        return organizationRepository.save(o);
    }
}
