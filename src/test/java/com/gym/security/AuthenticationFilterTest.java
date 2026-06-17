package com.gym.security;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.gym.dao.TraineeDao;
import com.gym.dao.TrainerDao;
import com.gym.facade.GymFacade;
import com.gym.model.Trainee;
import com.gym.model.Trainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("AuthenticationFilter Integration Tests")
class AuthenticationFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TraineeDao traineeDao;

    @MockBean
    private TrainerDao trainerDao;

    @MockBean
    private GymFacade gymFacade;

    private static final String TRAINEE_USERNAME = "john.doe";
    private static final String TRAINEE_PASSWORD = "secret123";
    private static final String TRAINER_USERNAME = "jane.smith";
    private static final String TRAINER_PASSWORD = "trainerPass1";

    private ListAppender<ILoggingEvent> logAppender;
    private Logger filterLogger;

    @BeforeEach
    void attachLogAppender() {
        filterLogger = (Logger) LoggerFactory.getLogger(AuthenticationFilter.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        filterLogger.addAppender(logAppender);
    }

    @AfterEach
    void detachLogAppender() {
        filterLogger.detachAppender(logAppender);
    }

    private String basicAuthHeader(String username, String password) {
        String credentials = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private Trainee trainee(String username, String password) {
        Trainee trainee = new Trainee();
        trainee.setUsername(username);
        trainee.setPassword(password);
        return trainee;
    }

    private Trainer trainer(String username, String password) {
        Trainer trainer = new Trainer();
        trainer.setUsername(username);
        trainer.setPassword(password);
        return trainer;
    }

    @Nested
    @DisplayName("Protected endpoints reject unauthenticated requests with 401")
    class UnauthorizedAccess {

        @Test
        @DisplayName("No Authorization header → 401")
        void missingAuthorizationHeader_returns401() throws Exception {
            mockMvc.perform(get("/trainees/" + TRAINEE_USERNAME))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("Authorization header present but not 'Basic' scheme → 401")
        void nonBasicAuthorizationHeader_returns401() throws Exception {
            mockMvc.perform(get("/trainees/" + TRAINEE_USERNAME)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer some-token"))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("Malformed Base64 in Authorization header → 401")
        void malformedBase64_returns401() throws Exception {
            mockMvc.perform(get("/trainees/" + TRAINEE_USERNAME)
                            .header(HttpHeaders.AUTHORIZATION, "Basic %%%not-base64%%%"))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("Decoded credentials missing ':' separator → 401")
        void missingColonSeparator_returns401() throws Exception {
            String noColon = Base64.getEncoder().encodeToString("justusername".getBytes(StandardCharsets.UTF_8));

            mockMvc.perform(get("/trainees/" + TRAINEE_USERNAME)
                            .header(HttpHeaders.AUTHORIZATION, "Basic " + noColon))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("Unknown username (not a trainee or trainer) → 401")
        void unknownUsername_returns401() throws Exception {
            when(traineeDao.findById(anyString())).thenReturn(Optional.empty());
            when(trainerDao.findById(anyString())).thenReturn(Optional.empty());

            mockMvc.perform(get("/trainees/ghost.user")
                            .header(HttpHeaders.AUTHORIZATION, basicAuthHeader("ghost.user", "anyPassword")))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Correct username but wrong password (trainee) → 401")
        void wrongPasswordForTrainee_returns401() throws Exception {
            when(traineeDao.findById(TRAINEE_USERNAME))
                    .thenReturn(Optional.of(trainee(TRAINEE_USERNAME, TRAINEE_PASSWORD)));
            when(trainerDao.findById(TRAINEE_USERNAME)).thenReturn(Optional.empty());

            mockMvc.perform(get("/trainees/" + TRAINEE_USERNAME)
                            .header(HttpHeaders.AUTHORIZATION, basicAuthHeader(TRAINEE_USERNAME, "wrongPassword")))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Correct username but wrong password (trainer) → 401")
        void wrongPasswordForTrainer_returns401() throws Exception {
            when(traineeDao.findById(TRAINER_USERNAME)).thenReturn(Optional.empty());
            when(trainerDao.findById(TRAINER_USERNAME))
                    .thenReturn(Optional.of(trainer(TRAINER_USERNAME, TRAINER_PASSWORD)));

            mockMvc.perform(get("/trainers/" + TRAINER_USERNAME)
                            .header(HttpHeaders.AUTHORIZATION, basicAuthHeader(TRAINER_USERNAME, "wrongPassword")))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("401 response includes WWW-Authenticate: Basic header")
        void unauthorizedResponse_includesWwwAuthenticateHeader() throws Exception {
            mockMvc.perform(get("/trainees/" + TRAINEE_USERNAME))
                    .andExpect(status().isUnauthorized())
                    .andExpect(result -> assertThat(
                            result.getResponse().getHeader(HttpHeaders.WWW_AUTHENTICATE))
                            .startsWith("Basic"));
        }
    }

    @Nested
    @DisplayName("Protected endpoints accept valid credentials and pass through the filter")
    class AuthorizedAccess {

        @Test
        @DisplayName("Valid trainee credentials → request passes filter (no 401)")
        void validTraineeCredentials_passesFilter() throws Exception {
            when(traineeDao.findById(TRAINEE_USERNAME))
                    .thenReturn(Optional.of(trainee(TRAINEE_USERNAME, TRAINEE_PASSWORD)));
            when(gymFacade.selectTrainee(TRAINEE_USERNAME, TRAINEE_PASSWORD, TRAINEE_USERNAME))
                    .thenReturn(Optional.of(trainee(TRAINEE_USERNAME, TRAINEE_PASSWORD)));

            mockMvc.perform(get("/trainees/" + TRAINEE_USERNAME)
                            .header(HttpHeaders.AUTHORIZATION, basicAuthHeader(TRAINEE_USERNAME, TRAINEE_PASSWORD)))
                    .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
        }

        @Test
        @DisplayName("Valid trainer credentials → request passes filter (no 401)")
        void validTrainerCredentials_passesFilter() throws Exception {
            when(traineeDao.findById(TRAINER_USERNAME)).thenReturn(Optional.empty());
            when(trainerDao.findById(TRAINER_USERNAME))
                    .thenReturn(Optional.of(trainer(TRAINER_USERNAME, TRAINER_PASSWORD)));
            when(gymFacade.selectTrainer(TRAINER_USERNAME, TRAINER_PASSWORD, TRAINER_USERNAME))
                    .thenReturn(Optional.of(trainer(TRAINER_USERNAME, TRAINER_PASSWORD)));

            mockMvc.perform(get("/trainers/" + TRAINER_USERNAME)
                            .header(HttpHeaders.AUTHORIZATION, basicAuthHeader(TRAINER_USERNAME, TRAINER_PASSWORD)))
                    .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
        }
    }

    @Nested
    @DisplayName("Registration and login endpoints are exempt from authentication")
    class PublicEndpoints {

        @Test
        @DisplayName("POST /trainees with no Authorization header → not 401")
        void registerTrainee_noAuthHeader_notUnauthorized() throws Exception {
            mockMvc.perform(post("/trainees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"firstName":"John","lastName":"Doe"}
                                    """))
                    .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401));

            verifyNoInteractions(traineeDao);
            verifyNoInteractions(trainerDao);
        }

        @Test
        @DisplayName("POST /trainers with no Authorization header → not 401")
        void registerTrainer_noAuthHeader_notUnauthorized() throws Exception {
            mockMvc.perform(post("/trainers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"firstName":"Jane","lastName":"Smith","specialization":"YOGA"}
                                    """))
                    .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401));

            verifyNoInteractions(traineeDao);
            verifyNoInteractions(trainerDao);
        }

        @Test
        @DisplayName("POST /auth/login with no Authorization header → not blocked by the filter")
        void login_noAuthHeader_notUnauthorized() throws Exception {
            when(gymFacade.traineeMatchCredentials("john.doe", "secret123")).thenReturn(true);

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"john.doe","password":"secret123"}
                                    """))
                    .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401));

            verifyNoInteractions(traineeDao);
            verifyNoInteractions(trainerDao);
        }

        @Test
        @DisplayName("GET on a non-exempt path is still protected even though /trainees POST is exempt")
        void getTrainees_isNotExempt_evenThoughPostIs() throws Exception {
            // Only POST /trainees is exempt (registration); GET /trainees/{username} must still
            // require authentication, proving the exemption is method+path specific, not path-only.
            mockMvc.perform(get("/trainees/" + TRAINEE_USERNAME))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Logging behavior")
    class LoggingBehavior {

        @Test
        @DisplayName("Failed authentication logs a warning that does not contain the attempted password")
        void failedAuth_logsWarningWithoutPassword() throws Exception {
            when(traineeDao.findById(anyString())).thenReturn(Optional.empty());
            when(trainerDao.findById(anyString())).thenReturn(Optional.empty());

            String attemptedPassword = "superSecretAttempt99";

            mockMvc.perform(get("/trainees/ghost.user")
                            .header(HttpHeaders.AUTHORIZATION, basicAuthHeader("ghost.user", attemptedPassword)))
                    .andExpect(status().isUnauthorized());

            List<ILoggingEvent> warnings = logAppender.list.stream()
                    .filter(event -> event.getLevel() == Level.WARN)
                    .toList();

            assertThat(warnings).isNotEmpty();
            assertThat(warnings)
                    .as("logged warning messages must never contain the raw password")
                    .noneMatch(event -> event.getFormattedMessage().contains(attemptedPassword));
            assertThat(warnings)
                    .anyMatch(event -> event.getFormattedMessage().contains("ghost.user"));
        }

        @Test
        @DisplayName("No log message at any level ever contains a password value")
        void noLogMessage_everContainsPassword() throws Exception {
            when(traineeDao.findById(TRAINEE_USERNAME))
                    .thenReturn(Optional.of(trainee(TRAINEE_USERNAME, TRAINEE_PASSWORD)));
            when(gymFacade.selectTrainee(TRAINEE_USERNAME, TRAINEE_PASSWORD, TRAINEE_USERNAME))
                    .thenReturn(Optional.of(trainee(TRAINEE_USERNAME, TRAINEE_PASSWORD)));

            mockMvc.perform(get("/trainees/" + TRAINEE_USERNAME)
                    .header(HttpHeaders.AUTHORIZATION, basicAuthHeader(TRAINEE_USERNAME, TRAINEE_PASSWORD)));

            assertThat(logAppender.list)
                    .as("no log line from the authentication filter should ever contain the raw password")
                    .noneMatch(event -> event.getFormattedMessage().contains(TRAINEE_PASSWORD));
        }

        @Test
        @DisplayName("Successful authentication is logged at debug level with the username")
        void successfulAuth_logsDebugWithUsername() throws Exception {
            filterLogger.setLevel(Level.DEBUG);

            when(traineeDao.findById(TRAINEE_USERNAME))
                    .thenReturn(Optional.of(trainee(TRAINEE_USERNAME, TRAINEE_PASSWORD)));
            when(gymFacade.selectTrainee(TRAINEE_USERNAME, TRAINEE_PASSWORD, TRAINEE_USERNAME))
                    .thenReturn(Optional.of(trainee(TRAINEE_USERNAME, TRAINEE_PASSWORD)));

            mockMvc.perform(get("/trainees/" + TRAINEE_USERNAME)
                    .header(HttpHeaders.AUTHORIZATION, basicAuthHeader(TRAINEE_USERNAME, TRAINEE_PASSWORD)));

            List<ILoggingEvent> debugLogs = logAppender.list.stream()
                    .filter(event -> event.getLevel() == Level.DEBUG)
                    .toList();

            assertThat(debugLogs)
                    .anyMatch(event -> event.getFormattedMessage().contains(TRAINEE_USERNAME)
                            && event.getFormattedMessage().toLowerCase().contains("succeeded"));
        }
    }
}