package com.gym.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.exception.AuthenticationException;
import com.gym.facade.GymFacade;
import com.gym.rest.dto.auth.LoginRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthEndpoint.class)
@DisplayName("AuthEndpoint Tests")
class AuthEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GymFacade gymFacade;

    private static final String LOGIN_URL = "/auth/login";
    private static final String VALID_USERNAME = "john.doe";
    private static final String VALID_PASSWORD = "secret123";

    private LoginRequestDto buildRequest(String username, String password) {
        LoginRequestDto dto = new LoginRequestDto();
        dto.setUsername(username);
        dto.setPassword(password);
        return dto;
    }

    @Nested
    @DisplayName("Happy Path — 200 OK")
    class HappyPath {

        @Test
        @DisplayName("Trainee credentials match → 200 OK")
        void loginAsTrainee_returnsOk() throws Exception {
            when(gymFacade.traineeMatchCredentials(VALID_USERNAME, VALID_PASSWORD)).thenReturn(true);
            when(gymFacade.trainerMatchCredentials(VALID_USERNAME, VALID_PASSWORD)).thenReturn(false);

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    buildRequest(VALID_USERNAME, VALID_PASSWORD))))
                    .andExpect(status().isOk())
                    .andExpect(content().string(""));

            verify(gymFacade).traineeMatchCredentials(VALID_USERNAME, VALID_PASSWORD);
        }

        @Test
        @DisplayName("Trainer credentials match → 200 OK")
        void loginAsTrainer_returnsOk() throws Exception {
            when(gymFacade.traineeMatchCredentials(VALID_USERNAME, VALID_PASSWORD)).thenReturn(false);
            when(gymFacade.trainerMatchCredentials(VALID_USERNAME, VALID_PASSWORD)).thenReturn(true);

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    buildRequest(VALID_USERNAME, VALID_PASSWORD))))
                    .andExpect(status().isOk())
                    .andExpect(content().string(""));

            verify(gymFacade).trainerMatchCredentials(VALID_USERNAME, VALID_PASSWORD);
        }

        @Test
        @DisplayName("Both trainee AND trainer credentials match → 200 OK")
        void loginAsBoth_returnsOk() throws Exception {
            when(gymFacade.traineeMatchCredentials(VALID_USERNAME, VALID_PASSWORD)).thenReturn(true);
            when(gymFacade.trainerMatchCredentials(VALID_USERNAME, VALID_PASSWORD)).thenReturn(true);

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    buildRequest(VALID_USERNAME, VALID_PASSWORD))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Both facade methods are called exactly once per login attempt")
        void bothFacadeMethodsCalled_once() throws Exception {
            when(gymFacade.traineeMatchCredentials(anyString(), anyString())).thenReturn(true);
            when(gymFacade.trainerMatchCredentials(anyString(), anyString())).thenReturn(false);

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    buildRequest(VALID_USERNAME, VALID_PASSWORD))))
                    .andExpect(status().isOk());

            verify(gymFacade, times(1)).traineeMatchCredentials(VALID_USERNAME, VALID_PASSWORD);
            verify(gymFacade, times(1)).trainerMatchCredentials(VALID_USERNAME, VALID_PASSWORD);
        }
    }

    @Nested
    @DisplayName("Validation Failure — 400 Bad Request")
    class ValidationFailure {

        @Test
        @DisplayName("Missing username field → 400")
        void missingUsername_returns400() throws Exception {
            LoginRequestDto dto = new LoginRequestDto();
            dto.setPassword(VALID_PASSWORD);

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("Blank username → 400")
        void blankUsername_returns400() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    buildRequest("   ", VALID_PASSWORD))))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("Empty username string → 400")
        void emptyUsername_returns400() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    buildRequest("", VALID_PASSWORD))))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("Missing password field → 400")
        void missingPassword_returns400() throws Exception {
            LoginRequestDto dto = new LoginRequestDto();
            dto.setUsername(VALID_USERNAME);

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("Blank password → 400")
        void blankPassword_returns400() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    buildRequest(VALID_USERNAME, "  "))))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("Both username and password missing → 400")
        void bothFieldsMissing_returns400() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("Malformed JSON body → 400")
        void malformedJson_returns400() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ not-valid-json"))
                    .andExpect(result -> {
                        assert result.getResolvedException() instanceof org.springframework.http.converter.HttpMessageNotReadableException;
                    });

            verifyNoInteractions(gymFacade);
        }

        @Test
        @DisplayName("Missing Content-Type header → 415")
        void missingContentType_returns415() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .content(objectMapper.writeValueAsString(
                                    buildRequest(VALID_USERNAME, VALID_PASSWORD))))
                    .andExpect(result -> {
                        assert result.getResolvedException() instanceof org.springframework.web.HttpMediaTypeNotSupportedException;
                    });

            verifyNoInteractions(gymFacade);
        }

        @Nested
        @DisplayName("Authentication Failure - Custom Exceptions")
        class AuthenticationFailure {

            @Test
            @DisplayName("Neither trainee nor trainer credentials match → throws AuthenticationException")
            void invalidCredentials_throwsAuthenticationException() throws Exception {
                when(gymFacade.traineeMatchCredentials(anyString(), anyString())).thenReturn(false);
                when(gymFacade.trainerMatchCredentials(anyString(), anyString())).thenReturn(false);

                mockMvc.perform(post(LOGIN_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        buildRequest("wrong.user", "wrongPass"))))
                        .andExpect(result -> {
                            if (result.getResolvedException() == null) {
                                throw new AssertionError("Expected AuthenticationException to be thrown");
                            }
                            assert result.getResolvedException() instanceof AuthenticationException;
                            assert "Invalid username or password".equals(result.getResolvedException().getMessage());
                        });

                verify(gymFacade).traineeMatchCredentials("wrong.user", "wrongPass");
                verify(gymFacade).trainerMatchCredentials("wrong.user", "wrongPass");
            }

            @Test
            @DisplayName("Wrong password for existing trainee → throws AuthenticationException")
            void wrongPasswordForTrainee_throwsAuthenticationException() throws Exception {
                when(gymFacade.traineeMatchCredentials(VALID_USERNAME, "wrongPass")).thenReturn(false);
                when(gymFacade.trainerMatchCredentials(VALID_USERNAME, "wrongPass")).thenReturn(false);

                mockMvc.perform(post(LOGIN_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        buildRequest(VALID_USERNAME, "wrongPass"))))
                        .andExpect(result -> {
                            assert result.getResolvedException() instanceof AuthenticationException;
                        });
            }
        }

        @Nested
        @DisplayName("Routing — 404 / 405")
        class RoutingErrors {

            @Test
            @DisplayName("GET /auth/login → 405 Method Not Allowed")
            void getOnLoginEndpoint_returns405() throws Exception {
                mockMvc.perform(get(LOGIN_URL)
                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(result -> {
                            assert result.getResolvedException() instanceof org.springframework.web.HttpRequestMethodNotSupportedException;
                        });

                verifyNoInteractions(gymFacade);
            }

            @Test
            @DisplayName("PUT /auth/login → 405 Method Not Allowed")
            void putOnLoginEndpoint_returns405() throws Exception {
                mockMvc.perform(put(LOGIN_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        buildRequest(VALID_USERNAME, VALID_PASSWORD))))
                        .andExpect(result -> {
                            assert result.getResolvedException() instanceof org.springframework.web.HttpRequestMethodNotSupportedException;
                        });

                verifyNoInteractions(gymFacade);
            }

            @Test
            @DisplayName("POST /auth/unknown → 404 Not Found")
            void postToUnknownPath_returns404() throws Exception {
                mockMvc.perform(post("/auth/unknown")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        buildRequest(VALID_USERNAME, VALID_PASSWORD))))
                        .andExpect(result -> {
                            assert result.getResolvedException() instanceof org.springframework.web.servlet.resource.NoResourceFoundException;
                        });

                verifyNoInteractions(gymFacade);
            }

            @Test
            @DisplayName("POST /login (missing /auth prefix) → 404 Not Found")
            void postWithoutAuthPrefix_returns404() throws Exception {
                mockMvc.perform(post("/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        buildRequest(VALID_USERNAME, VALID_PASSWORD))))
                        .andExpect(result -> {
                            assert result.getResolvedException() instanceof org.springframework.web.servlet.resource.NoResourceFoundException;
                        });

                verifyNoInteractions(gymFacade);
            }

            @Nested
            @DisplayName("Edge Cases")
            class EdgeCases {

                @Test
                @DisplayName("Facade throws unexpected RuntimeException → 500 Internal Server Error")
                void facadeThrowsRuntimeException_returns500() throws Exception {
                    when(gymFacade.traineeMatchCredentials(anyString(), anyString()))
                            .thenThrow(new RuntimeException("Unexpected DB failure"));

                    mockMvc.perform(post(LOGIN_URL)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(
                                            buildRequest(VALID_USERNAME, VALID_PASSWORD))))
                            .andExpect(status().isInternalServerError());
                }

                @Test
                @DisplayName("Username with special characters still delegates to facade")
                void specialCharUsername_delegatesToFacade() throws Exception {
                    String specialUser = "user@domain.com";
                    when(gymFacade.traineeMatchCredentials(specialUser, VALID_PASSWORD)).thenReturn(true);
                    when(gymFacade.trainerMatchCredentials(specialUser, VALID_PASSWORD)).thenReturn(false);

                    mockMvc.perform(post(LOGIN_URL)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(
                                            buildRequest(specialUser, VALID_PASSWORD))))
                            .andExpect(status().isOk());

                    verify(gymFacade).traineeMatchCredentials(specialUser, VALID_PASSWORD);
                }

                @Test
                @DisplayName("Very long username/password values are forwarded to facade without truncation")
                void longCredentials_delegatesToFacade() throws Exception {
                    String longUsername = "a".repeat(300);
                    String longPassword = "b".repeat(300);
                    when(gymFacade.traineeMatchCredentials(longUsername, longPassword)).thenReturn(true);
                    when(gymFacade.trainerMatchCredentials(longUsername, longPassword)).thenReturn(false);

                    mockMvc.perform(post(LOGIN_URL)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(
                                            buildRequest(longUsername, longPassword))))
                            .andExpect(status().isOk());

                    verify(gymFacade).traineeMatchCredentials(longUsername, longPassword);
                }

                @Test
                @DisplayName("Response body is empty (Void) on success")
                void successResponse_hasEmptyBody() throws Exception {
                    when(gymFacade.traineeMatchCredentials(anyString(), anyString())).thenReturn(true);
                    when(gymFacade.trainerMatchCredentials(anyString(), anyString())).thenReturn(false);

                    mockMvc.perform(post(LOGIN_URL)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(
                                            buildRequest(VALID_USERNAME, VALID_PASSWORD))))
                            .andExpect(status().isOk())
                            .andExpect(content().string(""));
                }
            }
        }
    }
}