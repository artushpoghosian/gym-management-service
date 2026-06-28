package com.gym.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 3;
    private static final long LOCK_DURATION_SECONDS = 300; // 5 minutes

    private record AttemptRecord(int count, Instant lockedUntil) {}

    private final ConcurrentHashMap<String, AttemptRecord> attempts = new ConcurrentHashMap<>();

    public void loginSucceeded(String username) {
        attempts.remove(username);
        log.debug("Login succeeded for '{}', attempt record cleared", username);
    }

    public void loginFailed(String username) {
        AttemptRecord current = attempts.getOrDefault(username, new AttemptRecord(0, Instant.EPOCH));

        int newCount = current.count() + 1;
        Instant lockedUntil = newCount >= MAX_ATTEMPTS
                ? Instant.now().plusSeconds(LOCK_DURATION_SECONDS)
                : Instant.EPOCH;

        attempts.put(username, new AttemptRecord(newCount, lockedUntil));

        if (newCount >= MAX_ATTEMPTS) {
            log.warn("User '{}' locked out after {} failed attempts until {}", username, newCount, lockedUntil);
        } else {
            log.warn("Failed login attempt {}/{} for '{}'", newCount, MAX_ATTEMPTS, username);
        }
    }

    public boolean isBlocked(String username) {
        AttemptRecord record = attempts.get(username);
        if (record == null) {
            return false;
        }
        if (record.lockedUntil().isAfter(Instant.now())) {
            log.warn("Blocked login attempt for locked user '{}'", username);
            return true;
        }
        attempts.remove(username);
        return false;
    }
}
