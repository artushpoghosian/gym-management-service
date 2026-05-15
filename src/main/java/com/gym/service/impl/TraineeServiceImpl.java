package com.gym.service.impl;

import com.gym.dao.TraineeDao;
import com.gym.dao.TrainerDao;
import com.gym.model.Trainee;
import com.gym.service.TraineeService;
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
public class TraineeServiceImpl implements TraineeService {

    private TraineeDao traineeDao;
    private TrainerDao trainerDao;
    private UserUtils userUtils;

    @Autowired
    public void setTraineeDao(TraineeDao traineeDao) {
        this.traineeDao = traineeDao;
    }

    @Autowired
    public void setUserUtils(UserUtils userUtils) {
        this.userUtils = userUtils;
    }

    @Autowired
    public void setTrainerDao(TrainerDao trainerDao) {
        this.trainerDao = trainerDao;
    }

    @Override
    public Trainee create(Trainee trainee) {
        log.info("Creating Trainee: {} {}", trainee.getFirstName(), trainee.getLastName());

        Predicate<String> usernameExists = username ->
                traineeDao.existsByUsername(username) || trainerDao.existsByUsername(username);

        String username = userUtils.generateUsername(trainee.getFirstName(), trainee.getLastName(), usernameExists);
        String password = userUtils.generatePassword();

        trainee.setUsername(username);
        trainee.setPassword(password);
        trainee.setActive(true);
        traineeDao.save(trainee);
        return trainee;
    }

    @Override
    public Trainee update(Trainee trainee) {
        log.info("Updating Trainee profile with Username: {}", trainee.getUsername());
        return traineeDao.update(trainee);
    }

    @Override
    public void delete(String username) {
        log.info("Deleting Trainee with username: {}", username);
        traineeDao.delete(username);
    }

    @Override
    public List<Trainee> findAll() {
        log.info("Finding all Trainees");
        return traineeDao.findAll();
    }

    @Override
    public Optional<Trainee> selectTrainee(String username) {
        log.info("Selecting Trainee with Username: {}", username);
        return traineeDao.findById(username);
    }
}
