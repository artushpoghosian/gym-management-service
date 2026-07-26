package com.gym.workload.repository;

import com.gym.workload.document.TrainerWorkload;

import java.util.Optional;

public interface TrainerWorkloadRepositoryCustom {

    Optional<TrainerWorkload> findByUsername(String username);

    void applyWorkload(String username, String firstName, String lastName, boolean active,
                       int year, int month, long minutes, boolean subtract);
}
