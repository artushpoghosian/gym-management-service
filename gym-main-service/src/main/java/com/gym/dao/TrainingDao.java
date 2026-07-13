package com.gym.dao;

import com.gym.model.Training;

import java.util.List;
import java.util.Optional;

public interface TrainingDao {
    Training save(Training training);
    List<Training> findAll();
    Optional<Training> findByName(String trainingName);
    void delete(Training training);
}
