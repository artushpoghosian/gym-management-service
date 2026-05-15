package com.gym.service;

import com.gym.model.Trainer;

import java.util.List;
import java.util.Optional;

public interface TrainerService {

    Trainer create(Trainer trainer);
    Trainer update(Trainer trainer);
    List<Trainer> findAll();
    Optional<Trainer> selectTrainer(String username);
}
