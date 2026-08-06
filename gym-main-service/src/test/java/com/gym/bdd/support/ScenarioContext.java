package com.gym.bdd.support;

import io.cucumber.spring.ScenarioScope;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ScenarioScope
@Getter
@Setter
public class ScenarioContext {

    private final Map<String, String> credentials = new HashMap<>();
    private String lastUsername;
    private String trainerUsername;
    private String traineeUsername;
    private String token;
    private ResponseEntity<String> lastResponse;

    public void rememberCredentials(String username, String password) {
        credentials.put(username, password);
        this.lastUsername = username;
    }

    public String getPassword(String username) {
        return credentials.get(username);
    }
}
