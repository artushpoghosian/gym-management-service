package com.gym.workload.bdd.support;

import io.cucumber.spring.ScenarioScope;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@ScenarioScope
@Getter
@Setter
public class ScenarioContext {

    private String token;
    private ResponseEntity<String> lastResponse;
}
