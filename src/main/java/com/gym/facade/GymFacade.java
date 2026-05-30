package com.gym.facade;

import com.gym.model.Trainee;
import com.gym.model.Trainer;
import com.gym.model.Training;
import com.gym.model.TrainingType;
import com.gym.service.TraineeService;
import com.gym.service.TrainerService;
import com.gym.service.TrainingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class GymFacade {

    private final TrainerService trainerService;
    private final TraineeService traineeService;
    private final TrainingService trainingService;

    public Trainee createTrainee(Trainee trainee) {
        log.info("Facade: creating trainee");
        return traineeService.create(trainee);
    }

    public boolean traineeMatchCredentials(String username, String password) {
        log.info("Facade: matching credentials for trainee: {}", username);
        return traineeService.matchCredentials(username, password);
    }

    public Trainee updateTrainee(String authUsername, String authPassword, Trainee trainee) {
        log.info("Facade: updating trainee: {}", trainee.getUsername());
        return traineeService.update(authUsername, authPassword, trainee);
    }

    public void setTraineeActive(String authUsername, String authPassword, String targetUsername, boolean active) {
        log.info("Facade: setting trainee active={} for: {}", active, targetUsername);
        traineeService.setActive(authUsername, authPassword, targetUsername, active);
    }

    public List<Training> getTraineeTrainings(String authUsername, String authPassword,
                                             String targetUsername, LocalDate fromDate, LocalDate toDate,
                                             String trainerName, TrainingType trainingType) {
        log.info("Facade: getting trainings for trainee: {}", targetUsername);
        return traineeService.getTrainings(authUsername, authPassword,
                targetUsername, fromDate, toDate, trainerName, trainingType);
    }

    public void deleteTrainee(String authUsername, String authPassword, String targetUsername) {
        log.info("Facade: deleting trainee: {}", targetUsername);
        traineeService.delete(authUsername, authPassword, targetUsername);
    }

    public Optional<Trainee> selectTrainee(String authUsername, String authPassword, String targetUsername) {
        log.info("Facade: selecting trainee: {}", targetUsername);
        return traineeService.selectTrainee(authUsername, authPassword, targetUsername);
    }

    public void changeTraineePassword(String username, String oldPassword, String newPassword) {
        log.info("Facade: changing password for trainee: {}", username);
        traineeService.changePassword(username, oldPassword, newPassword);
    }

    public List<Trainee> findAllTrainees() {
        return traineeService.findAll();
    }

    public Trainer createTrainer(Trainer trainer) {
        log.info("Facade: creating trainer");
        return trainerService.create(trainer);
    }

    public boolean trainerMatchCredentials(String username, String password) {
        log.info("Facade: matching credentials for trainer: {}", username);
        return trainerService.matchCredentials(username, password);
    }

    public Trainer updateTrainer(String authUsername, String authPassword, Trainer trainer) {
        log.info("Facade: updating trainer: {}", trainer.getUsername());
        return trainerService.update(authUsername, authPassword, trainer);
    }

    public void setTrainerActive(String authUsername, String authPassword, String targetUsername, boolean active) {
        log.info("Facade: setting trainer active={} for: {}", active, targetUsername);
        trainerService.setActive(authUsername, authPassword, targetUsername, active);
    }

    public List<Training> getTrainerTrainings(String authUsername, String authPassword,
                                              String targetUsername, LocalDate fromDate, LocalDate toDate,
                                              String traineeName) {
        log.info("Facade: getting trainings for trainer: {}", targetUsername);
        return trainerService.getTrainings(authUsername, authPassword,
                targetUsername, fromDate, toDate, traineeName);
    }

    public void changeTrainerPassword(String username, String oldPassword, String newPassword) {
        log.info("Facade: changing password for trainer: {}", username);
        trainerService.changePassword(username, oldPassword, newPassword);
    }

    public List<Trainer> findAllTrainers() {
        return trainerService.findAll();
    }

    public Training createTraining(String authUsername, String authPassword, Training training) {
        log.info("Facade: creating training: {}", training.getTrainingName());
        return trainingService.create(authUsername, authPassword, training);
    }

    public List<Trainer> getUnassignedTrainers(String authUsername, String authPassword, String traineeUsername) {
        log.info("Facade: getting unassigned trainers for trainee: {}", traineeUsername);
        return traineeService.getUnassignedTrainers(authUsername, authPassword, traineeUsername);
    }

    public Trainee updateTraineeTrainersList(String authUsername, String authPassword,
                                             String traineeUsername, List<String> trainerUsernames) {
        log.info("Facade: updating trainers list for trainee: {}", traineeUsername);
        return traineeService.updateTrainersList(authUsername, authPassword, traineeUsername, trainerUsernames);
    }

    public List<Training> findAllTrainings() {
        return trainingService.findAll();
    }
}
