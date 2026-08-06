package com.gym.bdd.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.gym.bdd.support.ApiClient;
import com.gym.bdd.support.ScenarioContext;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class AuthSteps {

    @Autowired
    private ApiClient api;

    @Autowired
    private ScenarioContext context;

    @When("the user logs in with the trainer's generated credentials")
    public void loginWithGeneratedCredentials() {
        String username = context.getLastUsername();
        login(username, context.getPassword(username));
    }

    @When("the user logs in as that trainer with password {string}")
    public void loginWithGivenPassword(String password) {
        login(context.getLastUsername(), password);
    }

    @Then("the response contains a JWT token")
    public void theResponseContainsAToken() {
        JsonNode json = api.read(context.getLastResponse());
        assertThat(json.hasNonNull("token")).as("response should carry a token").isTrue();
        assertThat(json.get("token").asText()).isNotBlank();
        context.setToken(json.get("token").asText());
    }

    private void login(String username, String password) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", username);
        body.put("password", password);
        context.setLastResponse(api.post("/auth/login", body));
    }
}
