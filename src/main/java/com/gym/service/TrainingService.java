package com.gym.service;

import com.gym.model.Training;

import java.util.List;
import java.util.Optional;

public interface TrainingService {

    Training create(Training training);
    void delete(Long id);
    List<Training> findAll();
    Optional<Training> selectTraining(Long id);
}
