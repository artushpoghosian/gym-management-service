package com.gym.service;

import com.gym.model.Training;

import java.util.List;
import java.util.Optional;

public interface TrainingService {

    Training create(String authUsername, String authPassword, Training training);
    Optional<Training> findByName(String trainingName);
    List<Training> findAll();
    Optional<Training> selectTraining(String trainingName);
}
