package com.gym.integration.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.gym.integration.env.Infrastructure;
import com.gym.integration.support.HttpSupport;
import com.gym.integration.support.ScenarioContext;
import io.cucumber.java.en.Then;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class WorkloadSteps {

    private final ScenarioContext context;
    private final HttpSupport http;

    public WorkloadSteps(ScenarioContext context, HttpSupport http) {
        this.context = context;
        this.http = http;
    }

    @Then("the response status is {int}")
    public void theResponseStatusIs(int expectedStatus) {
        assertThat(context.lastResponse).as("an HTTP call must have been made").isNotNull();
        assertThat(context.lastResponse.status()).isEqualTo(expectedStatus);
    }

    @Then("within {int} seconds the workload service reports {int} minutes for the trainer in {int}\\/{int}")
    public void withinSecondsWorkloadReports(int seconds, int minutes, int year, int month) {
        String username = context.trainerUsername;
        String token = context.token;

        await().atMost(Duration.ofSeconds(seconds)).untilAsserted(() ->
                assertThat(minutesFor(username, token, year, month)).isEqualTo((long) minutes));
    }

    private long minutesFor(String username, String token, int year, int month) {
        HttpSupport.Response response = http.get(
                Infrastructure.workloadBaseUrl() + "/api/trainer-workload/" + username,
                Map.of("Authorization", "Bearer " + token));

        if (response.status() != 200) {
            return -1;
        }

        JsonNode years = http.json(response).path("years");
        for (JsonNode y : years) {
            if (y.path("year").asInt() == year) {
                for (JsonNode m : y.path("months")) {
                    if (m.path("month").asInt() == month) {
                        return m.path("summaryDuration").asLong();
                    }
                }
            }
        }
        return 0;
    }
}
