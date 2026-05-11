package com.gym.dao;

import com.gym.model.Trainee;

import java.util.List;
import java.util.Optional;

public interface TraineeDao {
    Trainee save(Trainee trainee);
    Trainee update(Trainee trainee);
    void delete(Long id);
    Optional<Trainee> findById(Long id);
    List<Trainee> findAll();
}
