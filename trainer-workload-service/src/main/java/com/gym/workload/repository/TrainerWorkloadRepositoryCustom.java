package com.gym.workload.repository;

import com.gym.workload.document.TrainerWorkload;

import java.util.Optional;

public interface TrainerWorkloadRepositoryCustom {

    Optional<TrainerWorkload> findByUsername(String username);

    void applyWorkload(TrainerInfo trainer, WorkloadDelta delta);
}
