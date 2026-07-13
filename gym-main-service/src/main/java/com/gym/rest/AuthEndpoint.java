package com.gym.rest;

import com.gym.rest.dto.auth.LoginRequestDto;
import com.gym.rest.dto.auth.LoginResponseDto;
import com.gym.security.JwtService;
import com.gym.security.LoginAttemptService;
import com.gym.security.TokenBlacklistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthEndpoint {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;
    private final TokenBlacklistService tokenBlacklistService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDto request) {
        String username = request.getUsername();
        log.info("Login attempt for username: {}", username);

        if (loginAttemptService.isBlocked(username)) {
            log.warn("Login blocked for user '{}' due to too many failed attempts", username);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Account temporarily locked. Try again in 5 minutes.");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.getPassword()));

            loginAttemptService.loginSucceeded(username);
            String token = jwtService.generateToken(authentication.getName());
            log.info("Login successful for username: {}", username);
            return ResponseEntity.ok(new LoginResponseDto(token));

        } catch (BadCredentialsException e) {
            loginAttemptService.loginFailed(username);
            log.warn("Bad credentials for username: {}", username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid username or password");

        } catch (DisabledException e) {
            log.warn("Disabled account login attempt for username: {}", username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Account is disabled");

        } catch (LockedException e) {
            log.warn("Locked account login attempt for username: {}", username);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Account temporarily locked. Try again in 5 minutes.");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {

        String token = authorizationHeader.substring(7);
        tokenBlacklistService.blacklist(token);
        log.info("Token blacklisted — user logged out");
        return ResponseEntity.ok().build();
    }
}
