package com.gym.workload.rest;

import com.gym.workload.config.TestSecurityConfig;
import com.gym.workload.document.MonthSummary;
import com.gym.workload.document.TrainerWorkload;
import com.gym.workload.document.YearSummary;
import com.gym.workload.repository.TrainerWorkloadRepository;
import com.gym.workload.security.JwtAuthenticationFilter;
import com.gym.workload.security.JwtService;
import com.gym.workload.security.WorkloadUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkloadController.class)
@Import({JwtAuthenticationFilter.class, TestSecurityConfig.class})
class WorkloadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TrainerWorkloadRepository repository;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private WorkloadUserDetailsService userDetailsService;

    private static final String TOKEN = "test-bearer-token";
    private static final String SERVICE_USER = "gym-management-service";

    @BeforeEach
    void setUp() {
        UserDetails ud = User.withUsername(SERVICE_USER).password("").roles("SERVICE").build();
        lenient().when(jwtService.isValid(TOKEN)).thenReturn(true);
        lenient().when(jwtService.extractUsername(TOKEN)).thenReturn(SERVICE_USER);
        lenient().when(userDetailsService.loadUserByUsername(SERVICE_USER)).thenReturn(ud);
    }

    private String bearer() {
        return "Bearer " + TOKEN;
    }

    @Test
    @DisplayName("GET 200 OK – returns summary for known trainer")
    void get_knownTrainer_returns200() throws Exception {
        TrainerWorkload workload = new TrainerWorkload();
        workload.setUsername("trainer.jane");
        workload.setFirstName("Jane");
        workload.setLastName("Doe");
        workload.setActive(true);
        workload.setYears(List.of(new YearSummary(2026, List.of(new MonthSummary(7, 60L)))));
        when(repository.findByUsername("trainer.jane")).thenReturn(Optional.of(workload));

        mockMvc.perform(get("/api/trainer-workload/{username}", "trainer.jane")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("trainer.jane"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.years[0].year").value(2026))
                .andExpect(jsonPath("$.years[0].months[0].month").value(7))
                .andExpect(jsonPath("$.years[0].months[0].summaryDuration").value(60));
    }

    @Test
    @DisplayName("GET /search 200 OK – returns trainers matching first + last name")
    void search_byName_returnsMatches() throws Exception {
        TrainerWorkload workload = new TrainerWorkload();
        workload.setUsername("trainer.jane");
        workload.setFirstName("Jane");
        workload.setLastName("Doe");
        when(repository.findByFirstNameAndLastName("Jane", "Doe")).thenReturn(List.of(workload));

        mockMvc.perform(get("/api/trainer-workload/search")
                        .param("firstName", "Jane")
                        .param("lastName", "Doe")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("trainer.jane"))
                .andExpect(jsonPath("$[0].firstName").value("Jane"))
                .andExpect(jsonPath("$[0].lastName").value("Doe"));
    }

    @Test
    @DisplayName("GET /search 200 OK – empty array when no trainer matches")
    void search_byName_noMatch_returnsEmptyArray() throws Exception {
        when(repository.findByFirstNameAndLastName("No", "Body")).thenReturn(List.of());

        mockMvc.perform(get("/api/trainer-workload/search")
                        .param("firstName", "No")
                        .param("lastName", "Body")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET 404 Not Found – unknown trainer")
    void get_unknownTrainer_returns404() throws Exception {
        when(repository.findByUsername("nobody")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/trainer-workload/{username}", "nobody")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET 401 Unauthorized – no Bearer token")
    void get_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/trainer-workload/{username}", "trainer.jane"))
                .andExpect(status().isUnauthorized());
    }
}
