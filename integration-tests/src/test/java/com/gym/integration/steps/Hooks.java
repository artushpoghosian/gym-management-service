package com.gym.integration.steps;

import com.gym.integration.env.Infrastructure;
import io.cucumber.java.BeforeAll;

public class Hooks {

    @BeforeAll
    public static void startInfrastructure() {
        Infrastructure.start();
    }
}
