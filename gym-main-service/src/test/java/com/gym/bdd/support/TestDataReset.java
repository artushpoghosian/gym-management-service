package com.gym.bdd.support;

import io.cucumber.java.Before;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;

@RequiredArgsConstructor
public class TestDataReset {

    private final JdbcTemplate jdbcTemplate;
    private final PublishedWorkloadMessages publishedWorkloadMessages;

    @Before(order = 0)
    public void reset() {
        jdbcTemplate.execute(
                "TRUNCATE TABLE trainings, trainee_trainer, trainees, trainers, users RESTART IDENTITY CASCADE");
        publishedWorkloadMessages.clear();
    }
}
