package com.gym.service.impl;

import com.gym.dao.TraineeDao;
import com.gym.dao.TrainerDao;
import com.gym.exception.AuthenticationException;
import com.gym.exception.ValidationException;
import com.gym.model.Trainer;
import com.gym.model.Training;
import com.gym.service.TrainerService;
import com.gym.utilities.UserUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDate;
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
    public void setTraineeDao(TraineeDao traineeDao) {
        this.traineeDao = traineeDao;
    }

    @Autowired
    public void setUserUtils(UserUtils userUtils) {
        this.userUtils = userUtils;
    }

    @Override
    @Transactional
    public Trainer create(Trainer trainer) {
        log.info("Creating Trainer: {} {}", trainer.getFirstName(), trainer.getLastName());
        validateRequiredFields(trainer);

        Predicate<String> usernameExists =
                username -> trainerDao.existsByUsername(username) || traineeDao.existsByUsername(username);

        String username = userUtils.generateUsername(
                trainer.getFirstName(), trainer.getLastName(), usernameExists);
        String password = userUtils.generatePassword();

        trainer.setUsername(username);
        trainer.setPassword(password);
        trainer.setActive(true);
        trainerDao.save(trainer);
        return trainer;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean matchCredentials(String username, String password) {
        log.info("Checking credentials for trainer: {}", username);
        return trainerDao.findById(username)
                .map(t -> t.getPassword().equals(password))
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Trainer> selectTrainer(String authUsername, String authPassword, String targetUsername) {
        log.info("Selecting Trainer with username: {}", targetUsername);
        authenticate(authUsername, authPassword);
        return trainerDao.findById(targetUsername);
    }

    @Override
    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        log.info("Changing password for trainer: {}", username);
        authenticate(username, oldPassword);
        if (newPassword == null || newPassword.isBlank()) {
            throw new ValidationException("New password must not be blank");
        }
        trainerDao.updatePassword(username, newPassword);
    }

    @Override
    @Transactional
    public Trainer update(String authUsername, String authPassword, Trainer trainer) {
        log.info("Updating Trainer: {}", trainer.getUsername());
        authenticate(authUsername, authPassword);
        validateRequiredFields(trainer);
        return trainerDao.update(trainer);
    }

    @Override
    @Transactional
    public void setActive(String authUsername, String authPassword, String targetUsername, boolean active) {
        log.info("Setting isActive={} for trainer: {}", active, targetUsername);
        authenticate(authUsername, authPassword);

        Trainer trainer = trainerDao.findById(targetUsername)
                .orElseThrow(() -> new ValidationException("Trainer not found: " + targetUsername));

        if (trainer.isActive() == active) {
            throw new ValidationException(
                    "Trainer '" + targetUsername + "' is already " + (active ? "active" : "inactive"));
        }

        trainerDao.updateStatus(targetUsername, active);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Training> getTrainings(String authUsername, String authPassword,
                                       String targetUsername, LocalDate fromDate, LocalDate toDate,
                                       String traineeName) {
        log.info("Getting trainings for trainer: {}", targetUsername);
        authenticate(authUsername, authPassword);
        return trainerDao.findTrainingsByCriteria(targetUsername, fromDate, toDate, traineeName);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Trainer> findAll() {
        log.info("Finding all Trainers");
        return trainerDao.findAll();
    }

    private void authenticate(String username, String password) {
        boolean valid = trainerDao.findById(username)
                .map(t -> t.getPassword().equals(password))
                .orElse(false);
        if (!valid) {
            log.warn("Authentication failed for trainer username: {}", username);
            throw new AuthenticationException("Invalid username or password for trainer: " + username);
        }
    }

    private void validateRequiredFields(Trainer trainer) {
        if (trainer.getFirstName() == null || trainer.getFirstName().isBlank()) {
            throw new ValidationException("First name is required");
        }
        if (trainer.getLastName() == null || trainer.getLastName().isBlank()) {
            throw new ValidationException("Last name is required");
        }
        if (trainer.getSpecialization() == null) {
            throw new ValidationException("Specialization is required");
        }
    }

}
