package orga.takwa.ummati.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2test")
@Transactional
class PublicStatsControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void publicStats_shouldReturn200_withoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/public/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalVolunteers").isNumber())
                .andExpect(jsonPath("$.data.totalOrganizations").isNumber())
                .andExpect(jsonPath("$.data.totalEvents").isNumber())
                .andExpect(jsonPath("$.data.totalParticipations").isNumber());
    }
}
