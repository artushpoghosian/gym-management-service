package com.gym.utilities;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Predicate;

@Component
@Slf4j
public class UserUtils {

    public String generateUsername(String firstName, String lastName, Predicate<String> usernameExists) {
        final String baseUsername = firstName.toLowerCase() + "." + lastName.toLowerCase();

        String username = baseUsername;
        int s = 1;
        while (usernameExists.test(username)) {
            username = baseUsername + s;
            s++;
        }

        log.info("Generated username: {}", username);
        return username;
    }

    public String generatePassword() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }
}
