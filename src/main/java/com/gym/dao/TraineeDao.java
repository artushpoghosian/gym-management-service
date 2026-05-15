package com.gym.dao;

import com.gym.model.Trainee;

import java.util.List;
import java.util.Optional;

public interface TraineeDao {
    Trainee save(Trainee trainee);
    Trainee update(Trainee trainee);
    void delete(String username);
    Optional<Trainee> findById(String username);
    List<Trainee> findAll();
    boolean existsByUsername(String username);
}
