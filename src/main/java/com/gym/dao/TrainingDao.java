package com.gym.dao;

import com.gym.model.Training;

import java.util.List;
import java.util.Optional;

public interface TrainingDao {
    Training save(Training training);
    Training update(Training training);
    void delete(Long id);
    Optional<Training> findById(Long id);
    List<Training> findAll();
}
