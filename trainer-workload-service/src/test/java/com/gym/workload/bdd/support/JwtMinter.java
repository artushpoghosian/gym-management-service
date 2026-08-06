package com.gym.workload.bdd.support;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HexFormat;

@Component
public class JwtMinter {

    private final SecretKey signingKey;

    public JwtMinter(@Value("${jwt.secret}") String secretHex) {
        this.signingKey = Keys.hmacShaKeyFor(HexFormat.of().parseHex(secretHex));
    }

    public String validToken(String subject) {
        return build(subject, Instant.now().plus(1, ChronoUnit.HOURS));
    }

    private String build(String subject, Instant expiry) {
        return Jwts.builder()
                .subject(subject)
                .issuedAt(new Date())
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }
}
