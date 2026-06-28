package com.gym.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Service
@Slf4j
public class TokenBlacklistService {

    private final Set<String> blacklist = Collections.synchronizedSet(new HashSet<>());

    public void blacklist(String token) {
        blacklist.add(token);
        log.debug("Token blacklisted");
    }

    public boolean isBlacklisted(String token) {
        return blacklist.contains(token);
    }
}
