package com.gym.dao;

import com.gym.model.Trainer;

import java.util.List;
import java.util.Optional;

public interface TrainerDao {
    Trainer save(Trainer trainer);
    Trainer update(Trainer trainer);
    void delete(String username);
    Optional<Trainer> findById(String username);
    List<Trainer> findAll();
    boolean existsByUsername(String username);
}
