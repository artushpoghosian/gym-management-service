package com.gym.bdd.support;

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
public class JwtTestHelper {

    private final SecretKey signingKey;

    public JwtTestHelper(@Value("${jwt.secret}") String secretHex) {
        this.signingKey = Keys.hmacShaKeyFor(HexFormat.of().parseHex(secretHex));
    }

    public String expiredToken(String subject) {
        Instant issued = Instant.now().minus(2, ChronoUnit.HOURS);
        Instant expired = Instant.now().minus(1, ChronoUnit.HOURS);
        return Jwts.builder()
                .subject(subject)
                .issuedAt(Date.from(issued))
                .expiration(Date.from(expired))
                .signWith(signingKey)
                .compact();
    }
}
