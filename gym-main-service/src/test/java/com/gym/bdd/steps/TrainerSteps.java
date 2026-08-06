package com.gym.bdd.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.gym.bdd.support.ApiClient;
import com.gym.bdd.support.ScenarioContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
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

        Map<String, Object> body = trainerBody(parts[0], parts.length > 1 ? parts[1] : "", specialization);
        ResponseEntity<String> response = api.post("/trainers", body);
        assertThat(response.getStatusCode().value())
                .as("trainer registration should succeed")
                .isEqualTo(200);

        JsonNode json = api.read(response);
        String username = json.get("username").asText();
        context.rememberCredentials(username, json.get("password").asText());
        context.setTrainerUsername(username);
    }

    @When("a trainer is registered with first name {string} and last name {string} and specialization {string}")
    public void aTrainerIsRegisteredWith(String firstName, String lastName, String specialization) {
        context.setLastResponse(api.post("/trainers", trainerBody(firstName, lastName, specialization)));
    }

    @When("a trainer is registered without a specialization")
    public void aTrainerIsRegisteredWithoutSpecialization() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("firstName", "Nora");
        body.put("lastName", "Fit");
        context.setLastResponse(api.post("/trainers", body));
    }

    private Map<String, Object> trainerBody(String firstName, String lastName, String specialization) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("firstName", firstName);
        body.put("lastName", lastName);
        body.put("specialization", specialization);
        return body;
    }
}
