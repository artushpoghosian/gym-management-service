package com.gym.workload.bdd.steps;

import com.gym.workload.bdd.support.ApiClient;
import com.gym.workload.bdd.support.JwtMinter;
import com.gym.workload.bdd.support.ScenarioContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

public class WorkloadQuerySteps {

    private static final String SUMMARY_PATH = "/api/trainer-workload/";

    @Autowired
    private ApiClient api;

    @Autowired
    private ScenarioContext context;

    @Autowired
    private JwtMinter jwtMinter;

    @Given("a valid service token")
    public void aValidServiceToken() {
        context.setToken(jwtMinter.validToken("gym-management-service"));
    }

    @When("the workload summary for {string} is requested")
    public void theSummaryIsRequested(String username) {
        context.setLastResponse(api.get(SUMMARY_PATH + username, api.bearer(context.getToken())));
    }

    @When("the workload summary for {string} is requested without a token")
    public void theSummaryIsRequestedWithoutToken(String username) {
        context.setLastResponse(api.get(SUMMARY_PATH + username, new HttpHeaders()));
    }

    @When("the workload summary for {string} is requested with a tampered token")
    public void theSummaryIsRequestedWithTamperedToken(String username) {
        context.setLastResponse(api.get(SUMMARY_PATH + username, api.bearer("not.a.valid.jwt")));
    }
}
