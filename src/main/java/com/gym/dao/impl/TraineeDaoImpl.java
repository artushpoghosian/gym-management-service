package com.gym.dao.impl;

import com.gym.dao.TraineeDao;
import com.gym.model.Trainee;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@Slf4j
public class TraineeDaoImpl implements TraineeDao {

    private Map<String, Trainee> traineeStorage;

    @Autowired
    public void setTraineeStorage(@Qualifier("traineeStorage") Map<String, Trainee> traineeStorage) {
        this.traineeStorage = traineeStorage;
    }

    @Override
    public Trainee save(Trainee trainee) {
        String key = trainee.getUsername();
        log.debug("Saving trainee with username key: {}", key);
        traineeStorage.put(key, trainee);
        return trainee;
    }

    @Override
    public Trainee update(Trainee trainee) {
        String key = trainee.getUsername();
        if (traineeStorage.containsKey(key)) {
            log.debug("Updating trainee: {}", key);
            traineeStorage.put(key, trainee);
            return trainee;
        }
        return null;
    }

    @Override
    public void delete(Long id) {
        traineeStorage.values().stream()
                .filter(t -> t.getId().equals(id))
                .findFirst()
                .ifPresent(t -> {
                    log.debug("Deleting trainee: {}", t.getUsername());
                    traineeStorage.remove(t.getUsername());
                });
    }

    @Override
    public Optional<Trainee> findById(Long id) {
        log.info("Finding trainee with id: {}", id);
        return traineeStorage.values().stream().filter(t -> t.getId().equals(id)).findFirst();
    }

    @Override
    public List<Trainee> findAll() {
        log.info("Finding all trainees");
        if (traineeStorage != null) {
            return List.copyOf(traineeStorage.values());
        }
        return List.of();
    }
}
