package com.gym.service.impl;

import com.gym.dao.TraineeDao;
import com.gym.dao.TrainerDao;
import com.gym.exception.AuthenticationException;
import com.gym.exception.ValidationException;
import com.gym.model.Trainee;
import com.gym.model.Trainer;
import com.gym.model.Training;
import com.gym.model.TrainingType;
import com.gym.service.TraineeService;
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

        validateRequiredFields(trainee);

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
    @Transactional(readOnly = true)
    public boolean matchCredentials(String username, String password) {
        log.info("Checking credentials for trainee: {}", username);
        return traineeDao.findById(username)
                .map(t -> t.getPassword().equals(password))
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Trainee> selectTrainee(String authUsername, String authPassword, String targetUsername) {
        log.info("Selecting Trainee with username: {}", targetUsername);
        authenticate(authUsername, authPassword);
        return traineeDao.findById(targetUsername);
    }

    @Override
    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        log.info("Changing password for trainee: {}", username);
        authenticate(username, oldPassword);
        if (newPassword == null || newPassword.isBlank()) {
            throw new ValidationException("New password must not be blank");
        }
        traineeDao.updatePassword(username, newPassword);
    }

    @Override
    @Transactional
    public Trainee update(String authUsername, String authPassword, Trainee trainee) {
        log.info("Updating Trainee: {}", trainee.getUsername());
        authenticate(authUsername, authPassword);
        validateRequiredFields(trainee);
        return traineeDao.update(trainee);
    }

    @Override
    @Transactional
    public void setActive(String authUsername, String authPassword, String targetUsername, boolean active) {
        log.info("Setting isActive={} for trainee: {}", active, targetUsername);
        authenticate(authUsername, authPassword);

        Trainee trainee = traineeDao.findById(targetUsername)
                .orElseThrow(() -> new ValidationException("Trainee not found: " + targetUsername));

        if (trainee.isActive() == active) {
            throw new ValidationException(
                    "Trainee '" + targetUsername + "' is already " + (active ? "active" : "inactive"));
        }

        traineeDao.updateStatus(targetUsername, active);
    }

    @Override
    @Transactional
    public void delete(String authUsername, String authPassword, String targetUsername) {
        log.info("Deleting Trainee: {}", targetUsername);
        authenticate(authUsername, authPassword);
        traineeDao.delete(targetUsername);
    }

    @Transactional(readOnly = true)
    public List<Training> getTrainings(String authUsername, String authPassword,
                                       String targetUsername, LocalDate fromDate, LocalDate toDate,
                                       String trainerName, TrainingType trainingType) {
        log.info("Getting trainings for trainee: {}", targetUsername);
        authenticate(authUsername, authPassword);

        String trainingTypeName = trainingType != null ? trainingType.name() : null;
        return traineeDao.findTrainingsByCriteria(
                targetUsername, fromDate, toDate, trainerName, trainingTypeName);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Trainer> getUnassignedTrainers(String authUsername, String authPassword, String targetUsername) {
        log.info("Getting unassigned trainers for trainee: {}", targetUsername);
        authenticate(authUsername, authPassword);
        return traineeDao.findTrainersNotAssignedToTrainee(targetUsername);
    }

    @Override
    @Transactional
    public Trainee updateTrainersList(String authUsername, String authPassword,
                                      String targetUsername, List<String> trainerUsernames) {
        log.info("Updating trainers list for trainee: {}", targetUsername);
        authenticate(authUsername, authPassword);

        traineeDao.findById(targetUsername)
                .orElseThrow(() -> new ValidationException("Trainee not found: " + targetUsername));

        List<Trainer> resolvedTrainers = trainerUsernames.stream()
                .map(u -> trainerDao.findById(u)
                        .orElseThrow(() -> new ValidationException("Trainer not found: " + u)))
                .toList();

        traineeDao.updateTraineeTrainers(targetUsername, resolvedTrainers);

        return traineeDao.findById(targetUsername).orElseThrow();
    }

    @Override
    public List<Trainee> findAll() {
        log.info("Finding all Trainees");
        return traineeDao.findAll();
    }

    private void authenticate(String username, String password) {
        boolean valid = traineeDao.findById(username)
                .map(t -> t.getPassword().equals(password))
                .orElse(false);
        if (!valid) {
            log.warn("Authentication failed for trainee username: {}", username);
            throw new AuthenticationException("Invalid username or password for trainee: " + username);
        }
    }

    private void validateRequiredFields(Trainee trainee) {
        if (trainee.getFirstName() == null || trainee.getFirstName().isBlank()) {
            throw new ValidationException("First name is required");
        }
        if (trainee.getLastName() == null || trainee.getLastName().isBlank()) {
            throw new ValidationException("Last name is required");
        }
    }
}
