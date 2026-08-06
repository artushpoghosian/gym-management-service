package com.gym.bdd.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.gym.bdd.support.ApiClient;
import com.gym.bdd.support.ScenarioContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class TraineeSteps {

    @Autowired
    private ApiClient api;

    @Autowired
    private ScenarioContext context;

    @Given("a trainee {string} is registered")
    public void aTraineeIsRegistered(String fullName) {
        String[] parts = fullName.trim().split("\\s+", 2);

        Map<String, Object> body = traineeBody(parts[0], parts.length > 1 ? parts[1] : "");
        ResponseEntity<String> response = api.post("/trainees", body);
        assertThat(response.getStatusCode().value())
                .as("trainee registration should succeed")
                .isEqualTo(200);

        JsonNode json = api.read(response);
        String username = json.get("username").asText();
        context.rememberCredentials(username, json.get("password").asText());
        context.setTraineeUsername(username);
    }

    @When("a trainee is registered with first name {string} and last name {string}")
    public void aTraineeIsRegisteredWith(String firstName, String lastName) {
        context.setLastResponse(api.post("/trainees", traineeBody(firstName, lastName)));
    }

    @When("the trainee requests their own profile")
    public void theTraineeRequestsOwnProfile() {
        context.setLastResponse(api.get("/trainees/" + context.getTraineeUsername(), traineeAuthHeaders()));
    }

    @When("the trainee requests the profile of {string}")
    public void theTraineeRequestsProfileOf(String targetUsername) {
        context.setLastResponse(api.get("/trainees/" + targetUsername, traineeAuthHeaders()));
    }

    @When("the trainee is deleted")
    public void theTraineeIsDeleted() {
        context.setLastResponse(api.delete("/trainees/" + context.getTraineeUsername(), traineeAuthHeaders()));
    }

    private HttpHeaders traineeAuthHeaders() {
        String username = context.getTraineeUsername();
        HttpHeaders headers = api.bearer(context.getToken());
        headers.add("X-Auth-Username", username);
        headers.add("X-Auth-Password", context.getPassword(username));
        return headers;
    }

    private Map<String, Object> traineeBody(String firstName, String lastName) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("firstName", firstName);
        body.put("lastName", lastName);
        return body;
    }
}
