package com.gym.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;

@Service
@Slf4j
public class JwtService {

    private static final String SERVICE_SUBJECT = "gym-management-service";
    private static final Duration SERVICE_TOKEN_TTL = Duration.ofMinutes(1);

    private final SecretKey signingKey;
    private final Duration expiration;
    private final Clock clock;

    public JwtService(
            @Value("${jwt.secret}") String secretHex,
            @Value("${jwt.expiration-ms}") long expirationMs,
            Clock clock) {
        this.signingKey = Keys.hmacShaKeyFor(HexFormat.of().parseHex(secretHex));
        this.expiration = Duration.ofMillis(expirationMs);
        this.clock = clock;
    }

    public String generateToken(String username) {
        return buildToken(username, expiration);
    }

    public String generateServiceToken() {
        return buildToken(SERVICE_SUBJECT, SERVICE_TOKEN_TTL);
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isValid(String token) {
        try {
            Instant expiresAt = parseClaims(token).getExpiration().toInstant();
            return expiresAt.isAfter(clock.instant());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    private String buildToken(String subject, Duration ttl) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(signingKey)
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
