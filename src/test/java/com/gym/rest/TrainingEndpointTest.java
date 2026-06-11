package com.gym.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gym.exception.AuthenticationException;
import com.gym.exception.GlobalExceptionHandler;
import com.gym.exception.ValidationException;
import com.gym.facade.GymFacade;
import com.gym.model.Trainee;
import com.gym.model.Trainer;
import com.gym.model.Training;
import com.gym.model.TrainingType;
import com.gym.rest.dto.training.TrainingRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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
@Import(GlobalExceptionHandler.class)
@DisplayName("TrainingEndpoint — Full Test Suite")
class TrainingEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GymFacade gymFacade;

    private static final String TRAININGS_URL      = "/api/trainings";
    private static final String TRAINING_TYPES_URL = "/api/training-types";

    private static final String HEADER_USERNAME = "username";
    private static final String HEADER_PASSWORD = "password";

    private static final String AUTH_USER         = "john.doe";
    private static final String AUTH_PASS         = "secret123";
    private static final String TRAINEE_USERNAME  = "jane.smith";
    private static final String TRAINER_USERNAME  = "bob.trainer";
    private static final String TRAINING_NAME     = "Morning Cardio";
    private static final LocalDate TRAINING_DATE  = LocalDate.of(2024, 6, 15);
    private static final int     TRAINING_DURATION = 60;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
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
        when(gymFacade.selectTrainee(AUTH_USER, AUTH_PASS, TRAINEE_USERNAME))
                .thenReturn(Optional.of(buildTrainee()));
        when(gymFacade.selectTrainer(AUTH_USER, AUTH_PASS, TRAINER_USERNAME))
                .thenReturn(Optional.of(buildTrainer(type)));
        when(gymFacade.createTraining(eq(AUTH_USER), eq(AUTH_PASS), any(Training.class)))
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
                            .header(HEADER_USERNAME, AUTH_USER)
                            .header(HEADER_PASSWORD, AUTH_PASS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isOk())
                    .andExpect(content().string(""));

            verify(gymFacade).selectTrainee(AUTH_USER, AUTH_PASS, TRAINEE_USERNAME);
            verify(gymFacade).selectTrainer(AUTH_USER, AUTH_PASS, TRAINER_USERNAME);
            verify(gymFacade).createTraining(eq(AUTH_USER), eq(AUTH_PASS), any(Training.class));
        }

        @Test
        @DisplayName("Valid request with STRENGTH trainer → 200 OK")
        void validRequest_strengthTrainer_returns200() throws Exception {
            stubSuccessfulLookups(TrainingType.STRENGTH);

            mockMvc.perform(post(TRAININGS_URL)
                            .header(HEADER_USERNAME, AUTH_USER)
                            .header(HEADER_PASSWORD, AUTH_PASS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Training type is derived from trainer's specialization, not request body")
        void trainingType_setFromTrainerSpecialization() throws Exception {
            stubSuccessfulLookups(TrainingType.YOGA);

            mockMvc.perform(post(TRAININGS_URL)
                            .header(HEADER_USERNAME, AUTH_USER)
                            .header(HEADER_PASSWORD, AUTH_PASS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isOk());

            verify(gymFacade).createTraining(eq(AUTH_USER), eq(AUTH_PASS),
                    argThat(tr -> TrainingType.YOGA.equals(tr.getTrainingType())));
        }

        @Test
        @DisplayName("Training fields mapped correctly from request DTO to domain object")
        void trainingFields_mappedCorrectlyFromDto() throws Exception {
            stubSuccessfulLookups(TrainingType.CARDIO);

            mockMvc.perform(post(TRAININGS_URL)
                            .header(HEADER_USERNAME, AUTH_USER)
                            .header(HEADER_PASSWORD, AUTH_PASS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isOk());

            verify(gymFacade).createTraining(eq(AUTH_USER), eq(AUTH_PASS), argThat(tr ->
                    TRAINING_NAME.equals(tr.getTrainingName())
                            && TRAINING_DATE.equals(tr.getTrainingDate())
                            && TRAINING_DURATION == tr.getTrainingDuration()
            ));
        }

        @Test
        @DisplayName("Request without auth headers (null) still processes when facade permits it")
        void noAuthHeaders_facadePermits_returns200() throws Exception {
            when(gymFacade.selectTrainee(null, null, TRAINEE_USERNAME))
                    .thenReturn(Optional.of(buildTrainee()));
            when(gymFacade.selectTrainer(null, null, TRAINER_USERNAME))
                    .thenReturn(Optional.of(buildTrainer(TrainingType.CARDIO)));
            when(gymFacade.createTraining(isNull(), isNull(), any(Training.class)))
                    .thenReturn(new Training());

            mockMvc.perform(post(TRAININGS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isOk());

            verify(gymFacade).selectTrainee(null, null, TRAINEE_USERNAME);
            verify(gymFacade).selectTrainer(null, null, TRAINER_USERNAME);
        }

        @Test
        @DisplayName("createTraining is called exactly once on success")
        void createTraining_calledExactlyOnce() throws Exception {
            stubSuccessfulLookups(TrainingType.CARDIO);

            mockMvc.perform(post(TRAININGS_URL)
                            .header(HEADER_USERNAME, AUTH_USER)
                            .header(HEADER_PASSWORD, AUTH_PASS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isOk());

            verify(gymFacade, times(1)).createTraining(anyString(), anyString(), any(Training.class));
        }

        @Test
        @DisplayName("Missing traineeUsername → 400 with field error message")
        void missingTraineeUsername_returns400() throws Exception {
            TrainingRequestDto dto = buildValidRequest();
            dto.setTraineeUsername(null);

            mockMvc.perform(post(TRAININGS_URL)
                            .header(HEADER_USERNAME, AUTH_USER)
                            .header(HEADER_PASSWORD, AUTH_PASS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.traineeUsername")
                            .value("Trainee username is required"));

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("Blank traineeUsername → 400 with field error message")
        void blankTraineeUsername_returns400() throws Exception {
            TrainingRequestDto dto = buildValidRequest();
            dto.setTraineeUsername("   ");

            mockMvc.perform(post(TRAININGS_URL)
                            .header(HEADER_USERNAME, AUTH_USER)
                            .header(HEADER_PASSWORD, AUTH_PASS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.traineeUsername").exists());

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("Missing trainerUsername → 400 with field error message")
        void missingTrainerUsername_returns400() throws Exception {
            TrainingRequestDto dto = buildValidRequest();
            dto.setTrainerUsername(null);

            mockMvc.perform(post(TRAININGS_URL)
                            .header(HEADER_USERNAME, AUTH_USER)
                            .header(HEADER_PASSWORD, AUTH_PASS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.trainerUsername")
                            .value("Trainer username is required"));

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("Missing trainingName → 400 with field error message")
        void missingTrainingName_returns400() throws Exception {
            TrainingRequestDto dto = buildValidRequest();
            dto.setTrainingName(null);

            mockMvc.perform(post(TRAININGS_URL)
                            .header(HEADER_USERNAME, AUTH_USER)
                            .header(HEADER_PASSWORD, AUTH_PASS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.trainingName")
                            .value("Training name is required"));

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("Missing trainingDate → 400 with field error message")
        void missingTrainingDate_returns400() throws Exception {
            TrainingRequestDto dto = buildValidRequest();
            dto.setTrainingDate(null);

            mockMvc.perform(post(TRAININGS_URL)
                            .header(HEADER_USERNAME, AUTH_USER)
                            .header(HEADER_PASSWORD, AUTH_PASS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.trainingDate")
                            .value("Training date is required"));

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("Zero trainingDuration → 400 (@Positive rejects 0)")
        void zeroDuration_returns400() throws Exception {
            TrainingRequestDto dto = buildValidRequest();
            dto.setTrainingDuration(0);

            mockMvc.perform(post(TRAININGS_URL)
                            .header(HEADER_USERNAME, AUTH_USER)
                            .header(HEADER_PASSWORD, AUTH_PASS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.trainingDuration")
                            .value("Training duration must be a positive number"));

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("Negative trainingDuration → 400")
        void negativeDuration_returns400() throws Exception {
            TrainingRequestDto dto = buildValidRequest();
            dto.setTrainingDuration(-30);

            mockMvc.perform(post(TRAININGS_URL)
                            .header(HEADER_USERNAME, AUTH_USER)
                            .header(HEADER_PASSWORD, AUTH_PASS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.trainingDuration").exists());

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("All fields missing → 400 with errors for each @NotBlank/@NotNull field")
        void allFieldsMissing_returns400WithMultipleErrors() throws Exception {
            mockMvc.perform(post(TRAININGS_URL)
                            .header(HEADER_USERNAME, AUTH_USER)
                            .header(HEADER_PASSWORD, AUTH_PASS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.traineeUsername").exists())
                    .andExpect(jsonPath("$.trainerUsername").exists())
                    .andExpect(jsonPath("$.trainingName").exists())
                    .andExpect(jsonPath("$.trainingDate").exists());

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("Malformed JSON body → 400 with 'Malformed JSON request' message")
        void malformedJson_returns400() throws Exception {
            mockMvc.perform(post(TRAININGS_URL)
                            .header(HEADER_USERNAME, AUTH_USER)
                            .header(HEADER_PASSWORD, AUTH_PASS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ not-valid-json"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Malformed JSON request"));

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("Invalid date format for trainingDate → 400")
        void invalidDateFormat_returns400() throws Exception {
            String payload = """
                    {
                        "traineeUsername": "jane.smith",
                        "trainerUsername": "bob.trainer",
                        "trainingName": "Run",
                        "trainingDate": "not-a-date",
                        "trainingDuration": 60
                    }
                    """;

            mockMvc.perform(post(TRAININGS_URL)
                            .header(HEADER_USERNAME, AUTH_USER)
                            .header(HEADER_PASSWORD, AUTH_PASS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("Trainee not found in facade → 400 with 'Trainee not found' error detail")
        void traineeNotFound_returns400() throws Exception {
            when(gymFacade.selectTrainee(AUTH_USER, AUTH_PASS, TRAINEE_USERNAME))
                    .thenReturn(Optional.empty());

            mockMvc.perform(post(TRAININGS_URL)
                            .header(HEADER_USERNAME, AUTH_USER)
                            .header(HEADER_PASSWORD, AUTH_PASS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation error"))
                    .andExpect(jsonPath("$.details").value(containsString("Trainee not found")));

            verify(gymFacade, never()).selectTrainer(anyString(), anyString(), anyString());
            verify(gymFacade, never()).createTraining(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Trainer not found in facade → 400 with 'Trainer not found' error detail")
        void trainerNotFound_returns400() throws Exception {
            when(gymFacade.selectTrainee(AUTH_USER, AUTH_PASS, TRAINEE_USERNAME))
                    .thenReturn(Optional.of(buildTrainee()));
            when(gymFacade.selectTrainer(AUTH_USER, AUTH_PASS, TRAINER_USERNAME))
                    .thenReturn(Optional.empty());

            mockMvc.perform(post(TRAININGS_URL)
                            .header(HEADER_USERNAME, AUTH_USER)
                            .header(HEADER_PASSWORD, AUTH_PASS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation error"))
                    .andExpect(jsonPath("$.details").value(containsString("Trainer not found")));

            verify(gymFacade, never()).createTraining(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("ValidationException from facade includes the problematic username in message")
        void traineeNotFound_messageContainsUsername() throws Exception {
            when(gymFacade.selectTrainee(AUTH_USER, AUTH_PASS, TRAINEE_USERNAME))
                    .thenReturn(Optional.empty());

            mockMvc.perform(post(TRAININGS_URL)
                            .header(HEADER_USERNAME, AUTH_USER)
                            .header(HEADER_PASSWORD, AUTH_PASS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation error"))
                    .andExpect(jsonPath("$.details").value(containsString(TRAINEE_USERNAME)));
        }

        @Test
        @DisplayName("selectTrainee throws AuthenticationException → 401 with message body")
        void traineeSelectAuthFails_returns401() throws Exception {
            when(gymFacade.selectTrainee(AUTH_USER, "wrongPass", TRAINEE_USERNAME))
                    .thenThrow(new AuthenticationException("Invalid credentials"));

            TrainingRequestDto dto = buildValidRequest();

            mockMvc.perform(post(TRAININGS_URL)
                            .header(HEADER_USERNAME, AUTH_USER)
                            .header(HEADER_PASSWORD, "wrongPass")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().string("Invalid credentials"));

            verify(gymFacade, never()).createTraining(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("selectTrainer throws AuthenticationException → 401")
        void trainerSelectAuthFails_returns401() throws Exception {
            when(gymFacade.selectTrainee(AUTH_USER, AUTH_PASS, TRAINEE_USERNAME))
                    .thenReturn(Optional.of(buildTrainee()));
            when(gymFacade.selectTrainer(AUTH_USER, AUTH_PASS, TRAINER_USERNAME))
                    .thenThrow(new AuthenticationException("Unauthorized"));

            mockMvc.perform(post(TRAININGS_URL)
                            .header(HEADER_USERNAME, AUTH_USER)
                            .header(HEADER_PASSWORD, AUTH_PASS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isUnauthorized());

            verify(gymFacade, never()).createTraining(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("createTraining throws AuthenticationException → 401")
        void createTrainingAuthFails_returns401() throws Exception {
            when(gymFacade.selectTrainee(AUTH_USER, AUTH_PASS, TRAINEE_USERNAME))
                    .thenReturn(Optional.of(buildTrainee()));
            when(gymFacade.selectTrainer(AUTH_USER, AUTH_PASS, TRAINER_USERNAME))
                    .thenReturn(Optional.of(buildTrainer(TrainingType.CARDIO)));
            when(gymFacade.createTraining(eq(AUTH_USER), eq(AUTH_PASS), any(Training.class)))
                    .thenThrow(new AuthenticationException("Forbidden"));

            mockMvc.perform(post(TRAININGS_URL)
                            .header(HEADER_USERNAME, AUTH_USER)
                            .header(HEADER_PASSWORD, AUTH_PASS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().string("Forbidden"));
        }

        @Test
        @DisplayName("Unexpected RuntimeException from facade → 500 with error body")
        void unexpectedFacadeException_returns500() throws Exception {
            when(gymFacade.selectTrainee(AUTH_USER, AUTH_PASS, TRAINEE_USERNAME))
                    .thenThrow(new RuntimeException("Database unavailable"));

            mockMvc.perform(post(TRAININGS_URL)
                            .header(HEADER_USERNAME, AUTH_USER)
                            .header(HEADER_PASSWORD, AUTH_PASS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value("Internal Server Error"))
                    .andExpect(jsonPath("$.details").value("An internal error occurred"));
        }

        @Test
        @DisplayName("GET /api/trainings → 405 Method Not Allowed")
        void getOnTrainingsEndpoint_returns405() throws Exception {
            mockMvc.perform(get(TRAININGS_URL))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.message").value("Method Not Allowed"))
                    .andExpect(jsonPath("$.details").value("The requested HTTP method is not supported"));

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("DELETE /api/trainings → 405 Method Not Allowed")
        void deleteOnTrainingsEndpoint_returns405() throws Exception {
            mockMvc.perform(delete(TRAININGS_URL))
                    .andExpect(status().isMethodNotAllowed());

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("Missing Content-Type header → 415 Unsupported Media Type")
        void missingContentType_returns415() throws Exception {
            mockMvc.perform(post(TRAININGS_URL)
                            .header(HEADER_USERNAME, AUTH_USER)
                            .header(HEADER_PASSWORD, AUTH_PASS)
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
        @DisplayName("Each entry has a positive trainingTypeId (ordinal + 1)")
        void eachEntry_hasPositiveTrainingTypeId() throws Exception {
            mockMvc.perform(get(TRAINING_TYPES_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].trainingTypeId", everyItem(greaterThan(0))));
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
        @DisplayName("STRENGTH is present in the response list")
        void strengthType_isPresentInList() throws Exception {
            mockMvc.perform(get(TRAINING_TYPES_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].trainingType", hasItem("STRENGTH")));
        }

        @Test
        @DisplayName("Response content-type is application/json")
        void responseContentType_isJson() throws Exception {
            mockMvc.perform(get(TRAINING_TYPES_URL))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("trainingType values match TrainingType enum names exactly")
        void trainingTypeValues_matchEnumNames() throws Exception {
            String[] enumNames = java.util.Arrays.stream(TrainingType.values())
                    .map(Enum::name)
                    .toArray(String[]::new);

            mockMvc.perform(get(TRAINING_TYPES_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].trainingType",
                            containsInAnyOrder(enumNames)));
        }

        @Test
        @DisplayName("No trainingTypeId is duplicated in the response")
        void trainingTypeIds_areUnique() throws Exception {
            mockMvc.perform(get(TRAINING_TYPES_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].trainingTypeId",
                            hasSize(TrainingType.values().length)));
        }

        @Test
        @DisplayName("POST /api/training-types → 405 Method Not Allowed")
        void postOnTrainingTypes_returns405() throws Exception {
            mockMvc.perform(post(TRAINING_TYPES_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.message").value("Method Not Allowed"));

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("Auth headers are not required for training-types endpoint")
        void noAuthHeaders_trainingTypes_returns200() throws Exception {
            mockMvc.perform(get(TRAINING_TYPES_URL))
                    .andExpect(status().isOk());

            verifyNoInteractions(gymFacade);
        }
    }

    @Nested
    @DisplayName("Unknown Routes — 404 Not Found")
    class UnknownRoutes {

        @Test
        @DisplayName("POST /api/training → 404 (typo, missing trailing 's')")
        void typoInPath_returns404() throws Exception {
            mockMvc.perform(post("/api/training")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isNotFound());

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("GET /api/training-type → 404 (typo, missing trailing 's')")
        void typoInTrainingTypesPath_returns404() throws Exception {
            mockMvc.perform(get("/api/training-type"))
                    .andExpect(status().isNotFound());

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("GET /trainings (missing /api prefix) → 404")
        void missingApiPrefix_returns404() throws Exception {
            mockMvc.perform(get("/trainings"))
                    .andExpect(status().isNotFound());

            verifyNoInteractions(gymFacade);
        }
    }
}