package com.gym.workload.security;

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

    public JwtService(@Value("${jwt.secret}") String secretHex) {
        this.signingKey = Keys.hmacShaKeyFor(HexFormat.of().parseHex(secretHex));
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
