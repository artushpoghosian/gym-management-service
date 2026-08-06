package com.gym.workload.bdd.steps;

import com.gym.workload.bdd.support.QueueSender;
import com.gym.workload.document.MonthSummary;
import com.gym.workload.dto.ActionType;
import com.gym.workload.dto.WorkloadRequest;
import com.gym.workload.repository.TrainerWorkloadRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class WorkloadIngestSteps {

    @Autowired
    private QueueSender sender;

    @Autowired
    private TrainerWorkloadRepository repository;

    @Given("trainer {string} \\({string} {string}) already has {int} minutes in {int}\\/{int}")
    public void alreadyHas(String username, String firstName, String lastName, int minutes, int year, int month) {
        sender.sendValid(request(username, firstName, lastName, LocalDate.of(year, month, 15), minutes, ActionType.ADD));
        awaitMinutes(username, year, month, minutes, 5);
    }

    @When("an ADD workload message arrives for {string} \\({string} {string}) on {string} for {int} minutes")
    public void addArrives(String username, String firstName, String lastName, String date, int minutes) {
        sender.sendValid(request(username, firstName, lastName, LocalDate.parse(date), minutes, ActionType.ADD));
    }

    @When("a DELETE workload message arrives for {string} \\({string} {string}) on {string} for {int} minutes")
    public void deleteArrives(String username, String firstName, String lastName, String date, int minutes) {
        sender.sendValid(request(username, firstName, lastName, LocalDate.parse(date), minutes, ActionType.DELETE));
    }

    @When("an ADD workload message with an invalid token arrives for {string} \\({string} {string}) on {string} for {int} minutes")
    public void addArrivesWithInvalidToken(String username, String firstName, String lastName, String date, int minutes) {
        sender.send(request(username, firstName, lastName, LocalDate.parse(date), minutes, ActionType.ADD), "not.a.valid.jwt");
    }

    @When("a workload message with no trainer username arrives on {string} for {int} minutes")
    public void noUsernameArrives(String date, int minutes) {
        sender.sendValid(request(null, "Nora", "Fit", LocalDate.parse(date), minutes, ActionType.ADD));
    }

    @Then("within {int} seconds trainer {string} has {int} minutes in {int}\\/{int}")
    public void withinSecondsTrainerHasMinutes(int seconds, String username, int minutes, int year, int month) {
        awaitMinutes(username, year, month, minutes, seconds);
    }

    @Then("trainer {string} has no workload document")
    public void trainerHasNoDocument(String username) {
        await().during(Duration.ofMillis(500)).atMost(Duration.ofSeconds(2))
                .until(() -> repository.findByUsername(username).isEmpty());
    }

    @Then("no workload documents are stored")
    public void noWorkloadDocumentsStored() {
        await().during(Duration.ofMillis(500)).atMost(Duration.ofSeconds(2))
                .until(() -> repository.count() == 0);
    }

    private void awaitMinutes(String username, int year, int month, int minutes, int seconds) {
        await().atMost(Duration.ofSeconds(seconds)).untilAsserted(() ->
                assertThat(monthDuration(username, year, month)).isEqualTo((long) minutes));
    }

    private long monthDuration(String username, int year, int month) {
        return repository.findByUsername(username).stream()
                .flatMap(w -> w.getYears().stream())
                .filter(y -> y.getYear() == year)
                .flatMap(y -> y.getMonths().stream())
                .filter(m -> m.getMonth() == month)
                .mapToLong(MonthSummary::getSummaryDuration)
                .findFirst()
                .orElse(0L);
    }

    private WorkloadRequest request(String username, String firstName, String lastName,
                                    LocalDate date, int minutes, ActionType action) {
        WorkloadRequest request = new WorkloadRequest();
        request.setTrainerUsername(username);
        request.setTrainerFirstName(firstName);
        request.setTrainerLastName(lastName);
        request.setActive(true);
        request.setTrainingDate(date);
        request.setTrainingDurationMinutes(minutes);
        request.setActionType(action);
        return request;
    }
}
