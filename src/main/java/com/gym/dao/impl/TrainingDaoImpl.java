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
    public void setTraineeStorage(@Qualifier("trainingStorage") Map<String, Training> trainingStorage) {
        this.trainingStorage = trainingStorage;
    }

    @Override
    public Training save(Training training) {
        String key = training.getTrainingName();
        log.debug("Saving training with name key: {}", key);
        trainingStorage.put(key, training);
        return training;
    }

    @Override
    public Training update(Training training) {
        String key = training.getTrainingName();
        if (trainingStorage.containsKey(key)) {
            log.debug("Updating training key: {}", key);
            trainingStorage.put(key, training);
            return training;
        }
        return null;
    }

    @Override
    public void delete(Long id) {
        String key = trainingStorage.values().stream().filter(t -> t.getId().equals(id)).findFirst().get().getTrainingName();
        log.debug("Deleting training with name key: {}", key);
        trainingStorage.remove(key);
    }

    @Override
    public Optional<Training> findById(Long id) {
        log.info("Finding training with id: {}", id);
        return trainingStorage.values().stream().filter(t -> t.getId().equals(id)).findFirst();
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
