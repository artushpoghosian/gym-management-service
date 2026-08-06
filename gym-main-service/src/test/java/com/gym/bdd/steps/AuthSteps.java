package com.gym.bdd.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.gym.bdd.support.ApiClient;
import com.gym.bdd.support.ScenarioContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class AuthSteps {

    @Autowired
    private ApiClient api;

    @Autowired
    private ScenarioContext context;

    @Given("the trainer is logged in")
    public void theTrainerIsLoggedIn() {
        String username = context.getTrainerUsername();
        String token = loginAndReturnToken(username, context.getPassword(username));
        context.setToken(token);
    }

    @Given("the trainee is logged in")
    public void theTraineeIsLoggedIn() {
        String username = context.getTraineeUsername();
        String token = loginAndReturnToken(username, context.getPassword(username));
        context.setToken(token);
    }

    @When("the user logs in with the trainer's generated credentials")
    public void loginWithGeneratedCredentials() {
        String username = context.getTrainerUsername();
        context.setLastResponse(login(username, context.getPassword(username)));
    }

    @When("the user logs in as that trainer with password {string}")
    public void loginWithGivenPassword(String password) {
        context.setLastResponse(login(context.getTrainerUsername(), password));
    }

    @Then("the response contains a JWT token")
    public void theResponseContainsAToken() {
        JsonNode json = api.read(context.getLastResponse());
        assertThat(json.hasNonNull("token")).as("response should carry a token").isTrue();
        assertThat(json.get("token").asText()).isNotBlank();
        context.setToken(json.get("token").asText());
    }

    private ResponseEntity<String> login(String username, String password) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", username);
        body.put("password", password);
        return api.post("/auth/login", body);
    }

    private String loginAndReturnToken(String username, String password) {
        ResponseEntity<String> response = login(username, password);
        assertThat(response.getStatusCode().value()).as("login should succeed").isEqualTo(200);
        return api.read(response).get("token").asText();
    }
}
