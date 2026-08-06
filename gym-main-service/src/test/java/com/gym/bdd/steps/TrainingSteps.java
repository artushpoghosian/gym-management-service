package com.gym.bdd.steps;

import com.gym.bdd.support.ApiClient;
import com.gym.bdd.support.PublishedWorkloadMessages;
import com.gym.bdd.support.ScenarioContext;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class TrainingSteps {

    @Autowired
    private ApiClient api;

    @Autowired
    private ScenarioContext context;

    @Autowired
    private PublishedWorkloadMessages published;

    @When("the trainer creates a training named {string} on {string} for {int} minutes")
    public void theTrainerCreatesTraining(String name, String date, int minutes) {
        createTraining(name, date, minutes, context.getTraineeUsername());
    }

    @When("the trainer creates a training named {string} on {string} for {int} minutes for an unknown trainee")
    public void theTrainerCreatesTrainingForUnknownTrainee(String name, String date, int minutes) {
        createTraining(name, date, minutes, "ghost.trainee");
    }

    @Then("a workload {word} message is published for the trainer with {int} minutes on {string}")
    public void aWorkloadMessageIsPublished(String action, int minutes, String date) {
        String trainer = context.getTrainerUsername();
        LocalDate day = LocalDate.parse(date);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(published.all())
                        .as("a %s workload message for %s", action, trainer)
                        .anyMatch(m -> m.getActionType().name().equalsIgnoreCase(action)
                                && m.getTrainerUsername().equals(trainer)
                                && m.getTrainingDurationMinutes() == minutes
                                && m.getTrainingDate().equals(day)));
    }

    @Then("no workload message is published")
    public void noWorkloadMessageIsPublished() {
        await().during(Duration.ofMillis(500)).atMost(Duration.ofSeconds(2))
                .until(() -> published.all().isEmpty());
    }

    private void createTraining(String name, String date, int minutes, String traineeUsername) {
        String trainer = context.getTrainerUsername();

        HttpHeaders headers = api.bearer(context.getToken());
        headers.add("username", trainer);
        headers.add("password", context.getPassword(trainer));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("traineeUsername", traineeUsername);
        body.put("trainerUsername", trainer);
        body.put("trainingName", name);
        body.put("trainingDate", date);
        body.put("trainingDuration", minutes);

        context.setLastResponse(api.post("/api/trainings", body, headers));
    }
}
