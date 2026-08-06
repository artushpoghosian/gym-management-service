package com.gym.bdd.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.gym.bdd.support.ApiClient;
import com.gym.bdd.support.ScenarioContext;
import io.cucumber.java.en.Given;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class TrainerSteps {

    @Autowired
    private ApiClient api;

    @Autowired
    private ScenarioContext context;

    @Given("a trainer {string} specializing in {word} is registered")
    public void aTrainerIsRegistered(String fullName, String specialization) {
        String[] parts = fullName.trim().split("\\s+", 2);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("firstName", parts[0]);
        body.put("lastName", parts.length > 1 ? parts[1] : "");
        body.put("specialization", specialization);

        ResponseEntity<String> response = api.post("/trainers", body);
        assertThat(response.getStatusCode().value())
                .as("trainer registration should succeed")
                .isEqualTo(200);

        JsonNode json = api.read(response);
        context.rememberCredentials(json.get("username").asText(), json.get("password").asText());
    }
}
