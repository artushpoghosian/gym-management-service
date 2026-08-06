package com.gym.bdd.steps;

import com.gym.bdd.support.ApiClient;
import com.gym.bdd.support.JwtTestHelper;
import com.gym.bdd.support.ScenarioContext;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

public class SecuritySteps {

    private static final String PROTECTED_PATH = "/trainees/someone";

    @Autowired
    private ApiClient api;

    @Autowired
    private ScenarioContext context;

    @Autowired
    private JwtTestHelper jwtTestHelper;

    @When("a protected endpoint is called without a token")
    public void calledWithoutToken() {
        context.setLastResponse(api.get(PROTECTED_PATH, new HttpHeaders()));
    }

    @When("a protected endpoint is called with a tampered token")
    public void calledWithTamperedToken() {
        context.setLastResponse(api.get(PROTECTED_PATH, api.bearer("not.a.valid.jwt")));
    }

    @When("a protected endpoint is called with an expired token")
    public void calledWithExpiredToken() {
        context.setLastResponse(api.get(PROTECTED_PATH, api.bearer(jwtTestHelper.expiredToken("someone"))));
    }
}
