package com.gym.service;

import com.gym.model.Trainee;

import java.util.List;
import java.util.Optional;

public interface TraineeService {

    Trainee create(Trainee trainee);
    Trainee update(Trainee trainee);
    void delete(String username);
    List<Trainee> findAll();
    Optional<Trainee> selectTrainee(String username);
}
