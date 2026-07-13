package com.gym.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HexFormat;

@Service
@Slf4j
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secretHex,
            @Value("${jwt.expiration-ms}") long expirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(HexFormat.of().parseHex(secretHex));
        this.expirationMs = expirationMs;
    }

    private static final String SERVICE_SUBJECT = "gym-management-service";
    private static final long SERVICE_TOKEN_TTL_MS = 60_000;

    public String generateToken(String username) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(signingKey)
                .compact();
    }

    public String generateServiceToken() {
        Date now = new Date();
        return Jwts.builder()
                .subject(SERVICE_SUBJECT)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + SERVICE_TOKEN_TTL_MS))
                .signWith(signingKey)
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isValid(String token) {
        try {
            Date expiration = parseClaims(token).getExpiration();
            return expiration.after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
