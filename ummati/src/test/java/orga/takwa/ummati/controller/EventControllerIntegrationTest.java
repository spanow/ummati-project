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
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2test")
@Transactional
class EventControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UserRepository userRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private MembershipRepository membershipRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private EventOccurrenceRepository eventOccurrenceRepository;
    @Autowired private EventSignupRepository eventSignupRepository;
    @Autowired private EventFeedbackRepository eventFeedbackRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private User adminUser;
    private User volunteerUser;
    private Organization activeOrg;
    private String adminToken;
    private String volunteerToken;

    @BeforeEach
    void setUp() {
        // Create admin user
        adminUser = new User();
        adminUser.setEmail("orgadmin-" + UUID.randomUUID() + "@test.com");
        adminUser.setPasswordHash(passwordEncoder.encode("Password1!"));
        adminUser.setFirstName("OrgAdmin");
        adminUser.setLastName("Test");
        adminUser.setRole(UserRole.VOLUNTEER);
        adminUser.setEmailVerified(true);
        adminUser.setEnabled(true);
        adminUser.setOnboardingDone(true);
        adminUser = userRepository.save(adminUser);

        // Create volunteer user
        volunteerUser = new User();
        volunteerUser.setEmail("volunteer-" + UUID.randomUUID() + "@test.com");
        volunteerUser.setPasswordHash(passwordEncoder.encode("Password1!"));
        volunteerUser.setFirstName("Bénévole");
        volunteerUser.setLastName("Test");
        volunteerUser.setRole(UserRole.VOLUNTEER);
        volunteerUser.setEmailVerified(true);
        volunteerUser.setEnabled(true);
        volunteerUser.setDateOfBirth(java.time.LocalDate.of(1995, 5, 15));
        volunteerUser = userRepository.save(volunteerUser);

        // Create active organization
        activeOrg = new Organization();
        activeOrg.setName("Test Org " + UUID.randomUUID().toString().substring(0, 8));
        activeOrg.setSlug("test-org-" + UUID.randomUUID().toString().substring(0, 8));
        activeOrg.setDescription("Test description for integration tests");
        activeOrg.setDomain(OrganizationDomain.SOCIAL);
        activeOrg.setAddressCity("Paris");
        activeOrg.setAddressZip("75001");
        activeOrg.setEmail("org@test.com");
        activeOrg.setStatus(OrganizationStatus.ACTIVE);
        activeOrg.setCreatedBy(adminUser);
        activeOrg = organizationRepository.save(activeOrg);

        // Admin membership
        Membership m = new Membership();
        m.setUser(adminUser);
        m.setOrganization(activeOrg);
        m.setRole(MembershipRole.ADMIN);
        m.setStatus(MembershipStatus.ACTIVE);
        m.setJoinedAt(LocalDateTime.now());
        membershipRepository.save(m);

        // JWT tokens
        adminToken = jwtTokenProvider.generateAccessToken(adminUser.getId(), adminUser.getEmail(), adminUser.getRole().name());
        volunteerToken = jwtTokenProvider.generateAccessToken(volunteerUser.getId(), volunteerUser.getEmail(), volunteerUser.getRole().name());
    }

    // ===== T-070: Create Event =====

    @Test
    void createEvent_shouldReturn201_whenAdmin() throws Exception {
        var body = Map.of(
                "title", "Maraude Paris",
                "description", "Distribution de repas",
                "type", "MARAUDE",
                "locationCity", "Paris",
                "startDate", LocalDateTime.now().plusDays(7).toString(),
                "endDate", LocalDateTime.now().plusDays(7).plusHours(3).toString(),
                "online", false
        );

        mockMvc.perform(post("/api/v1/organizations/" + activeOrg.getId() + "/events")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Maraude Paris"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.occurrences", hasSize(1)))
                .andExpect(jsonPath("$.data.organizationName").value(activeOrg.getName()));
    }

    @Test
    void createEvent_withRecurrence_shouldCreateMultipleOccurrences() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(3);
        var body = new HashMap<String, Object>();
        body.put("title", "Maraude hebdomadaire");
        body.put("description", "Chaque semaine");
        body.put("type", "MARAUDE");
        body.put("locationCity", "Paris");
        body.put("startDate", start.toString());
        body.put("endDate", start.plusHours(3).toString());
        body.put("online", false);
        body.put("recurrence", Map.of(
                "frequency", "WEEKLY", "interval", 1,
                "until", start.toLocalDate().plusWeeks(2).toString()));

        // 1 créneau principal + 2 générés (semaines +1, +2) = 3.
        mockMvc.perform(post("/api/v1/organizations/" + activeOrg.getId() + "/events")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.occurrences", hasSize(3)));
    }

    @Test
    void createEvent_shouldReturn403_whenNotAdmin() throws Exception {
        var body = Map.of(
                "title", "Event", "description", "Desc", "type", "FORMATION",
                "locationCity", "Lyon",
                "startDate", LocalDateTime.now().plusDays(7).toString(),
                "endDate", LocalDateTime.now().plusDays(7).plusHours(2).toString(),
                "online", false
        );

        mockMvc.perform(post("/api/v1/organizations/" + activeOrg.getId() + "/events")
                        .header("Authorization", "Bearer " + volunteerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createEvent_shouldReturn400_whenEndDateBeforeStart() throws Exception {
        var body = Map.of(
                "title", "Event", "description", "Desc", "type", "FORMATION",
                "locationCity", "Lyon",
                "startDate", LocalDateTime.now().plusDays(7).toString(),
                "endDate", LocalDateTime.now().plusDays(6).toString(),
                "online", false
        );

        mockMvc.perform(post("/api/v1/organizations/" + activeOrg.getId() + "/events")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ===== T-072: Change Status =====

    @Test
    void publishEvent_shouldReturn200_andNotifyMembers() throws Exception {
        Event event = createDraftEvent();

        mockMvc.perform(patch("/api/v1/events/" + event.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PUBLISH\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @Test
    void cancelEvent_shouldReturn200_withReason() throws Exception {
        Event event = createPublishedEvent();

        mockMvc.perform(patch("/api/v1/events/" + event.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCEL\",\"reason\":\"Météo très défavorable pour cette sortie\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.cancellationReason").value("Météo très défavorable pour cette sortie"));
    }

    @Test
    void cancelEvent_shouldReturn400_withShortReason() throws Exception {
        Event event = createPublishedEvent();

        mockMvc.perform(patch("/api/v1/events/" + event.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCEL\",\"reason\":\"short\"}"))
                .andExpect(status().isBadRequest());
    }

    // ===== T-073: List Events (public) =====

    @Test
    void listEvents_shouldReturn200_public() throws Exception {
        createPublishedEvent();

        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    // ===== T-074: Get Event Detail (public) =====

    @Test
    void getEvent_shouldReturn200_public() throws Exception {
        Event event = createPublishedEvent();

        mockMvc.perform(get("/api/v1/events/" + event.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value(event.getTitle()))
                .andExpect(jsonPath("$.data.registeredCount").value(0))
                .andExpect(jsonPath("$.data.occurrences", hasSize(1)));
    }

    @Test
    void getEvent_shouldReturn404_whenNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/events/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    // ===== T-075: Signup =====

    @Test
    void signup_shouldReturn201_registered() throws Exception {
        Event event = createPublishedEvent();

        mockMvc.perform(post("/api/v1/events/" + event.getId() + "/signups")
                        .header("Authorization", "Bearer " + volunteerToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("REGISTERED"));
    }

    @Test
    void signup_shouldReturn201_waitlisted_whenFull() throws Exception {
        Event event = createPublishedEventWithMaxParticipants(1);
        // Fill the only spot on the occurrence
        registerUser(event);

        mockMvc.perform(post("/api/v1/events/" + event.getId() + "/signups")
                        .header("Authorization", "Bearer " + volunteerToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("WAITLISTED"));
    }

    @Test
    void signupToSpecificOccurrence_shouldReturn201() throws Exception {
        Event event = createPublishedEvent();
        EventOccurrence occ2 = new EventOccurrence();
        occ2.setEvent(event);
        occ2.setStartDate(LocalDateTime.now().plusDays(14));
        occ2.setEndDate(LocalDateTime.now().plusDays(14).plusHours(3));
        occ2.setStatus(EventOccurrenceStatus.PUBLISHED);
        occ2 = eventOccurrenceRepository.save(occ2);

        mockMvc.perform(post("/api/v1/events/" + event.getId() + "/occurrences/" + occ2.getId() + "/signups")
                        .header("Authorization", "Bearer " + volunteerToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("REGISTERED"))
                .andExpect(jsonPath("$.data.occurrenceId").value(occ2.getId().toString()));
    }

    @Test
    void signup_shouldReturn409_whenDuplicate() throws Exception {
        Event event = createPublishedEvent();
        saveSignup(event, volunteerUser, SignupStatus.REGISTERED, LocalDateTime.now());

        mockMvc.perform(post("/api/v1/events/" + event.getId() + "/signups")
                        .header("Authorization", "Bearer " + volunteerToken))
                .andExpect(status().isConflict());
    }

    @Test
    void signup_shouldReturn400_whenDeadlinePassed() throws Exception {
        Event event = createPublishedEvent();
        EventOccurrence occ = occurrenceOf(event);
        occ.setRegistrationDeadline(LocalDateTime.now().minusDays(1));
        eventOccurrenceRepository.save(occ);

        mockMvc.perform(post("/api/v1/events/" + event.getId() + "/signups")
                        .header("Authorization", "Bearer " + volunteerToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signup_shouldReturn401or403_whenNotAuthenticated() throws Exception {
        Event event = createPublishedEvent();

        mockMvc.perform(post("/api/v1/events/" + event.getId() + "/signups"))
                .andExpect(status().isForbidden());
    }

    // ===== T-076: Cancel Signup =====

    @Test
    void cancelSignup_shouldReturn204() throws Exception {
        Event event = createPublishedEvent();
        saveSignup(event, volunteerUser, SignupStatus.REGISTERED, LocalDateTime.now());

        mockMvc.perform(delete("/api/v1/events/" + event.getId() + "/signups")
                        .header("Authorization", "Bearer " + volunteerToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void cancelSignup_shouldPromoteWaitlisted() throws Exception {
        Event event = createPublishedEventWithMaxParticipants(1);
        saveSignup(event, volunteerUser, SignupStatus.REGISTERED, LocalDateTime.now().minusHours(2));

        // Waitlist another user
        User waitUser = new User();
        waitUser.setEmail("wait-" + UUID.randomUUID() + "@test.com");
        waitUser.setPasswordHash(passwordEncoder.encode("Password1!"));
        waitUser.setFirstName("Wait");
        waitUser.setLastName("User");
        waitUser.setRole(UserRole.VOLUNTEER);
        waitUser.setEmailVerified(true);
        waitUser.setEnabled(true);
        waitUser = userRepository.save(waitUser);
        saveSignup(event, waitUser, SignupStatus.WAITLISTED, LocalDateTime.now().minusHours(1));

        // Cancel the registered signup
        mockMvc.perform(delete("/api/v1/events/" + event.getId() + "/signups")
                        .header("Authorization", "Bearer " + volunteerToken))
                .andExpect(status().isNoContent());

        // Verify waitlisted was promoted
        EventSignup promoted = eventSignupRepository
                .findByEventIdAndUserId(event.getId(), waitUser.getId()).get(0);
        Assertions.assertEquals(SignupStatus.REGISTERED, promoted.getStatus());
    }

    // ===== T-077: List Signups (admin) =====

    @Test
    void listSignups_shouldReturn200_forAdmin() throws Exception {
        Event event = createPublishedEvent();

        mockMvc.perform(get("/api/v1/events/" + event.getId() + "/signups")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void listSignups_shouldReturn403_forNonAdmin() throws Exception {
        Event event = createPublishedEvent();

        mockMvc.perform(get("/api/v1/events/" + event.getId() + "/signups")
                        .header("Authorization", "Bearer " + volunteerToken))
                .andExpect(status().isForbidden());
    }

    // ===== T-078: Export CSV =====

    @Test
    void exportCsv_shouldReturnCsv_forAdmin() throws Exception {
        Event event = createPublishedEvent();

        mockMvc.perform(get("/api/v1/events/" + event.getId() + "/signups/export")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"));
    }

    // ===== T-079: Mark Attendance =====

    @Test
    void markAttendance_shouldReturn200() throws Exception {
        Event event = createPublishedEvent();
        EventSignup signup = saveSignup(event, volunteerUser, SignupStatus.REGISTERED, LocalDateTime.now());

        String body = objectMapper.writeValueAsString(Map.of("userIds", List.of(volunteerUser.getId())));

        mockMvc.perform(patch("/api/v1/events/" + event.getId() + "/signups/attendance")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        EventSignup updated = eventSignupRepository.findById(signup.getId()).orElseThrow();
        Assertions.assertEquals(SignupStatus.ATTENDED, updated.getStatus());
        Assertions.assertNotNull(updated.getAttendedAt());
    }

    // ===== T-080: Create Feedback =====

    @Test
    void createFeedback_shouldReturn201_whenAttended() throws Exception {
        Event event = createPublishedEvent();
        saveSignup(event, volunteerUser, SignupStatus.ATTENDED, LocalDateTime.now());

        var body = Map.of("rating", 4, "comment", "Très bien organisé", "anonymous", false);

        mockMvc.perform(post("/api/v1/events/" + event.getId() + "/feedbacks")
                        .header("Authorization", "Bearer " + volunteerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.rating").value(4))
                .andExpect(jsonPath("$.data.userFirstName").value("Bénévole"));
    }

    @Test
    void createFeedback_shouldReturn400_whenNotAttended() throws Exception {
        Event event = createPublishedEvent();
        saveSignup(event, volunteerUser, SignupStatus.REGISTERED, LocalDateTime.now());

        var body = Map.of("rating", 5, "comment", "Nice", "anonymous", false);

        mockMvc.perform(post("/api/v1/events/" + event.getId() + "/feedbacks")
                        .header("Authorization", "Bearer " + volunteerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createFeedback_shouldHideNameWhenAnonymous() throws Exception {
        Event event = createPublishedEvent();
        saveSignup(event, volunteerUser, SignupStatus.ATTENDED, LocalDateTime.now());

        var body = Map.of("rating", 3, "comment", "Correct", "anonymous", true);

        mockMvc.perform(post("/api/v1/events/" + event.getId() + "/feedbacks")
                        .header("Authorization", "Bearer " + volunteerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userFirstName").isEmpty())
                .andExpect(jsonPath("$.data.userLastName").isEmpty());
    }

    // ===== T-081: List Feedbacks (public) =====

    @Test
    void listFeedbacks_shouldReturn200_public() throws Exception {
        Event event = createPublishedEvent();

        mockMvc.perform(get("/api/v1/events/" + event.getId() + "/feedbacks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feedbacks.content").isArray());
    }

    // ===== T-071: Update Event =====

    @Test
    void updateDraftEvent_shouldReturn200() throws Exception {
        Event event = createDraftEvent();

        var body = Map.of("title", "Updated Title", "description", "Updated desc");

        mockMvc.perform(put("/api/v1/events/" + event.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated Title"));
    }

    @Test
    void updatePublishedEvent_shouldReturn400_whenChangingDates() throws Exception {
        Event event = createPublishedEvent();

        var body = Map.of("startDate", LocalDateTime.now().plusDays(20).toString());

        mockMvc.perform(put("/api/v1/events/" + event.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ===== PR3 : fiabilité =====

    @Test
    void markNoShow_shouldReturn200_andSetStatus() throws Exception {
        Event event = createPublishedEvent();
        EventSignup s = saveSignup(event, volunteerUser, SignupStatus.REGISTERED, LocalDateTime.now());
        EventOccurrence occ = occurrenceOf(event);

        String body = objectMapper.writeValueAsString(Map.of("userIds", List.of(volunteerUser.getId())));
        mockMvc.perform(patch("/api/v1/events/" + event.getId() + "/occurrences/" + occ.getId() + "/signups/no-show")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        Assertions.assertEquals(SignupStatus.NO_SHOW,
                eventSignupRepository.findById(s.getId()).orElseThrow().getStatus());
    }

    @Test
    void reliability_shouldReturnCounts_forAdmin() throws Exception {
        Event e1 = createPublishedEvent();
        saveSignup(e1, volunteerUser, SignupStatus.ATTENDED, LocalDateTime.now());
        Event e2 = createPublishedEvent();
        saveSignup(e2, volunteerUser, SignupStatus.NO_SHOW, LocalDateTime.now());

        mockMvc.perform(get("/api/v1/organizations/" + activeOrg.getId()
                        + "/members/" + volunteerUser.getId() + "/reliability")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attendedCount").value(1))
                .andExpect(jsonPath("$.data.noShowCount").value(1))
                .andExpect(jsonPath("$.data.reliabilityRate").value(0.5));
    }

    @Test
    void reliability_shouldReturn403_forNonAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/" + activeOrg.getId()
                        + "/members/" + volunteerUser.getId() + "/reliability")
                        .header("Authorization", "Bearer " + volunteerToken))
                .andExpect(status().isForbidden());
    }

    // ===== Helpers =====

    private Event createDraftEvent() {
        return createEventWithOccurrence(EventStatus.DRAFT, EventOccurrenceStatus.DRAFT, null);
    }

    private Event createPublishedEvent() {
        return createEventWithOccurrence(EventStatus.PUBLISHED, EventOccurrenceStatus.PUBLISHED, null);
    }

    private Event createPublishedEventWithMaxParticipants(int max) {
        return createEventWithOccurrence(EventStatus.PUBLISHED, EventOccurrenceStatus.PUBLISHED, max);
    }

    // Crée un événement mono-créneau (série + son unique occurrence miroir).
    private Event createEventWithOccurrence(EventStatus eventStatus, EventOccurrenceStatus occStatus, Integer max) {
        Event event = new Event();
        event.setOrganization(activeOrg);
        event.setTitle("Event " + UUID.randomUUID().toString().substring(0, 8));
        event.setDescription("Test event");
        event.setType(EventType.FORMATION);
        event.setLocationCity("Paris");
        event.setStartDate(LocalDateTime.now().plusDays(7));
        event.setEndDate(LocalDateTime.now().plusDays(7).plusHours(3));
        event.setMaxParticipants(max);
        event.setStatus(eventStatus);
        event.setCreatedBy(adminUser);
        event.setRequiredSkills(new HashSet<>());
        event = eventRepository.save(event);

        EventOccurrence occ = new EventOccurrence();
        occ.setEvent(event);
        occ.setStartDate(event.getStartDate());
        occ.setEndDate(event.getEndDate());
        occ.setMaxParticipants(max);
        occ.setStatus(occStatus);
        eventOccurrenceRepository.save(occ);
        return event;
    }

    private EventOccurrence occurrenceOf(Event event) {
        return eventOccurrenceRepository.findByEventIdOrderByStartDateAsc(event.getId()).get(0);
    }

    private EventSignup saveSignup(Event event, User u, SignupStatus status, LocalDateTime registeredAt) {
        EventSignup signup = new EventSignup();
        signup.setEvent(event);
        signup.setOccurrence(occurrenceOf(event));
        signup.setUser(u);
        signup.setStatus(status);
        signup.setRegisteredAt(registeredAt);
        if (status == SignupStatus.ATTENDED) {
            signup.setAttendedAt(LocalDateTime.now());
        }
        return eventSignupRepository.save(signup);
    }

    private void registerUser(Event event) {
        User filler = new User();
        filler.setEmail("filler-" + UUID.randomUUID() + "@test.com");
        filler.setPasswordHash(passwordEncoder.encode("Password1!"));
        filler.setFirstName("Filler");
        filler.setLastName("User");
        filler.setRole(UserRole.VOLUNTEER);
        filler.setEmailVerified(true);
        filler.setEnabled(true);
        filler = userRepository.save(filler);

        saveSignup(event, filler, SignupStatus.REGISTERED, LocalDateTime.now());
    }
}
