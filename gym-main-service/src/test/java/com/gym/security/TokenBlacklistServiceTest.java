package com.gym.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenBlacklistServiceTest {

    private TokenBlacklistService blacklistService;

    @BeforeEach
    void setUp() {
        blacklistService = new TokenBlacklistService();
    }

    @Test
    void isBlacklisted_ReturnsFalseForUnknownToken() {
        assertThat(blacklistService.isBlacklisted("some-token")).isFalse();
    }

    @Test
    void isBlacklisted_ReturnsTrueAfterBlacklisting() {
        blacklistService.blacklist("some-token");
        assertThat(blacklistService.isBlacklisted("some-token")).isTrue();
    }

    @Test
    void blacklist_DoesNotAffectOtherTokens() {
        blacklistService.blacklist("token-a");
        assertThat(blacklistService.isBlacklisted("token-b")).isFalse();
    }

    @Test
    void blacklist_IsIdempotent() {
        blacklistService.blacklist("token-a");
        blacklistService.blacklist("token-a");
        assertThat(blacklistService.isBlacklisted("token-a")).isTrue();
    }
}
