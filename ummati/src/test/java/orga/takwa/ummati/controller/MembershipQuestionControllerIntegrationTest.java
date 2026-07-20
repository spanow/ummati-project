package orga.takwa.ummati.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import orga.takwa.ummati.config.security.JwtTokenProvider;
import orga.takwa.ummati.entity.*;
import orga.takwa.ummati.entity.enums.*;
import orga.takwa.ummati.repository.*;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2test")
@Transactional
class MembershipQuestionControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UserRepository userRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private MembershipRepository membershipRepository;
    @Autowired private MembershipQuestionRepository questionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User adminUser;
    private User volunteerUser;
    private Organization activeOrg;
    private String adminToken;
    private String volunteerToken;

    @BeforeEach
    void setUp() {
        adminUser = newUser("admin");
        volunteerUser = newUser("vol");

        activeOrg = new Organization();
        activeOrg.setName("ONG " + UUID.randomUUID().toString().substring(0, 6));
        activeOrg.setSlug("ong-" + UUID.randomUUID().toString().substring(0, 8));
        activeOrg.setDescription("Test");
        activeOrg.setDomain(OrganizationDomain.SOCIAL);
        activeOrg.setAddressCity("Paris");
        activeOrg.setAddressZip("75001");
        activeOrg.setEmail("ong@test.com");
        activeOrg.setStatus(OrganizationStatus.ACTIVE);
        activeOrg.setCreatedBy(adminUser);
        activeOrg = organizationRepository.save(activeOrg);

        Membership m = new Membership();
        m.setUser(adminUser);
        m.setOrganization(activeOrg);
        m.setRole(MembershipRole.ADMIN);
        m.setStatus(MembershipStatus.ACTIVE);
        m.setJoinedAt(LocalDateTime.now());
        membershipRepository.save(m);

        adminToken = jwtTokenProvider.generateAccessToken(adminUser.getId(), adminUser.getEmail(), adminUser.getRole().name());
        volunteerToken = jwtTokenProvider.generateAccessToken(volunteerUser.getId(), volunteerUser.getEmail(), volunteerUser.getRole().name());
    }

    @Test
    void createAndListQuestions() throws Exception {
        var body = Map.of("label", "Permis B ?", "type", "BOOLEAN", "required", false);
        mockMvc.perform(post("/api/v1/organizations/" + activeOrg.getId() + "/membership-questions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.label").value("Permis B ?"));

        mockMvc.perform(get("/api/v1/organizations/" + activeOrg.getId() + "/membership-questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].type").value("BOOLEAN"));
    }

    @Test
    void createQuestion_shouldReturn403_forNonAdmin() throws Exception {
        var body = Map.of("label", "X", "type", "TEXT", "required", false);
        mockMvc.perform(post("/api/v1/organizations/" + activeOrg.getId() + "/membership-questions")
                        .header("Authorization", "Bearer " + volunteerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    @Test
    void joinWithoutRequiredAnswer_shouldReturn400() throws Exception {
        saveQuestion("Pourquoi nous rejoindre ?", true);

        mockMvc.perform(post("/api/v1/organizations/" + activeOrg.getId() + "/memberships")
                        .header("Authorization", "Bearer " + volunteerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void joinWithAnswer_shouldSucceed_andAdminSeesAnswer() throws Exception {
        MembershipQuestion q = saveQuestion("Pourquoi nous rejoindre ?", true);

        String body = objectMapper.writeValueAsString(Map.of(
                "motivation", "Envie d'aider",
                "answers", List.of(Map.of("questionId", q.getId().toString(), "value", "Pour ma communauté"))));

        mockMvc.perform(post("/api/v1/organizations/" + activeOrg.getId() + "/memberships")
                        .header("Authorization", "Bearer " + volunteerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        UUID membershipId = membershipRepository
                .findByUserIdAndOrganizationId(volunteerUser.getId(), activeOrg.getId()).orElseThrow().getId();

        mockMvc.perform(get("/api/v1/memberships/" + membershipId + "/answers")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].questionLabel").value("Pourquoi nous rejoindre ?"))
                .andExpect(jsonPath("$.data[0].value").value("Pour ma communauté"));
    }

    // --- Helpers ---

    private User newUser(String prefix) {
        User u = new User();
        u.setEmail(prefix + "-" + UUID.randomUUID() + "@test.com");
        u.setPasswordHash(passwordEncoder.encode("Password1!"));
        u.setFirstName("Test");
        u.setLastName("User");
        u.setRole(UserRole.VOLUNTEER);
        u.setEmailVerified(true);
        u.setEnabled(true);
        return userRepository.save(u);
    }

    private MembershipQuestion saveQuestion(String label, boolean required) {
        MembershipQuestion q = new MembershipQuestion();
        q.setOrganization(activeOrg);
        q.setLabel(label);
        q.setType(QuestionType.TEXT);
        q.setRequired(required);
        q.setPosition(0);
        return questionRepository.save(q);
    }
}
