package com.gym.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gym.config.TestSecurityConfig;
import com.gym.security.JwtAuthenticationFilter;
import com.gym.exception.AuthenticationException;
import com.gym.exception.GlobalExceptionHandler;
import com.gym.exception.ValidationException;
import com.gym.facade.GymFacade;
import com.gym.model.Trainee;
import com.gym.model.Trainer;
import com.gym.model.Training;
import com.gym.model.TrainingType;
import com.gym.rest.dto.training.TrainingRequestDto;
import com.gym.security.GymUserDetailsService;
import com.gym.security.JwtService;
import com.gym.security.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TrainingEndpoint.class)
@Import({JwtAuthenticationFilter.class, GlobalExceptionHandler.class, TestSecurityConfig.class})
@DisplayName("TrainingEndpoint — Full Test Suite")
class TrainingEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GymFacade gymFacade;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private GymUserDetailsService userDetailsService;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    private static final String TRAININGS_URL      = "/api/trainings";
    private static final String TRAINING_TYPES_URL = "/api/training-types";

    private static final String HEADER_USERNAME = "username";
    private static final String HEADER_PASSWORD = "password";

    private static final String AUTH_USER         = "john.doe";
    private static final String TRAINEE_USERNAME  = "jane.smith";
    private static final String TRAINER_USERNAME  = "bob.trainer";
    private static final String TRAINING_NAME     = "Morning Cardio";
    private static final LocalDate TRAINING_DATE  = LocalDate.of(2024, 6, 15);
    private static final int     TRAINING_DURATION = 60;
    private static final String BEARER_TOKEN      = "test-bearer-token";

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        UserDetails userDetails = User.withUsername(AUTH_USER).password("").roles("USER").build();
        lenient().when(jwtService.isValid(BEARER_TOKEN)).thenReturn(true);
        lenient().when(tokenBlacklistService.isBlacklisted(BEARER_TOKEN)).thenReturn(false);
        lenient().when(jwtService.extractUsername(BEARER_TOKEN)).thenReturn(AUTH_USER);
        lenient().when(userDetailsService.loadUserByUsername(AUTH_USER)).thenReturn(userDetails);
    }

    private String bearerToken() {
        return "Bearer " + BEARER_TOKEN;
    }

    private Trainee buildTrainee() {
        Trainee t = new Trainee();
        t.setUsername(TRAINEE_USERNAME);
        t.setFirstName("Jane");
        t.setLastName("Smith");
        t.setActive(true);
        return t;
    }

    private Trainer buildTrainer(TrainingType specialization) {
        Trainer t = new Trainer();
        t.setUsername(TRAINER_USERNAME);
        t.setFirstName("Bob");
        t.setLastName("Trainer");
        t.setSpecialization(specialization);
        return t;
    }

    private TrainingRequestDto buildValidRequest() {
        TrainingRequestDto dto = new TrainingRequestDto();
        dto.setTraineeUsername(TRAINEE_USERNAME);
        dto.setTrainerUsername(TRAINER_USERNAME);
        dto.setTrainingName(TRAINING_NAME);
        dto.setTrainingDate(TRAINING_DATE);
        dto.setTrainingDuration(TRAINING_DURATION);
        return dto;
    }

    private void stubSuccessfulLookups(TrainingType type) {
        when(gymFacade.findTraineeByUsername(TRAINEE_USERNAME))
                .thenReturn(Optional.of(buildTrainee()));
        when(gymFacade.findTrainerByUsername(TRAINER_USERNAME))
                .thenReturn(Optional.of(buildTrainer(type)));
        when(gymFacade.createTraining(anyString(), anyString(), any(Training.class)))
                .thenReturn(new Training());
    }

    @Nested
    @DisplayName("POST /api/trainings — Add Training")
    class AddTraining {

        @Test
        @DisplayName("Valid request with CARDIO trainer → 200 OK, empty body")
        void validRequest_cardioTrainer_returns200() throws Exception {
            stubSuccessfulLookups(TrainingType.CARDIO);

            mockMvc.perform(post(TRAININGS_URL)
                            .header(HttpHeaders.AUTHORIZATION, bearerToken())
                            .header(HEADER_USERNAME, AUTH_USER)
                            .header(HEADER_PASSWORD, "secret123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isOk())
                    .andExpect(content().string(""));

            verify(gymFacade).createTraining(anyString(), anyString(), any(Training.class));
        }

        @Test
        @DisplayName("Training type is derived from trainer's specialization")
        void trainingType_setFromTrainerSpecialization() throws Exception {
            stubSuccessfulLookups(TrainingType.YOGA);

            mockMvc.perform(post(TRAININGS_URL)
                            .header(HttpHeaders.AUTHORIZATION, bearerToken())
                            .header(HEADER_USERNAME, AUTH_USER)
                            .header(HEADER_PASSWORD, "secret123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isOk());

            verify(gymFacade).createTraining(anyString(), anyString(),
                    argThat(tr -> TrainingType.YOGA.equals(tr.getTrainingType())));
        }

        @Test
        @DisplayName("Missing traineeUsername → 400")
        void missingTraineeUsername_returns400() throws Exception {
            TrainingRequestDto dto = buildValidRequest();
            dto.setTraineeUsername(null);

            mockMvc.perform(post(TRAININGS_URL)
                            .header(HttpHeaders.AUTHORIZATION, bearerToken())
                            .header(HEADER_USERNAME, AUTH_USER)
                            .header(HEADER_PASSWORD, "secret123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.traineeUsername").value("Trainee username is required"));

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("Missing trainerUsername → 400")
        void missingTrainerUsername_returns400() throws Exception {
            TrainingRequestDto dto = buildValidRequest();
            dto.setTrainerUsername(null);

            mockMvc.perform(post(TRAININGS_URL)
                            .header(HttpHeaders.AUTHORIZATION, bearerToken())
                            .header(HEADER_USERNAME, AUTH_USER)
                            .header(HEADER_PASSWORD, "secret123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.trainerUsername").value("Trainer username is required"));

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("Missing trainingName → 400")
        void missingTrainingName_returns400() throws Exception {
            TrainingRequestDto dto = buildValidRequest();
            dto.setTrainingName(null);

            mockMvc.perform(post(TRAININGS_URL)
                            .header(HttpHeaders.AUTHORIZATION, bearerToken())
                            .header(HEADER_USERNAME, AUTH_USER)
                            .header(HEADER_PASSWORD, "secret123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.trainingName").value("Training name is required"));

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("Zero trainingDuration → 400")
        void zeroDuration_returns400() throws Exception {
            TrainingRequestDto dto = buildValidRequest();
            dto.setTrainingDuration(0);

            mockMvc.perform(post(TRAININGS_URL)
                            .header(HttpHeaders.AUTHORIZATION, bearerToken())
                            .header(HEADER_USERNAME, AUTH_USER)
                            .header(HEADER_PASSWORD, "secret123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.trainingDuration")
                            .value("Training duration must be a positive number"));

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("Malformed JSON body → 400")
        void malformedJson_returns400() throws Exception {
            mockMvc.perform(post(TRAININGS_URL)
                            .header(HttpHeaders.AUTHORIZATION, bearerToken())
                            .header(HEADER_USERNAME, AUTH_USER)
                            .header(HEADER_PASSWORD, "secret123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ not-valid-json"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Malformed JSON request"));

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("Trainee not found → 400 with error detail")
        void traineeNotFound_returns400() throws Exception {
            when(gymFacade.findTraineeByUsername(TRAINEE_USERNAME))
                    .thenReturn(Optional.empty());

            mockMvc.perform(post(TRAININGS_URL)
                            .header(HttpHeaders.AUTHORIZATION, bearerToken())
                            .header(HEADER_USERNAME, AUTH_USER)
                            .header(HEADER_PASSWORD, "secret123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation error"))
                    .andExpect(jsonPath("$.details").value(containsString("Trainee not found")));

            verify(gymFacade, never()).createTraining(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Trainer not found → 400 with error detail")
        void trainerNotFound_returns400() throws Exception {
            when(gymFacade.findTraineeByUsername(TRAINEE_USERNAME))
                    .thenReturn(Optional.of(buildTrainee()));
            when(gymFacade.findTrainerByUsername(TRAINER_USERNAME))
                    .thenReturn(Optional.empty());

            mockMvc.perform(post(TRAININGS_URL)
                            .header(HttpHeaders.AUTHORIZATION, bearerToken())
                            .header(HEADER_USERNAME, AUTH_USER)
                            .header(HEADER_PASSWORD, "secret123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details").value(containsString("Trainer not found")));

            verify(gymFacade, never()).createTraining(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("createTraining throws AuthenticationException → 401")
        void createTrainingAuthFails_returns401() throws Exception {
            when(gymFacade.findTraineeByUsername(TRAINEE_USERNAME))
                    .thenReturn(Optional.of(buildTrainee()));
            when(gymFacade.findTrainerByUsername(TRAINER_USERNAME))
                    .thenReturn(Optional.of(buildTrainer(TrainingType.CARDIO)));
            when(gymFacade.createTraining(anyString(), eq("wrongPass"), any(Training.class)))
                    .thenThrow(new AuthenticationException("Invalid credentials"));

            mockMvc.perform(post(TRAININGS_URL)
                            .header(HttpHeaders.AUTHORIZATION, bearerToken())
                            .header(HEADER_USERNAME, AUTH_USER)
                            .header(HEADER_PASSWORD, "wrongPass")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().string("Invalid credentials"));
        }

        @Test
        @DisplayName("Unexpected RuntimeException from facade → 500")
        void unexpectedFacadeException_returns500() throws Exception {
            when(gymFacade.findTraineeByUsername(anyString()))
                    .thenThrow(new RuntimeException("Database unavailable"));

            mockMvc.perform(post(TRAININGS_URL)
                            .header(HttpHeaders.AUTHORIZATION, bearerToken())
                            .header(HEADER_USERNAME, AUTH_USER)
                            .header(HEADER_PASSWORD, "secret123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value("Internal Server Error"));
        }

        @Test
        @DisplayName("GET /api/trainings (no auth) → 401")
        void getOnTrainingsEndpoint_returns401FromFilter() throws Exception {
            mockMvc.perform(get(TRAININGS_URL))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("Missing Content-Type header → 415")
        void missingContentType_returns415() throws Exception {
            mockMvc.perform(post(TRAININGS_URL)
                            .header(HttpHeaders.AUTHORIZATION, bearerToken())
                            .header(HEADER_USERNAME, AUTH_USER)
                            .header(HEADER_PASSWORD, "secret123")
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isUnsupportedMediaType());

            verifyNoInteractions(gymFacade);
        }
    }

    @Nested
    @DisplayName("GET /api/training-types — Get Training Types")
    class GetTrainingTypes {

        @Test
        @DisplayName("Returns 200 OK with a list of all TrainingType enum values")
        void returns200WithAllTypes() throws Exception {
            int expectedCount = TrainingType.values().length;

            mockMvc.perform(get(TRAINING_TYPES_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(expectedCount)));

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("Each entry has non-null trainingType string")
        void eachEntry_hasNonNullTrainingType() throws Exception {
            mockMvc.perform(get(TRAINING_TYPES_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].trainingType", everyItem(notNullValue())));
        }

        @Test
        @DisplayName("TrainingTypeId values are sequential starting from 1")
        void trainingTypeIds_areSequentialFromOne() throws Exception {
            mockMvc.perform(get(TRAINING_TYPES_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].trainingTypeId").value(1));
        }

        @Test
        @DisplayName("CARDIO is present in the response list")
        void cardioType_isPresentInList() throws Exception {
            mockMvc.perform(get(TRAINING_TYPES_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].trainingType", hasItem("CARDIO")));
        }

        @Test
        @DisplayName("GET /api/training-types requires no auth")
        void noAuthHeaders_trainingTypes_returns200() throws Exception {
            mockMvc.perform(get(TRAINING_TYPES_URL))
                    .andExpect(status().isOk());

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("POST /api/training-types (wrong method, no auth) → 401")
        void postOnTrainingTypes_returns401FromFilter() throws Exception {
            mockMvc.perform(post(TRAINING_TYPES_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(gymFacade);
        }
    }

    @Nested
    @DisplayName("Unknown Routes")
    class UnknownRoutes {

        @Test
        @DisplayName("POST /api/training (typo, no auth) → 401")
        void typoInPath_returns401FromFilter() throws Exception {
            mockMvc.perform(post("/api/training")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("GET /trainings - missing /api prefix, no auth → 401")
        void missingApiPrefix_returns401FromFilter() throws Exception {
            mockMvc.perform(get("/trainings"))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(gymFacade);
        }
    }
}
