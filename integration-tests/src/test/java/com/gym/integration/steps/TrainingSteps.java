package com.gym.integration.steps;

import com.gym.integration.env.Infrastructure;
import com.gym.integration.support.HttpSupport;
import com.gym.integration.support.ScenarioContext;
import io.cucumber.java.en.When;

import java.util.LinkedHashMap;
import java.util.Map;

public class TrainingSteps {

    private final ScenarioContext context;
    private final HttpSupport http;

    public TrainingSteps(ScenarioContext context, HttpSupport http) {
        this.context = context;
        this.http = http;
    }

    @When("the trainer creates a training named {string} on {string} for {int} minutes")
    public void theTrainerCreatesTraining(String name, String date, int minutes) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("traineeUsername", context.traineeUsername);
        body.put("trainerUsername", context.trainerUsername);
        body.put("trainingName", name);
        body.put("trainingDate", date);
        body.put("trainingDuration", minutes);

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + context.token);
        headers.put("username", context.trainerUsername);
        headers.put("password", context.trainerPassword);

        context.lastResponse = http.postJson(Infrastructure.mainBaseUrl() + "/api/trainings", body, headers);
    }

    @When("the trainee is deleted")
    public void theTraineeIsDeleted() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + context.token);
        headers.put("X-Auth-Username", context.traineeUsername);
        headers.put("X-Auth-Password", context.traineePassword);

        context.lastResponse = http.delete(
                Infrastructure.mainBaseUrl() + "/trainees/" + context.traineeUsername, headers);
    }
}
