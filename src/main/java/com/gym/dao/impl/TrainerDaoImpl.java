package com.gym.dao.impl;

import com.gym.dao.TrainerDao;
import com.gym.model.Trainer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@Slf4j
public class TrainerDaoImpl implements TrainerDao {

    private Map<String, Trainer> trainerStorage;

    @Autowired
    public void setTraineeStorage(@Qualifier("trainerStorage") Map<String, Trainer> trainerStorage) {
        this.trainerStorage = trainerStorage;
    }

    @Override
    public Trainer save(Trainer trainer) {
        String key = trainer.getUsername();
        log.debug("Saving trainer with username key: {}", key);
        trainerStorage.put(key, trainer);
        return trainer;
    }

    @Override
    public Trainer update(Trainer trainer) {
        String key = trainer.getUsername();
        if (trainerStorage.containsKey(key)) {
            log.debug("Updating trainer: {}", key);
            trainerStorage.put(key, trainer);
            return trainer;
        }
        return null;
    }

    @Override
    public void delete(Long id) {
        String key = trainerStorage.values().stream().filter(t -> t.getId().equals(id)).findFirst().get().getUsername();
        log.debug("Deleting trainer with username key: {}", key);
        trainerStorage.remove(key);
    }

    @Override
    public Optional<Trainer> findById(Long id) {
        log.info("Finding trainer with id: {}", id);
        return trainerStorage.values().stream().filter(t -> t.getId().equals(id)).findFirst();
    }

    @Override
    public List<Trainer> findAll() {
        log.info("Finding all trainers");
        if (trainerStorage != null) {
            return List.copyOf(trainerStorage.values());
        }
        return List.of();
    }
}
