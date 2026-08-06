package com.gym.bdd.support;

import io.cucumber.java.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class TestDataReset {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Before(order = 0)
    public void resetDatabase() {
        jdbcTemplate.execute(
                "TRUNCATE TABLE trainings, trainee_trainer, trainees, trainers, users RESTART IDENTITY CASCADE");
    }
}
