package com.gym.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BruteForceAuthFailureHandlerTest {

    @Mock
    private LoginAttemptService loginAttemptService;

    private BruteForceAuthFailureHandler handler;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        handler = new BruteForceAuthFailureHandler(loginAttemptService, new ObjectMapper());
        request = new MockHttpServletRequest("POST", "/login");
        response = new MockHttpServletResponse();
    }

    @Test
    void onFailure_RecordsFailedAttemptForUsername() throws IOException {
        request.setParameter("username", "john.doe");

        handler.onAuthenticationFailure(request, response, new BadCredentialsException("bad"));

        verify(loginAttemptService).loginFailed("john.doe");
    }

    @Test
    void onFailure_WithoutUsernameParam_DoesNotRecordAttempt() throws IOException {
        handler.onAuthenticationFailure(request, response, new BadCredentialsException("bad"));

        verify(loginAttemptService, never()).loginFailed(anyString());
    }

    @Test
    void onFailure_Returns401WithJsonErrorBody() throws IOException {
        request.setParameter("username", "john.doe");

        handler.onAuthenticationFailure(request, response, new BadCredentialsException("bad"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
        assertThat(response.getContentAsString()).contains("Unauthorized");
        assertThat(response.getContentAsString()).contains("Invalid username or password");
    }
}
