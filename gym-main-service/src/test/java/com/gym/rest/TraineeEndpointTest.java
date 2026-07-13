package com.gym.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gym.config.TestSecurityConfig;
import com.gym.security.JwtAuthenticationFilter;
import com.gym.exception.AuthenticationException;
import com.gym.exception.GlobalExceptionHandler;
import com.gym.facade.GymFacade;
import com.gym.model.Trainee;
import com.gym.model.Trainer;
import com.gym.model.Training;
import com.gym.model.TrainingType;
import com.gym.rest.dto.trainee.TraineeRegistrationRequestDto;
import com.gym.rest.dto.trainee.TraineeStatusPatchDto;
import com.gym.rest.dto.trainee.TraineeUpdateRequestDto;
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
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TraineeEndpoint.class)
@Import({JwtAuthenticationFilter.class, GlobalExceptionHandler.class, TestSecurityConfig.class})
@DisplayName("TraineeEndpoint Tests")
class TraineeEndpointTest {

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

    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/trainees";
    private static final String AUTH_USER = "john.doe";
    private static final String TARGET_USERNAME = "jane.smith";
    private static final String BEARER_TOKEN = "test-bearer-token";

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

    private Trainee buildTrainee(String username) {
        Trainee t = new Trainee();
        t.setUsername(username);
        t.setFirstName("Jane");
        t.setLastName("Smith");
        t.setDateOfBirth(LocalDate.of(1990, 5, 20));
        t.setAddress("123 Main St");
        t.setActive(true);
        t.setPassword("generatedPass");
        return t;
    }

    private TraineeRegistrationRequestDto buildRegisterRequest() {
        TraineeRegistrationRequestDto dto = new TraineeRegistrationRequestDto();
        dto.setFirstName("Jane");
        dto.setLastName("Smith");
        dto.setDateOfBirth(LocalDate.of(1990, 5, 20));
        dto.setAddress("123 Main St");
        return dto;
    }

    private TraineeUpdateRequestDto buildUpdateRequest() {
        TraineeUpdateRequestDto dto = new TraineeUpdateRequestDto();
        dto.setFirstName("Jane");
        dto.setLastName("Smith");
        dto.setDateOfBirth(LocalDate.of(1990, 5, 20));
        dto.setAddress("456 New Ave");
        dto.setIsActive(true);
        return dto;
    }

    @Nested
    @DisplayName("POST /trainees — Register Trainee")
    class RegisterTrainee {

        @Test
        @DisplayName("Valid request → 200 OK with generated username & password")
        void validRequest_returns200WithCredentials() throws Exception {
            Trainee created = buildTrainee(TARGET_USERNAME);
            when(gymFacade.createTrainee(any(Trainee.class))).thenReturn(created);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildRegisterRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value(TARGET_USERNAME))
                    .andExpect(jsonPath("$.password").value("generatedPass"));

            verify(gymFacade).createTrainee(any(Trainee.class));
        }

        @Test
        @DisplayName("Missing firstName → 400 with field error")
        void missingFirstName_returns400() throws Exception {
            TraineeRegistrationRequestDto dto = buildRegisterRequest();
            dto.setFirstName(null);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.firstName").value("First name is required"));

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("Blank firstName → 400 with field error")
        void blankFirstName_returns400() throws Exception {
            TraineeRegistrationRequestDto dto = buildRegisterRequest();
            dto.setFirstName("   ");

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.firstName").exists());

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("Missing lastName → 400 with field error")
        void missingLastName_returns400() throws Exception {
            TraineeRegistrationRequestDto dto = buildRegisterRequest();
            dto.setLastName(null);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.lastName").value("Last name is required"));

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("Both firstName and lastName blank → 400 with two field errors")
        void bothNamesMissing_returns400WithTwoErrors() throws Exception {
            TraineeRegistrationRequestDto dto = new TraineeRegistrationRequestDto();

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.firstName").exists())
                    .andExpect(jsonPath("$.lastName").exists());

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("Optional fields (dateOfBirth, address) absent → 200 OK")
        void optionalFieldsAbsent_returns200() throws Exception {
            TraineeRegistrationRequestDto dto = new TraineeRegistrationRequestDto();
            dto.setFirstName("Jane");
            dto.setLastName("Smith");

            Trainee created = buildTrainee(TARGET_USERNAME);
            when(gymFacade.createTrainee(any(Trainee.class))).thenReturn(created);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value(TARGET_USERNAME));
        }

        @Test
        @DisplayName("Facade throws RuntimeException → 500 Internal Server Error")
        void facadeThrowsException_returns500() throws Exception {
            when(gymFacade.createTrainee(any(Trainee.class)))
                    .thenThrow(new RuntimeException("DB failure"));

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildRegisterRequest())))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("Malformed JSON body → 400")
        void malformedJson_returns400() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ not-valid-json"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(gymFacade);
        }
    }

    @Nested
    @DisplayName("GET /trainees/{username} — Get Profile")
    class GetTraineeProfile {

        @Test
        @DisplayName("Valid auth headers & existing username → 200 OK with profile")
        void validRequest_returns200WithProfile() throws Exception {
            Trainee trainee = buildTrainee(TARGET_USERNAME);
            when(gymFacade.selectTrainee(AUTH_USER, "secret123", TARGET_USERNAME))
                    .thenReturn(Optional.of(trainee));

            mockMvc.perform(get(BASE_URL + "/{username}", TARGET_USERNAME)
                            .header(HttpHeaders.AUTHORIZATION, bearerToken())
                            .header("X-Auth-Username", AUTH_USER)
                            .header("X-Auth-Password", "secret123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("Jane"))
                    .andExpect(jsonPath("$.lastName").value("Smith"))
                    .andExpect(jsonPath("$.address").value("123 Main St"))
                    .andExpect(jsonPath("$.active").value(true))
                    .andExpect(jsonPath("$.trainers").isArray());
        }

        @Test
        @DisplayName("Trainee not found → 404 Not Found")
        void traineeNotFound_returns404() throws Exception {
            when(gymFacade.selectTrainee(anyString(), anyString(), eq("ghost")))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get(BASE_URL + "/{username}", "ghost")
                            .header(HttpHeaders.AUTHORIZATION, bearerToken())
                            .header("X-Auth-Username", AUTH_USER)
                            .header("X-Auth-Password", "secret123"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Not Found"));
        }

        @Test
        @DisplayName("No Authorization header → 401")
        void missingAuthUsernameHeader_returns401FromFilter() throws Exception {
            mockMvc.perform(get(BASE_URL + "/{username}", TARGET_USERNAME)
                            .header("X-Auth-Password", "secret123"))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("No Authorization header (only X-Auth-Username) → 401")
        void missingAuthPasswordHeader_returns401FromFilter() throws Exception {
            mockMvc.perform(get(BASE_URL + "/{username}", TARGET_USERNAME)
                            .header("X-Auth-Username", AUTH_USER))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("Both auth headers missing → 401")
        void bothHeadersMissing_returns401FromFilter() throws Exception {
            mockMvc.perform(get(BASE_URL + "/{username}", TARGET_USERNAME))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("Facade throws AuthenticationException → 401")
        void facadeThrowsAuthException_returns401() throws Exception {
            when(gymFacade.selectTrainee(anyString(), anyString(), anyString()))
                    .thenThrow(new AuthenticationException("Invalid credentials"));

            mockMvc.perform(get(BASE_URL + "/{username}", TARGET_USERNAME)
                            .header(HttpHeaders.AUTHORIZATION, bearerToken())
                            .header("X-Auth-Username", AUTH_USER)
                            .header("X-Auth-Password", "wrongPass"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().string("Invalid credentials"));
        }

        @Test
        @DisplayName("Response contains empty trainers list when none assigned")
        void emptyTrainersList_inResponse() throws Exception {
            Trainee trainee = buildTrainee(TARGET_USERNAME);
            when(gymFacade.selectTrainee(anyString(), anyString(), eq(TARGET_USERNAME)))
                    .thenReturn(Optional.of(trainee));

            mockMvc.perform(get(BASE_URL + "/{username}", TARGET_USERNAME)
                            .header(HttpHeaders.AUTHORIZATION, bearerToken())
                            .header("X-Auth-Username", AUTH_USER)
                            .header("X-Auth-Password", "secret123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.trainers", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("PUT /trainees/{username} — Update Profile")
    class UpdateTraineeProfile {

        @Test
        @DisplayName("Valid request → 200 OK with updated profile")
        void validRequest_returns200WithUpdatedProfile() throws Exception {
            Trainee updated = buildTrainee(TARGET_USERNAME);
            updated.setAddress("456 New Ave");
            when(gymFacade.updateTrainee(eq(AUTH_USER), anyString(), any(Trainee.class)))
                    .thenReturn(updated);

            mockMvc.perform(put(BASE_URL + "/{username}", TARGET_USERNAME)
                            .header(HttpHeaders.AUTHORIZATION, bearerToken())
                            .header("X-Auth-Username", AUTH_USER)
                            .header("X-Auth-Password", "secret123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildUpdateRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("Jane"))
                    .andExpect(jsonPath("$.lastName").value("Smith"))
                    .andExpect(jsonPath("$.address").value("456 New Ave"))
                    .andExpect(jsonPath("$.active").value(true));
        }

        @Test
        @DisplayName("Missing firstName in body → 400")
        void missingFirstName_returns400() throws Exception {
            TraineeUpdateRequestDto dto = buildUpdateRequest();
            dto.setFirstName(null);

            mockMvc.perform(put(BASE_URL + "/{username}", TARGET_USERNAME)
                            .header(HttpHeaders.AUTHORIZATION, bearerToken())
                            .header("X-Auth-Username", AUTH_USER)
                            .header("X-Auth-Password", "secret123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.firstName").value("First name is required"));

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("Missing lastName in body → 400")
        void missingLastName_returns400() throws Exception {
            TraineeUpdateRequestDto dto = buildUpdateRequest();
            dto.setLastName(null);

            mockMvc.perform(put(BASE_URL + "/{username}", TARGET_USERNAME)
                            .header(HttpHeaders.AUTHORIZATION, bearerToken())
                            .header("X-Auth-Username", AUTH_USER)
                            .header("X-Auth-Password", "secret123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.lastName").value("Last name is required"));

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("Null isActive in body → 400")
        void nullIsActive_returns400() throws Exception {
            TraineeUpdateRequestDto dto = buildUpdateRequest();
            dto.setIsActive(null);

            mockMvc.perform(put(BASE_URL + "/{username}", TARGET_USERNAME)
                            .header(HttpHeaders.AUTHORIZATION, bearerToken())
                            .header("X-Auth-Username", AUTH_USER)
                            .header("X-Auth-Password", "secret123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isActive").value("Is Active status is required"));

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("Missing Authorization header → 401")
        void missingHeaders_returns401FromFilter() throws Exception {
            mockMvc.perform(put(BASE_URL + "/{username}", TARGET_USERNAME)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildUpdateRequest())))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("Facade throws AuthenticationException → 401")
        void facadeThrowsAuthException_returns401() throws Exception {
            when(gymFacade.updateTrainee(anyString(), anyString(), any(Trainee.class)))
                    .thenThrow(new AuthenticationException("Unauthorized"));

            mockMvc.perform(put(BASE_URL + "/{username}", TARGET_USERNAME)
                            .header(HttpHeaders.AUTHORIZATION, bearerToken())
                            .header("X-Auth-Username", AUTH_USER)
                            .header("X-Auth-Password", "wrongPass")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildUpdateRequest())))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Username in path is passed to the trainee object")
        void usernameFromPath_isSetOnTrainee() throws Exception {
            Trainee updated = buildTrainee(TARGET_USERNAME);
            when(gymFacade.updateTrainee(anyString(), anyString(), any(Trainee.class)))
                    .thenReturn(updated);

            mockMvc.perform(put(BASE_URL + "/{username}", TARGET_USERNAME)
                            .header(HttpHeaders.AUTHORIZATION, bearerToken())
                            .header("X-Auth-Username", AUTH_USER)
                            .header("X-Auth-Password", "secret123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildUpdateRequest())))
                    .andExpect(status().isOk());

            verify(gymFacade).updateTrainee(anyString(), anyString(),
                    argThat(t -> TARGET_USERNAME.equals(t.getUsername())));
        }
    }

    @Nested
    @DisplayName("DELETE /trainees/{username} — Delete Trainee")
    class DeleteTrainee {

        @Test
        @DisplayName("Valid auth + existing username → 200 OK, empty body")
        void validRequest_returns200() throws Exception {
            doNothing().when(gymFacade).deleteTrainee(anyString(), anyString(), eq(TARGET_USERNAME));

            mockMvc.perform(delete(BASE_URL + "/{username}", TARGET_USERNAME)
                            .header(HttpHeaders.AUTHORIZATION, bearerToken())
                            .header("X-Auth-Username", AUTH_USER)
                            .header("X-Auth-Password", "secret123"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(""));
        }

        @Test
        @DisplayName("Missing Authorization header → 401")
        void missingUsernameHeader_returns401FromFilter() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/{username}", TARGET_USERNAME)
                            .header("X-Auth-Password", "secret123"))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("Facade throws AuthenticationException → 401")
        void facadeThrowsAuthException_returns401() throws Exception {
            doThrow(new AuthenticationException("Unauthorized"))
                    .when(gymFacade).deleteTrainee(anyString(), anyString(), anyString());

            mockMvc.perform(delete(BASE_URL + "/{username}", TARGET_USERNAME)
                            .header(HttpHeaders.AUTHORIZATION, bearerToken())
                            .header("X-Auth-Username", AUTH_USER)
                            .header("X-Auth-Password", "badPass"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().string("Unauthorized"));
        }

        @Test
        @DisplayName("Delete delegates to facade exactly once")
        void deleteDelegatesExactlyOnce() throws Exception {
            doNothing().when(gymFacade).deleteTrainee(anyString(), anyString(), anyString());

            mockMvc.perform(delete(BASE_URL + "/{username}", TARGET_USERNAME)
                            .header(HttpHeaders.AUTHORIZATION, bearerToken())
                            .header("X-Auth-Username", AUTH_USER)
                            .header("X-Auth-Password", "secret123"))
                    .andExpect(status().isOk());

            verify(gymFacade, times(1)).deleteTrainee(anyString(), anyString(), eq(TARGET_USERNAME));
        }
    }

    @Nested
    @DisplayName("PATCH /trainees/{username}/status — Update Status")
    class UpdateTraineeStatus {

        private TraineeStatusPatchDto buildStatusDto(Boolean active) {
            TraineeStatusPatchDto dto = new TraineeStatusPatchDto();
            dto.setIsActive(active);
            return dto;
        }

        @Test
        @DisplayName("Activate trainee → 200 OK")
        void activateTrainee_returns200() throws Exception {
            doNothing().when(gymFacade).setTraineeActive(anyString(), anyString(), eq(TARGET_USERNAME), eq(true));

            mockMvc.perform(patch(BASE_URL + "/{username}/status", TARGET_USERNAME)
                            .header(HttpHeaders.AUTHORIZATION, bearerToken())
                            .header("X-Auth-Username", AUTH_USER)
                            .header("X-Auth-Password", "secret123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildStatusDto(true))))
                    .andExpect(status().isOk())
                    .andExpect(content().string(""));
        }

        @Test
        @DisplayName("Deactivate trainee → 200 OK")
        void deactivateTrainee_returns200() throws Exception {
            doNothing().when(gymFacade).setTraineeActive(anyString(), anyString(), eq(TARGET_USERNAME), eq(false));

            mockMvc.perform(patch(BASE_URL + "/{username}/status", TARGET_USERNAME)
                            .header(HttpHeaders.AUTHORIZATION, bearerToken())
                            .header("X-Auth-Username", AUTH_USER)
                            .header("X-Auth-Password", "secret123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildStatusDto(false))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Null isActive in body → 400")
        void nullIsActive_returns400() throws Exception {
            mockMvc.perform(patch(BASE_URL + "/{username}/status", TARGET_USERNAME)
                            .header(HttpHeaders.AUTHORIZATION, bearerToken())
                            .header("X-Auth-Username", AUTH_USER)
                            .header("X-Auth-Password", "secret123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"isActive\": null}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isActive").value("Is Active status is required"));

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("Missing Authorization header → 401")
        void missingHeaders_returns401FromFilter() throws Exception {
            mockMvc.perform(patch(BASE_URL + "/{username}/status", TARGET_USERNAME)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildStatusDto(true))))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("Facade throws AuthenticationException → 401")
        void facadeThrowsAuthException_returns401() throws Exception {
            doThrow(new AuthenticationException("Unauthorized"))
                    .when(gymFacade).setTraineeActive(anyString(), anyString(), anyString(), anyBoolean());

            mockMvc.perform(patch(BASE_URL + "/{username}/status", TARGET_USERNAME)
                            .header(HttpHeaders.AUTHORIZATION, bearerToken())
                            .header("X-Auth-Username", AUTH_USER)
                            .header("X-Auth-Password", "wrong")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildStatusDto(true))))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /trainees/{username}/trainers/not-assigned — Unassigned Trainers")
    class GetUnassignedTrainers {

        private Trainer buildTrainer(String username) {
            Trainer t = new Trainer();
            t.setUsername(username);
            t.setFirstName("Bob");
            t.setLastName("Builder");
            t.setSpecialization(TrainingType.CARDIO);
            return t;
        }

        @Test
        @DisplayName("Trainers exist → 200 OK with list")
        void unassignedTrainersExist_returns200WithList() throws Exception {
            List<Trainer> trainers = List.of(
                    buildTrainer("bob.builder"),
                    buildTrainer("alice.coach")
            );
            when(gymFacade.getUnassignedTrainers(anyString(), anyString(), eq(TARGET_USERNAME)))
                    .thenReturn(trainers);

            mockMvc.perform(get(BASE_URL + "/{username}/trainers/not-assigned", TARGET_USERNAME)
                            .header(HttpHeaders.AUTHORIZATION, bearerToken())
                            .header("X-Auth-Username", AUTH_USER)
                            .header("X-Auth-Password", "secret123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].username").value("bob.builder"))
                    .andExpect(jsonPath("$[0].firstName").value("Bob"))
                    .andExpect(jsonPath("$[0].specialization").value("CARDIO"));
        }

        @Test
        @DisplayName("No unassigned trainers → 200 OK with empty list")
        void noUnassignedTrainers_returns200WithEmptyList() throws Exception {
            when(gymFacade.getUnassignedTrainers(anyString(), anyString(), anyString()))
                    .thenReturn(List.of());

            mockMvc.perform(get(BASE_URL + "/{username}/trainers/not-assigned", TARGET_USERNAME)
                            .header(HttpHeaders.AUTHORIZATION, bearerToken())
                            .header("X-Auth-Username", AUTH_USER)
                            .header("X-Auth-Password", "secret123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("Missing Authorization header → 401")
        void missingHeaders_returns401FromFilter() throws Exception {
            mockMvc.perform(get(BASE_URL + "/{username}/trainers/not-assigned", TARGET_USERNAME))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("Facade throws AuthenticationException → 401")
        void facadeThrowsAuthException_returns401() throws Exception {
            when(gymFacade.getUnassignedTrainers(anyString(), anyString(), anyString()))
                    .thenThrow(new AuthenticationException("Bad credentials"));

            mockMvc.perform(get(BASE_URL + "/{username}/trainers/not-assigned", TARGET_USERNAME)
                            .header(HttpHeaders.AUTHORIZATION, bearerToken())
                            .header("X-Auth-Username", AUTH_USER)
                            .header("X-Auth-Password", "wrong"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PUT /trainees/{username}/trainers — Update Trainer List")
    class UpdateTraineeTrainers {

        @Test
        @DisplayName("Valid trainer list → 200 OK")
        void validRequest_returns200() throws Exception {
            Trainee trainee = buildTrainee(TARGET_USERNAME);
            when(gymFacade.updateTraineeTrainersList(anyString(), anyString(), eq(TARGET_USERNAME), anyList()))
                    .thenReturn(trainee);

            mockMvc.perform(put(BASE_URL + "/{username}/trainers", TARGET_USERNAME)
                            .header(HttpHeaders.AUTHORIZATION, bearerToken())
                            .header("X-Auth-Username", AUTH_USER)
                            .header("X-Auth-Password", "secret123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(List.of("bob.builder", "alice.coach"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("Empty trainer list is accepted → 200 OK")
        void emptyTrainerList_returns200() throws Exception {
            Trainee trainee = buildTrainee(TARGET_USERNAME);
            when(gymFacade.updateTraineeTrainersList(anyString(), anyString(), anyString(), anyList()))
                    .thenReturn(trainee);

            mockMvc.perform(put(BASE_URL + "/{username}/trainers", TARGET_USERNAME)
                            .header(HttpHeaders.AUTHORIZATION, bearerToken())
                            .header("X-Auth-Username", AUTH_USER)
                            .header("X-Auth-Password", "secret123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("[]"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Missing Authorization header → 401")
        void missingHeaders_returns401FromFilter() throws Exception {
            mockMvc.perform(put(BASE_URL + "/{username}/trainers", TARGET_USERNAME)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(List.of("bob.builder"))))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(gymFacade);
        }
    }

    @Nested
    @DisplayName("GET /trainees/{username}/trainings — Get Trainings")
    class GetTraineeTrainings {

        private Training buildTraining() {
            Trainer trainer = new Trainer();
            trainer.setUsername("bob.builder");

            Training t = new Training();
            t.setTrainingName("Morning Run");
            t.setTrainingDate(LocalDate.of(2024, 3, 15));
            t.setTrainingType(TrainingType.CARDIO);
            t.setTrainingDuration(60);
            t.setTrainer(trainer);
            return t;
        }

        @Test
        @DisplayName("No filters → 200 OK with all trainings")
        void noFilters_returns200WithAllTrainings() throws Exception {
            when(gymFacade.getTraineeTrainings(
                    anyString(), anyString(), eq(TARGET_USERNAME),
                    isNull(), isNull(), isNull(), isNull()))
                    .thenReturn(List.of(buildTraining()));

            mockMvc.perform(get(BASE_URL + "/{username}/trainings", TARGET_USERNAME)
                            .header(HttpHeaders.AUTHORIZATION, bearerToken())
                            .header("X-Auth-Username", AUTH_USER)
                            .header("X-Auth-Password", "secret123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].trainingName").value("Morning Run"))
                    .andExpect(jsonPath("$[0].trainingType").value("CARDIO"))
                    .andExpect(jsonPath("$[0].trainingDuration").value(60))
                    .andExpect(jsonPath("$[0].trainerName").value("bob.builder"));
        }

        @Test
        @DisplayName("Date range filters passed to facade")
        void dateRangeFilters_passedToFacade() throws Exception {
            when(gymFacade.getTraineeTrainings(anyString(), anyString(), anyString(),
                    any(), any(), isNull(), isNull()))
                    .thenReturn(List.of());

            mockMvc.perform(get(BASE_URL + "/{username}/trainings", TARGET_USERNAME)
                            .header(HttpHeaders.AUTHORIZATION, bearerToken())
                            .header("X-Auth-Username", AUTH_USER)
                            .header("X-Auth-Password", "secret123")
                            .param("periodFrom", "2024-01-01")
                            .param("periodTo", "2024-12-31"))
                    .andExpect(status().isOk());

            verify(gymFacade).getTraineeTrainings(
                    anyString(), anyString(), eq(TARGET_USERNAME),
                    eq(LocalDate.of(2024, 1, 1)),
                    eq(LocalDate.of(2024, 12, 31)),
                    isNull(), isNull());
        }

        @Test
        @DisplayName("No trainings found → 200 OK with empty list")
        void noTrainings_returns200WithEmptyList() throws Exception {
            when(gymFacade.getTraineeTrainings(anyString(), anyString(), anyString(),
                    any(), any(), any(), any()))
                    .thenReturn(List.of());

            mockMvc.perform(get(BASE_URL + "/{username}/trainings", TARGET_USERNAME)
                            .header(HttpHeaders.AUTHORIZATION, bearerToken())
                            .header("X-Auth-Username", AUTH_USER)
                            .header("X-Auth-Password", "secret123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("Missing Authorization header → 401")
        void missingHeaders_returns401FromFilter() throws Exception {
            mockMvc.perform(get(BASE_URL + "/{username}/trainings", TARGET_USERNAME))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("Invalid date format for periodFrom → 400")
        void invalidDateFormat_returns400() throws Exception {
            mockMvc.perform(get(BASE_URL + "/{username}/trainings", TARGET_USERNAME)
                            .header(HttpHeaders.AUTHORIZATION, bearerToken())
                            .header("X-Auth-Username", AUTH_USER)
                            .header("X-Auth-Password", "secret123")
                            .param("periodFrom", "not-a-date"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Routing — Wrong HTTP Methods")
    class RoutingTests {

        @Test
        @DisplayName("GET /trainees (no auth) → 401")
        void getOnRegisterEndpoint_returns401FromFilter() throws Exception {
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("POST /trainees/{username} (no auth) → 401")
        void postOnProfileEndpoint_returns401FromFilter() throws Exception {
            mockMvc.perform(post(BASE_URL + "/{username}", TARGET_USERNAME)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(gymFacade);
        }
    }
}
