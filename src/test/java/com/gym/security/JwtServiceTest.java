package com.gym.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String TEST_SECRET_HEX =
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final long EXPIRATION_MS = 3_600_000L;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET_HEX, EXPIRATION_MS);
    }

    @Test
    void generateToken_ReturnsNonEmptyString() {
        String token = jwtService.generateToken("alice.brown");
        assertThat(token).isNotBlank();
    }

    @Test
    void extractUsername_ReturnsOriginalUsername() {
        String token = jwtService.generateToken("alice.brown");
        assertThat(jwtService.extractUsername(token)).isEqualTo("alice.brown");
    }

    @Test
    void isValid_ReturnsTrueForFreshToken() {
        String token = jwtService.generateToken("john.doe");
        assertThat(jwtService.isValid(token)).isTrue();
    }

    @Test
    void isValid_ReturnsFalseForExpiredToken() {
        JwtService shortLived = new JwtService(TEST_SECRET_HEX, -1000L);
        String token = shortLived.generateToken("john.doe");
        assertThat(jwtService.isValid(token)).isFalse();
    }

    @Test
    void isValid_ReturnsFalseForTamperedToken() {
        String token = jwtService.generateToken("john.doe");
        String tampered = token.substring(0, token.length() - 4) + "xxxx";
        assertThat(jwtService.isValid(tampered)).isFalse();
    }

    @Test
    void isValid_ReturnsFalseForGarbage() {
        assertThat(jwtService.isValid("not.a.jwt.token")).isFalse();
    }

    @Test
    void generateToken_ProducesDistinctTokensForDifferentUsers() {
        String t1 = jwtService.generateToken("user.one");
        String t2 = jwtService.generateToken("user.two");
        assertThat(t1).isNotEqualTo(t2);
    }
}
