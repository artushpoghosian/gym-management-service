package com.gym.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BruteForceAuthSuccessHandlerTest {

    @Mock
    private LoginAttemptService loginAttemptService;

    @InjectMocks
    private BruteForceAuthSuccessHandler handler;

    @Test
    void onSuccess_ClearsFailedAttemptsForUser() {
        Authentication auth = new UsernamePasswordAuthenticationToken("john.doe", null, List.of());

        handler.onAuthenticationSuccess(new MockHttpServletRequest(), new MockHttpServletResponse(), auth);

        verify(loginAttemptService).loginSucceeded("john.doe");
    }
}
