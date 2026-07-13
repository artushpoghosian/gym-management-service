package com.gym.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.config.TestSecurityConfig;
import com.gym.rest.dto.auth.LoginRequestDto;
import com.gym.security.JwtAuthenticationFilter;
import com.gym.security.GymUserDetailsService;
import com.gym.security.JwtService;
import com.gym.security.LoginAttemptService;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthEndpoint.class)
@Import({JwtAuthenticationFilter.class, TestSecurityConfig.class})
@DisplayName("AuthEndpoint Tests")
class AuthEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private LoginAttemptService loginAttemptService;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @MockBean
    private GymUserDetailsService userDetailsService;

    private static final String LOGIN_URL = "/auth/login";
    private static final String LOGOUT_URL = "/auth/logout";
    private static final String AUTH_USER = "john.doe";
    private static final String AUTH_PASS = "secret123";
    private static final String BEARER_TOKEN = "test-bearer-token";

    @BeforeEach
    void setUp() {
        UserDetails userDetails = User.withUsername(AUTH_USER).password("").roles("USER").build();
        lenient().when(jwtService.isValid(BEARER_TOKEN)).thenReturn(true);
        lenient().when(tokenBlacklistService.isBlacklisted(BEARER_TOKEN)).thenReturn(false);
        lenient().when(jwtService.extractUsername(BEARER_TOKEN)).thenReturn(AUTH_USER);
        lenient().when(userDetailsService.loadUserByUsername(AUTH_USER)).thenReturn(userDetails);
    }

    private String bearerToken() {
        return "Bearer " + BEARER_TOKEN;
    }

    private LoginRequestDto buildRequest(String username, String password) {
        LoginRequestDto dto = new LoginRequestDto();
        dto.setUsername(username);
        dto.setPassword(password);
        return dto;
    }

    @Nested
    @DisplayName("POST /auth/login — Login")
    class Login {

        @Test
        @DisplayName("Valid credentials → 200 OK with JWT token")
        void validCredentials_returns200WithToken() throws Exception {
            Authentication mockAuth = mock(Authentication.class);
            when(mockAuth.getName()).thenReturn(AUTH_USER);
            when(loginAttemptService.isBlocked(AUTH_USER)).thenReturn(false);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(mockAuth);
            when(jwtService.generateToken(AUTH_USER)).thenReturn("generated-jwt-token");

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildRequest(AUTH_USER, AUTH_PASS))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("generated-jwt-token"));

            verify(loginAttemptService).loginSucceeded(AUTH_USER);
        }

        @Test
        @DisplayName("Bad credentials → 401 and failure recorded")
        void badCredentials_returns401AndRecordsFailure() throws Exception {
            when(loginAttemptService.isBlocked(AUTH_USER)).thenReturn(false);
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildRequest(AUTH_USER, "wrong"))))
                    .andExpect(status().isUnauthorized());

            verify(loginAttemptService).loginFailed(AUTH_USER);
        }

        @Test
        @DisplayName("Account blocked after max attempts → 429 Too Many Requests")
        void blockedUser_returns429() throws Exception {
            when(loginAttemptService.isBlocked(AUTH_USER)).thenReturn(true);

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildRequest(AUTH_USER, AUTH_PASS))))
                    .andExpect(status().isTooManyRequests());

            verifyNoInteractions(authenticationManager);
        }

        @Test
        @DisplayName("Disabled account → 401")
        void disabledAccount_returns401() throws Exception {
            when(loginAttemptService.isBlocked(AUTH_USER)).thenReturn(false);
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new DisabledException("Account disabled"));

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildRequest(AUTH_USER, AUTH_PASS))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Missing username → 400")
        void missingUsername_returns400() throws Exception {
            LoginRequestDto dto = new LoginRequestDto();
            dto.setPassword(AUTH_PASS);

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(authenticationManager);
        }

        @Test
        @DisplayName("Blank username → 400")
        void blankUsername_returns400() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildRequest("   ", AUTH_PASS))))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(authenticationManager);
        }

        @Test
        @DisplayName("Missing password → 400")
        void missingPassword_returns400() throws Exception {
            LoginRequestDto dto = new LoginRequestDto();
            dto.setUsername(AUTH_USER);

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(authenticationManager);
        }

        @Test
        @DisplayName("Both fields missing → 400")
        void bothFieldsMissing_returns400() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(authenticationManager);
        }

        @Test
        @DisplayName("Malformed JSON → 400")
        void malformedJson_returns400() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ not-valid-json"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(authenticationManager);
        }
    }

    @Nested
    @DisplayName("POST /auth/logout — Logout")
    class Logout {

        @Test
        @DisplayName("Valid Bearer token → 200 OK, token blacklisted")
        void validToken_returns200AndBlacklists() throws Exception {
            mockMvc.perform(post(LOGOUT_URL)
                            .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                    .andExpect(status().isOk());

            verify(tokenBlacklistService).blacklist(BEARER_TOKEN);
        }

        @Test
        @DisplayName("No Authorization header → 401")
        void noToken_returns401() throws Exception {
            mockMvc.perform(post(LOGOUT_URL))
                    .andExpect(status().isUnauthorized());

            verify(tokenBlacklistService, never()).blacklist(any());
        }
    }
}
