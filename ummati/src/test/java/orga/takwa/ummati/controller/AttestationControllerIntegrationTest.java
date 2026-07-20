package orga.takwa.ummati.controller;

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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2test")
@Transactional
class AttestationControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UserRepository userRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private EventOccurrenceRepository eventOccurrenceRepository;
    @Autowired private EventSignupRepository eventSignupRepository;

    private User volunteer;
    private String token;
    private Organization org;

    @BeforeEach
    void setUp() {
        volunteer = new User();
        volunteer.setEmail("vol-" + UUID.randomUUID() + "@test.com");
        volunteer.setPasswordHash(passwordEncoder.encode("Password1!"));
        volunteer.setFirstName("Amina");
        volunteer.setLastName("Bénévole");
        volunteer.setRole(UserRole.VOLUNTEER);
        volunteer.setEmailVerified(true);
        volunteer.setEnabled(true);
        volunteer = userRepository.save(volunteer);
        token = jwtTokenProvider.generateAccessToken(volunteer.getId(), volunteer.getEmail(), volunteer.getRole().name());

        org = new Organization();
        org.setName("ONG Solidarité " + UUID.randomUUID().toString().substring(0, 6));
        org.setSlug("ong-" + UUID.randomUUID().toString().substring(0, 8));
        org.setDescription("Test");
        org.setDomain(OrganizationDomain.SOCIAL);
        org.setAddressCity("Paris");
        org.setAddressZip("75001");
        org.setEmail("ong@test.com");
        org.setStatus(OrganizationStatus.ACTIVE);
        org.setCreatedBy(volunteer);
        org = organizationRepository.save(org);
    }

    private void attendedMission(BigDecimal hours) {
        Event event = new Event();
        event.setOrganization(org);
        event.setTitle("Maraude " + UUID.randomUUID().toString().substring(0, 5));
        event.setDescription("Distribution");
        event.setType(EventType.MARAUDE);
        event.setLocationCity("Paris");
        event.setStartDate(LocalDateTime.now().minusDays(3));
        event.setEndDate(LocalDateTime.now().minusDays(3).plusHours(3));
        event.setStatus(EventStatus.COMPLETED);
        event.setCreatedBy(volunteer);
        event.setRequiredSkills(new java.util.HashSet<>());
        event = eventRepository.save(event);

        EventOccurrence occ = new EventOccurrence();
        occ.setEvent(event);
        occ.setStartDate(event.getStartDate());
        occ.setEndDate(event.getEndDate());
        occ.setStatus(EventOccurrenceStatus.COMPLETED);
        occ = eventOccurrenceRepository.save(occ);

        EventSignup signup = new EventSignup();
        signup.setEvent(event);
        signup.setOccurrence(occ);
        signup.setUser(volunteer);
        signup.setStatus(SignupStatus.ATTENDED);
        signup.setRegisteredAt(LocalDateTime.now().minusDays(5));
        signup.setAttendedAt(LocalDateTime.now().minusDays(3));
        signup.setHoursValidated(hours);
        eventSignupRepository.save(signup);
    }

    @Test
    void attestation_shouldReturnPdf_forCurrentUser() throws Exception {
        attendedMission(new BigDecimal("3.00"));
        attendedMission(new BigDecimal("2.50"));

        MvcResult result = mockMvc.perform(get("/api/v1/profile/attestation")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF))
                .andReturn();

        byte[] body = result.getResponse().getContentAsByteArray();
        assertThat(body).isNotEmpty();
        // Signature d'un fichier PDF valide.
        assertThat(new String(body, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    void attestation_shouldReturnPdf_evenWithNoMissions() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/profile/attestation")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        byte[] body = result.getResponse().getContentAsByteArray();
        assertThat(new String(body, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    void attestation_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/v1/profile/attestation"))
                .andExpect(status().isForbidden());
    }
}
