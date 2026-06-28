package com.gym.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptServiceTest {

    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        service = new LoginAttemptService();
    }

    @Test
    void isBlocked_ReturnsFalseForUnknownUser() {
        assertThat(service.isBlocked("unknown.user")).isFalse();
    }

    @Test
    void isBlocked_ReturnsFalseAfterOneFailure() {
        service.loginFailed("alice");
        assertThat(service.isBlocked("alice")).isFalse();
    }

    @Test
    void isBlocked_ReturnsFalseAfterTwoFailures() {
        service.loginFailed("alice");
        service.loginFailed("alice");
        assertThat(service.isBlocked("alice")).isFalse();
    }

    @Test
    void isBlocked_ReturnsTrueAfterThreeFailures() {
        service.loginFailed("alice");
        service.loginFailed("alice");
        service.loginFailed("alice");
        assertThat(service.isBlocked("alice")).isTrue();
    }

    @Test
    void loginSucceeded_ClearsFailureRecord() {
        service.loginFailed("alice");
        service.loginFailed("alice");
        service.loginFailed("alice");

        service.loginSucceeded("alice");

        assertThat(service.isBlocked("alice")).isFalse();
    }

    @Test
    void loginSucceeded_CanLoginAgainAfterSuccessfulReset() {
        service.loginFailed("alice");
        service.loginFailed("alice");
        service.loginSucceeded("alice");
        service.loginFailed("alice");

        assertThat(service.isBlocked("alice")).isFalse();
    }

    @Test
    void isBlocked_DoesNotAffectOtherUsers() {
        service.loginFailed("alice");
        service.loginFailed("alice");
        service.loginFailed("alice");

        assertThat(service.isBlocked("bob")).isFalse();
    }

    @Test
    void loginSucceeded_OnUnknownUserDoesNotThrow() {
        service.loginSucceeded("nobody");
        assertThat(service.isBlocked("nobody")).isFalse();
    }
}
