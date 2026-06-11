package com.gym.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.exception.AuthenticationException;
import com.gym.exception.GlobalExceptionHandler;
import com.gym.exception.ValidationException;
import com.gym.facade.GymFacade;
import com.gym.model.Trainee;
import com.gym.model.Trainer;
import com.gym.model.Training;
import com.gym.model.TrainingType;
import com.gym.rest.dto.trainer.TrainerRegistrationRequestDto;
import com.gym.rest.dto.trainer.TrainerStatusPatchDto;
import com.gym.rest.dto.trainer.TrainerUpdateRequestDto;
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
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TrainerEndpoint.class)
@Import(GlobalExceptionHandler.class)
class TrainerEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GymFacade gymFacade;

    private static final String AUTH_USER = "trainer.john";
    private static final String AUTH_PASS = "secret123";
    private static final String TARGET_USER = "trainer.jane";
    private static final String HEADER_USER = "X-Auth-Username";
    private static final String HEADER_PASS = "X-Auth-Password";

    private Trainer sampleTrainer;

    @BeforeEach
    void setUp() {
        sampleTrainer = new Trainer();
        sampleTrainer.setUsername(TARGET_USER);
        sampleTrainer.setFirstName("Jane");
        sampleTrainer.setLastName("Doe");
        sampleTrainer.setSpecialization(TrainingType.CARDIO);
        sampleTrainer.setActive(true);
    }

    @Nested
    @DisplayName("POST /trainers – registerTrainer")
    class RegisterTrainer {

        private TrainerRegistrationRequestDto validRequest() {
            TrainerRegistrationRequestDto dto = new TrainerRegistrationRequestDto();
            dto.setFirstName("Jane");
            dto.setLastName("Doe");
            dto.setSpecialization(TrainingType.CARDIO);
            return dto;
        }

        @Test
        @DisplayName("200 OK – returns generated username and password")
        void happyPath_returnsCredentials() throws Exception {
            sampleTrainer.setPassword("generatedPass");
            when(gymFacade.createTrainer(any(Trainer.class))).thenReturn(sampleTrainer);

            mockMvc.perform(post("/trainers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value(TARGET_USER))
                    .andExpect(jsonPath("$.password").value("generatedPass"));

            verify(gymFacade).createTrainer(any(Trainer.class));
        }

        @Test
        @DisplayName("400 Bad Request – missing firstName")
        void missingFirstName_returns400() throws Exception {
            TrainerRegistrationRequestDto dto = validRequest();
            dto.setFirstName("");

            mockMvc.perform(post("/trainers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.firstName").exists());

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("400 Bad Request – missing lastName")
        void missingLastName_returns400() throws Exception {
            TrainerRegistrationRequestDto dto = validRequest();
            dto.setLastName(null);

            mockMvc.perform(post("/trainers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.lastName").exists());
        }

        @Test
        @DisplayName("400 Bad Request – null specialization")
        void nullSpecialization_returns400() throws Exception {
            TrainerRegistrationRequestDto dto = validRequest();
            dto.setSpecialization(null);

            mockMvc.perform(post("/trainers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.specialization").exists());
        }

        @Test
        @DisplayName("400 Bad Request – malformed JSON body")
        void malformedJson_returns400() throws Exception {
            mockMvc.perform(post("/trainers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ not-valid-json"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("415 Unsupported Media Type – wrong content-type")
        void wrongContentType_returns415() throws Exception {
            mockMvc.perform(post("/trainers")
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("some text"))
                    .andExpect(status().isUnsupportedMediaType());
        }
    }

    @Nested
    @DisplayName("GET /trainers/{username} – getTrainerProfile")
    class GetTrainerProfile {

        @Test
        @DisplayName("200 OK – returns full trainer profile")
        void happyPath_returnsProfile() throws Exception {
            when(gymFacade.selectTrainer(AUTH_USER, AUTH_PASS, TARGET_USER))
                    .thenReturn(Optional.of(sampleTrainer));

            mockMvc.perform(get("/trainers/{username}", TARGET_USER)
                            .header(HEADER_USER, AUTH_USER)
                            .header(HEADER_PASS, AUTH_PASS))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("Jane"))
                    .andExpect(jsonPath("$.lastName").value("Doe"))
                    .andExpect(jsonPath("$.specialization").value("CARDIO"))
                    .andExpect(jsonPath("$.active").value(true))
                    .andExpect(jsonPath("$.trainees").isArray());

            verify(gymFacade).selectTrainer(AUTH_USER, AUTH_PASS, TARGET_USER);
        }

        @Test
        @DisplayName("401 Unauthorized – facade throws AuthenticationException")
        void badCredentials_returns401() throws Exception {
            when(gymFacade.selectTrainer(anyString(), anyString(), anyString()))
                    .thenThrow(new AuthenticationException("Invalid credentials"));

            mockMvc.perform(get("/trainers/{username}", TARGET_USER)
                            .header(HEADER_USER, "wrong")
                            .header(HEADER_PASS, "wrong"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("404 Not Found – trainer does not exist")
        void trainerNotFound_returns500WithRuntimeException() throws Exception {
            when(gymFacade.selectTrainer(AUTH_USER, AUTH_PASS, TARGET_USER))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/trainers/{username}", TARGET_USER)
                            .header(HEADER_USER, AUTH_USER)
                            .header(HEADER_PASS, AUTH_PASS))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("400 Bad Request – missing auth header")
        void missingAuthHeader_returns400() throws Exception {
            mockMvc.perform(get("/trainers/{username}", TARGET_USER)
                            .header(HEADER_USER, AUTH_USER))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Missing header"))
                    .andExpect(jsonPath("$.details").value("Required header: X-Auth-Password"));
        }
    }

    @Nested
    @DisplayName("PUT /trainers/{username} – updateTrainerProfile")
    class UpdateTrainerProfile {

        private TrainerUpdateRequestDto validUpdateRequest() {
            TrainerUpdateRequestDto dto = new TrainerUpdateRequestDto();
            dto.setFirstName("Jane");
            dto.setLastName("Smith");
            dto.setSpecialization(TrainingType.YOGA);
            dto.setIsActive(true);
            return dto;
        }

        @Test
        @DisplayName("200 OK – returns updated trainer profile")
        void happyPath_returnsUpdatedProfile() throws Exception {
            Trainer updated = new Trainer();
            updated.setFirstName("Jane");
            updated.setLastName("Smith");
            updated.setSpecialization(TrainingType.YOGA);
            updated.setActive(true);

            when(gymFacade.updateTrainer(eq(AUTH_USER), eq(AUTH_PASS), any(Trainer.class)))
                    .thenReturn(updated);

            mockMvc.perform(put("/trainers/{username}", TARGET_USER)
                            .header(HEADER_USER, AUTH_USER)
                            .header(HEADER_PASS, AUTH_PASS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUpdateRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("Jane"))
                    .andExpect(jsonPath("$.lastName").value("Smith"))
                    .andExpect(jsonPath("$.specialization").value("YOGA"))
                    .andExpect(jsonPath("$.active").value(true));

            verify(gymFacade).updateTrainer(eq(AUTH_USER), eq(AUTH_PASS), any(Trainer.class));
        }

        @Test
        @DisplayName("400 Bad Request – blank firstName in update body")
        void blankFirstName_returns400() throws Exception {
            TrainerUpdateRequestDto dto = validUpdateRequest();
            dto.setFirstName("   ");

            mockMvc.perform(put("/trainers/{username}", TARGET_USER)
                            .header(HEADER_USER, AUTH_USER)
                            .header(HEADER_PASS, AUTH_PASS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.firstName").exists());

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("400 Bad Request – null isActive in update body")
        void nullIsActive_returns400() throws Exception {
            TrainerUpdateRequestDto dto = validUpdateRequest();
            dto.setIsActive(null);

            mockMvc.perform(put("/trainers/{username}", TARGET_USER)
                            .header(HEADER_USER, AUTH_USER)
                            .header(HEADER_PASS, AUTH_PASS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isActive").exists());
        }

        @Test
        @DisplayName("401 Unauthorized – facade throws AuthenticationException")
        void badCredentials_returns401() throws Exception {
            when(gymFacade.updateTrainer(anyString(), anyString(), any(Trainer.class)))
                    .thenThrow(new AuthenticationException("Unauthorized"));

            mockMvc.perform(put("/trainers/{username}", TARGET_USER)
                            .header(HEADER_USER, "bad")
                            .header(HEADER_PASS, "creds")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUpdateRequest())))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("400 Bad Request – facade throws ValidationException")
        void facadeValidationError_returns400() throws Exception {
            when(gymFacade.updateTrainer(anyString(), anyString(), any(Trainer.class)))
                    .thenThrow(new ValidationException("Username does not match"));

            mockMvc.perform(put("/trainers/{username}", TARGET_USER)
                            .header(HEADER_USER, AUTH_USER)
                            .header(HEADER_PASS, AUTH_PASS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUpdateRequest())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details").value("Username does not match"));
        }
    }

    @Nested
    @DisplayName("PATCH /trainers/{username}/status – updateTrainerStatus")
    class UpdateTrainerStatus {

        private TrainerStatusPatchDto statusDto(boolean active) {
            TrainerStatusPatchDto dto = new TrainerStatusPatchDto();
            dto.setIsActive(active);
            return dto;
        }

        @Test
        @DisplayName("200 OK – activates trainer successfully")
        void activate_returns200() throws Exception {
            doNothing().when(gymFacade)
                    .setTrainerActive(AUTH_USER, AUTH_PASS, TARGET_USER, true);

            mockMvc.perform(patch("/trainers/{username}/status", TARGET_USER)
                            .header(HEADER_USER, AUTH_USER)
                            .header(HEADER_PASS, AUTH_PASS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(statusDto(true))))
                    .andExpect(status().isOk());

            verify(gymFacade).setTrainerActive(AUTH_USER, AUTH_PASS, TARGET_USER, true);
        }

        @Test
        @DisplayName("200 OK – deactivates trainer successfully")
        void deactivate_returns200() throws Exception {
            doNothing().when(gymFacade)
                    .setTrainerActive(AUTH_USER, AUTH_PASS, TARGET_USER, false);

            mockMvc.perform(patch("/trainers/{username}/status", TARGET_USER)
                            .header(HEADER_USER, AUTH_USER)
                            .header(HEADER_PASS, AUTH_PASS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(statusDto(false))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("400 Bad Request – null isActive")
        void nullIsActive_returns400() throws Exception {
            TrainerStatusPatchDto dto = new TrainerStatusPatchDto();

            mockMvc.perform(patch("/trainers/{username}/status", TARGET_USER)
                            .header(HEADER_USER, AUTH_USER)
                            .header(HEADER_PASS, AUTH_PASS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isActive").exists());

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("401 Unauthorized – facade throws AuthenticationException")
        void badCredentials_returns401() throws Exception {
            doThrow(new AuthenticationException("Bad credentials"))
                    .when(gymFacade).setTrainerActive(anyString(), anyString(), anyString(), anyBoolean());

            mockMvc.perform(patch("/trainers/{username}/status", TARGET_USER)
                            .header(HEADER_USER, "bad")
                            .header(HEADER_PASS, "creds")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(statusDto(true))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("400 Bad Request – missing X-Auth-Password header")
        void missingPasswordHeader_returns400() throws Exception {
            mockMvc.perform(patch("/trainers/{username}/status", TARGET_USER)
                            .header(HEADER_USER, AUTH_USER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(statusDto(true))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Missing header"))
                    .andExpect(jsonPath("$.details").value("Required header: X-Auth-Password"));
        }
    }

    @Nested
    @DisplayName("GET /trainers/{username}/trainings – getTrainerTrainings")
    class GetTrainerTrainings {

        private Training buildTraining(String name, String traineeUsername) {
            Training t = new Training();
            t.setTrainingName(name);
            t.setTrainingDate(LocalDate.of(2024, 6, 15));
            t.setTrainingType(TrainingType.CARDIO);
            t.setTrainingDuration(60);

            Trainee trainee = new Trainee();
            trainee.setUsername(traineeUsername);
            t.setTrainee(trainee);
            return t;
        }

        @Test
        @DisplayName("200 OK – returns list of trainings without filters")
        void noFilters_returnsAllTrainings() throws Exception {
            List<Training> trainings = List.of(
                    buildTraining("Morning Run", "trainee.alice"),
                    buildTraining("Interval Sprint", "trainee.bob")
            );
            when(gymFacade.getTrainerTrainings(AUTH_USER, AUTH_PASS, TARGET_USER, null, null, null))
                    .thenReturn(trainings);

            mockMvc.perform(get("/trainers/{username}/trainings", TARGET_USER)
                            .header(HEADER_USER, AUTH_USER)
                            .header(HEADER_PASS, AUTH_PASS))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].trainingName").value("Morning Run"))
                    .andExpect(jsonPath("$[0].trainerName").value("trainee.alice"))
                    .andExpect(jsonPath("$[1].trainingName").value("Interval Sprint"));
        }

        @Test
        @DisplayName("200 OK – returns filtered trainings with all query params")
        void withFilters_returnsFilteredTrainings() throws Exception {
            LocalDate from = LocalDate.of(2024, 1, 1);
            LocalDate to = LocalDate.of(2024, 12, 31);
            String traineeName = "trainee.alice";

            List<Training> trainings = List.of(buildTraining("Morning Run", traineeName));
            when(gymFacade.getTrainerTrainings(AUTH_USER, AUTH_PASS, TARGET_USER, from, to, traineeName))
                    .thenReturn(trainings);

            mockMvc.perform(get("/trainers/{username}/trainings", TARGET_USER)
                            .header(HEADER_USER, AUTH_USER)
                            .header(HEADER_PASS, AUTH_PASS)
                            .param("periodFrom", "2024-01-01")
                            .param("periodTo", "2024-12-31")
                            .param("traineeName", traineeName))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].trainingName").value("Morning Run"));

            verify(gymFacade).getTrainerTrainings(AUTH_USER, AUTH_PASS, TARGET_USER, from, to, traineeName);
        }

        @Test
        @DisplayName("200 OK – returns empty list when no trainings match")
        void noMatchingTrainings_returnsEmptyList() throws Exception {
            when(gymFacade.getTrainerTrainings(any(), any(), any(), any(), any(), any()))
                    .thenReturn(List.of());

            mockMvc.perform(get("/trainers/{username}/trainings", TARGET_USER)
                            .header(HEADER_USER, AUTH_USER)
                            .header(HEADER_PASS, AUTH_PASS))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("200 OK – training with null trainee does not set trainerName")
        void trainingWithNullTrainee_omitsTrainerName() throws Exception {
            Training t = new Training();
            t.setTrainingName("Solo Session");
            t.setTrainingDate(LocalDate.of(2024, 3, 10));
            t.setTrainingType(TrainingType.YOGA);
            t.setTrainingDuration(45);
            t.setTrainee(null);

            when(gymFacade.getTrainerTrainings(any(), any(), any(), any(), any(), any()))
                    .thenReturn(List.of(t));

            mockMvc.perform(get("/trainers/{username}/trainings", TARGET_USER)
                            .header(HEADER_USER, AUTH_USER)
                            .header(HEADER_PASS, AUTH_PASS))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].trainerName").doesNotExist());
        }

        @Test
        @DisplayName("400 Bad Request – invalid date format")
        void invalidDateFormat_returns400() throws Exception {
            mockMvc.perform(get("/trainers/{username}/trainings", TARGET_USER)
                            .header(HEADER_USER, AUTH_USER)
                            .header(HEADER_PASS, AUTH_PASS)
                            .param("periodFrom", "not-a-date"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("401 Unauthorized – facade throws AuthenticationException")
        void badCredentials_returns401() throws Exception {
            when(gymFacade.getTrainerTrainings(anyString(), anyString(), anyString(), any(), any(), any()))
                    .thenThrow(new AuthenticationException("Unauthorized"));

            mockMvc.perform(get("/trainers/{username}/trainings", TARGET_USER)
                            .header(HEADER_USER, "bad")
                            .header(HEADER_PASS, "creds"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /trainers/not-assigned – getNotAssignedTrainers")
    class GetNotAssignedTrainers {

        private Trainer buildTrainer(String username, String first, String last) {
            Trainer t = new Trainer();
            t.setUsername(username);
            t.setFirstName(first);
            t.setLastName(last);
            t.setSpecialization(TrainingType.STRENGTH);
            t.setActive(true);
            return t;
        }

        @Test
        @DisplayName("200 OK – returns list of unassigned trainers")
        void happyPath_returnsUnassignedTrainers() throws Exception {
            String traineeUsername = "trainee.alice";
            List<Trainer> unassigned = List.of(
                    buildTrainer("trainer.bob", "Bob", "Smith"),
                    buildTrainer("trainer.carol", "Carol", "Jones")
            );
            when(gymFacade.getUnassignedTrainers(AUTH_USER, AUTH_PASS, traineeUsername))
                    .thenReturn(unassigned);

            mockMvc.perform(get("/trainers/not-assigned")
                            .header(HEADER_USER, AUTH_USER)
                            .header(HEADER_PASS, AUTH_PASS)
                            .param("traineeUsername", traineeUsername))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].username").value("trainer.bob"))
                    .andExpect(jsonPath("$[0].firstName").value("Bob"))
                    .andExpect(jsonPath("$[0].specialization").value("STRENGTH"))
                    .andExpect(jsonPath("$[1].username").value("trainer.carol"));

            verify(gymFacade).getUnassignedTrainers(AUTH_USER, AUTH_PASS, traineeUsername);
        }

        @Test
        @DisplayName("200 OK – returns empty list when all trainers are assigned")
        void allAssigned_returnsEmptyList() throws Exception {
            when(gymFacade.getUnassignedTrainers(any(), any(), any()))
                    .thenReturn(List.of());

            mockMvc.perform(get("/trainers/not-assigned")
                            .header(HEADER_USER, AUTH_USER)
                            .header(HEADER_PASS, AUTH_PASS)
                            .param("traineeUsername", "trainee.alice"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("401 Unauthorized – facade throws AuthenticationException")
        void badCredentials_returns401() throws Exception {
            when(gymFacade.getUnassignedTrainers(anyString(), anyString(), anyString()))
                    .thenThrow(new AuthenticationException("Unauthorized"));

            mockMvc.perform(get("/trainers/not-assigned")
                            .header(HEADER_USER, "bad")
                            .header(HEADER_PASS, "creds")
                            .param("traineeUsername", "trainee.alice"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("404 Not Found – trainee username does not exist (ValidationException)")
        void traineeNotFound_returns400() throws Exception {
            when(gymFacade.getUnassignedTrainers(anyString(), anyString(), eq("ghost.user")))
                    .thenThrow(new ValidationException("Trainee not found"));

            mockMvc.perform(get("/trainers/not-assigned")
                            .header(HEADER_USER, AUTH_USER)
                            .header(HEADER_PASS, AUTH_PASS)
                            .param("traineeUsername", "ghost.user"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details").value("Trainee not found"));
        }

        @Test
        @DisplayName("400 Bad Request – missing traineeUsername query param")
        void missingTraineeUsername_returns400() throws Exception {
            mockMvc.perform(get("/trainers/not-assigned")
                            .header(HEADER_USER, AUTH_USER)
                            .header(HEADER_PASS, AUTH_PASS))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 Bad Request – missing X-Auth-Username header")
        void missingAuthUsernameHeader_returns400() throws Exception {
            mockMvc.perform(get("/trainers/not-assigned")
                            .header(HEADER_PASS, AUTH_PASS)
                            .param("traineeUsername", "trainee.alice"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Missing header"))
                    .andExpect(jsonPath("$.details").value(containsString("X-Auth-Username")));
        }
    }
}