package com.gym.workload.rest;

import com.gym.workload.config.TestSecurityConfig;
import com.gym.workload.model.TrainerSummary;
import com.gym.workload.security.JwtAuthenticationFilter;
import com.gym.workload.security.JwtService;
import com.gym.workload.security.WorkloadUserDetailsService;
import com.gym.workload.store.TrainerSummaryStore;
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

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

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
    private TrainerSummaryStore store;

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
        TrainerSummary summary = new TrainerSummary();
        summary.setUsername("trainer.jane");
        summary.setFirstName("Jane");
        summary.setLastName("Doe");
        summary.setActive(true);
        summary.setYears(new ConcurrentHashMap<>(Map.of(2026, new ConcurrentHashMap<>(Map.of(7, 60L)))));
        when(store.find("trainer.jane")).thenReturn(Optional.of(summary));

        mockMvc.perform(get("/api/trainer-workload/{username}", "trainer.jane")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("trainer.jane"))
                .andExpect(jsonPath("$.years.2026.7").value(60));
    }

    @Test
    @DisplayName("GET 404 Not Found – unknown trainer")
    void get_unknownTrainer_returns404() throws Exception {
        when(store.find("nobody")).thenReturn(Optional.empty());

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
