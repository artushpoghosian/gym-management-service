package com.gym.workload.repository;

import com.gym.workload.document.TrainerWorkload;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TrainerWorkloadRepository
        extends MongoRepository<TrainerWorkload, String>, TrainerWorkloadRepositoryCustom {

    List<TrainerWorkload> findByFirstNameAndLastName(String firstName, String lastName);
}
