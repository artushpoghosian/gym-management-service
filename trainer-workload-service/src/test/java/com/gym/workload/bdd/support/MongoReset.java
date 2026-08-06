package com.gym.workload.bdd.support;

import com.gym.workload.repository.TrainerWorkloadRepository;
import io.cucumber.java.Before;
import lombok.RequiredArgsConstructor;

/** Clears the workload collection before every scenario. */
@RequiredArgsConstructor
public class MongoReset {

    private final TrainerWorkloadRepository repository;

    @Before(order = 0)
    public void reset() {
        repository.deleteAll();
    }
}
