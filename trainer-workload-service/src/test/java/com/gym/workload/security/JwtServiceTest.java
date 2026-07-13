package com.gym.workload.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET =
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final String OTHER_SECRET =
            "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";

    private SecretKey key;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        key = Keys.hmacShaKeyFor(HexFormat.of().parseHex(SECRET));
        jwtService = new JwtService(SECRET);
    }

    private String token(SecretKey signingKey, String subject, long ttlMs) {
        Date now = new Date();
        return Jwts.builder()
                .subject(subject)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMs))
                .signWith(signingKey)
                .compact();
    }

    @Test
    void extractUsername_ReturnsSubject() {
        String jwt = token(key, "gym-management-service", 60_000);
        assertThat(jwtService.extractUsername(jwt)).isEqualTo("gym-management-service");
    }

    @Test
    void isValid_ReturnsTrueForFreshToken() {
        assertThat(jwtService.isValid(token(key, "svc", 60_000))).isTrue();
    }

    @Test
    void isValid_ReturnsFalseForExpiredToken() {
        assertThat(jwtService.isValid(token(key, "svc", -1_000))).isFalse();
    }

    @Test
    void isValid_ReturnsFalseForTamperedToken() {
        String jwt = token(key, "svc", 60_000);
        String tampered = jwt.substring(0, jwt.length() - 3) + "xxx";
        assertThat(jwtService.isValid(tampered)).isFalse();
    }

    @Test
    void isValid_ReturnsFalseForGarbage() {
        assertThat(jwtService.isValid("not.a.jwt.token")).isFalse();
    }

    @Test
    void isValid_ReturnsFalseForTokenSignedWithDifferentSecret() {
        SecretKey other = Keys.hmacShaKeyFor(HexFormat.of().parseHex(OTHER_SECRET));
        assertThat(jwtService.isValid(token(other, "svc", 60_000))).isFalse();
    }
}
