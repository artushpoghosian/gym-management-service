package com.gym.integration.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.gym.integration.env.Infrastructure;
import com.gym.integration.support.HttpSupport;
import com.gym.integration.support.ScenarioContext;
import io.cucumber.java.en.Given;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class OnboardingSteps {

    private final ScenarioContext context;
    private final HttpSupport http;

    public OnboardingSteps(ScenarioContext context, HttpSupport http) {
        this.context = context;
        this.http = http;
    }

    @Given("a trainer {string} specializing in {word} is registered")
    public void aTrainerIsRegistered(String fullName, String specialization) {
        String[] parts = fullName.trim().split("\\s+", 2);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("firstName", parts[0]);
        body.put("lastName", parts.length > 1 ? parts[1] : "");
        body.put("specialization", specialization);

        HttpSupport.Response response = http.postJson(Infrastructure.mainBaseUrl() + "/trainers", body, Map.of());
        assertThat(response.status()).as("trainer registration").isEqualTo(200);

        JsonNode json = http.json(response);
        context.trainerUsername = json.get("username").asText();
        context.trainerPassword = json.get("password").asText();
    }

    @Given("a trainee {string} is registered")
    public void aTraineeIsRegistered(String fullName) {
        String[] parts = fullName.trim().split("\\s+", 2);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("firstName", parts[0]);
        body.put("lastName", parts.length > 1 ? parts[1] : "");

        HttpSupport.Response response = http.postJson(Infrastructure.mainBaseUrl() + "/trainees", body, Map.of());
        assertThat(response.status()).as("trainee registration").isEqualTo(200);

        JsonNode json = http.json(response);
        context.traineeUsername = json.get("username").asText();
        context.traineePassword = json.get("password").asText();
    }

    @Given("the trainer is logged in")
    public void theTrainerIsLoggedIn() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", context.trainerUsername);
        body.put("password", context.trainerPassword);

        HttpSupport.Response response = http.postJson(Infrastructure.mainBaseUrl() + "/auth/login", body, Map.of());
        assertThat(response.status()).as("trainer login").isEqualTo(200);
        context.token = http.json(response).get("token").asText();
    }
}
