package com.gym.dao.impl;

import com.gym.dao.TrainingDao;
import com.gym.model.Training;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@Slf4j
public class TrainingDaoImpl implements TrainingDao {

    private Map<String, Training> trainingStorage;

    @Autowired
    public void setTrainingStorage(@Qualifier("trainingStorage") Map<String, Training> trainingStorage) {
        this.trainingStorage = trainingStorage;
    }

    @Override
    public Training save(Training training) {
        String trainingName = training.getTrainingName();
        log.debug("Saving training with name key: {}", trainingName);
        trainingStorage.put(trainingName, training);
        return training;
    }

    @Override
    public Optional<Training> findByName(String trainingName) {
        log.info("Finding training with name: {}", trainingName);
        return Optional.ofNullable(trainingStorage.get(trainingName));
    }

    @Override
    public List<Training> findAll() {
        log.info("Finding all trainings");
        if (trainingStorage != null) {
            return List.copyOf(trainingStorage.values());
        }
        return List.of();
    }
}
