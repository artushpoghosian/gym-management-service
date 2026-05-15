package com.gym.service.impl;

import com.gym.dao.TraineeDao;
import com.gym.dao.TrainerDao;
import com.gym.model.Trainer;
import com.gym.service.TrainerService;
import com.gym.utilities.UserUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrainerServiceImpl implements TrainerService {

    private TrainerDao trainerDao;
    private TraineeDao traineeDao;
    private UserUtils userUtils;

    @Autowired
    public void setTrainerDao(TrainerDao trainerDao) {
        this.trainerDao = trainerDao;
    }

    @Autowired
    public void setTrainerDao(TraineeDao traineeDao) {
        this.traineeDao = traineeDao;
    }

    @Autowired
    public void setUserUtils(UserUtils userUtils) {
        this.userUtils = userUtils;
    }


    @Override
    public Trainer create(Trainer trainer) {
        log.info("Creating Trainer: {} {}", trainer.getFirstName(), trainer.getLastName());

        Predicate<String> usernameExists = username ->
                trainerDao.existsByUsername(username) || traineeDao.existsByUsername(username);

        String username = userUtils.generateUsername(trainer.getFirstName(), trainer.getLastName(), usernameExists);
        String password = userUtils.generatePassword();

        trainer.setUsername(username);
        trainer.setPassword(password);
        trainer.setActive(true);
        return trainerDao.save(trainer);
    }

    @Override
    public Trainer update(Trainer trainer) {
        log.info("Updating Trainer profile with Username: {}", trainer.getUsername());
        return trainerDao.update(trainer);
    }

    @Override
    public List<Trainer> findAll() {
        log.info("Finding all Trainers");
        return trainerDao.findAll();
    }

    @Override
    public Optional<Trainer> selectTrainer(String username) {
        log.info("Selecting Trainer with Username: {}", username);
        return trainerDao.findById(username);
    }
}
