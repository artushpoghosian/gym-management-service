package com.gym.bdd.steps;

import com.gym.bdd.support.ScenarioContext;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class CommonSteps {

    @Autowired
    private ScenarioContext context;

    @Then("the response status is {int}")
    public void theResponseStatusIs(int expectedStatus) {
        assertThat(context.getLastResponse()).as("an HTTP call must have been made").isNotNull();
        assertThat(context.getLastResponse().getStatusCode().value()).isEqualTo(expectedStatus);
    }
}
