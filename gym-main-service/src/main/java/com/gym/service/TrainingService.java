package com.gym.service;

import com.gym.model.Training;

import java.util.List;
import java.util.Optional;

public interface TrainingService {

    Training create(String authUsername, String authPassword, Training training);
    void delete(String authUsername, String authPassword, String trainingName);
    Optional<Training> findByName(String trainingName);
    List<Training> findAll();
    Optional<Training> selectTraining(String trainingName);
}
