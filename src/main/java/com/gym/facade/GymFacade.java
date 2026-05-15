package com.gym.facade;

import com.gym.model.Trainee;
import com.gym.model.Trainer;
import com.gym.model.Training;
import com.gym.service.TraineeService;
import com.gym.service.TrainerService;
import com.gym.service.TrainingService;
import com.gym.utilities.UserUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class GymFacade {

    private final TrainerService trainerService;
    private final TraineeService traineeService;
    private final TrainingService trainingService;
    private final UserUtils userUtils;


    public Trainee createTrainee(Trainee trainee) {
        log.info("Facade: creating trainee");
        return traineeService.create(trainee);
    }

    public Trainee updateTrainee(Trainee trainee) {
        log.info("Facade: updating trainee");
        return traineeService.update(trainee);
    }

    public void deleteTrainee(String username) {
        log.info("Facade: deleting trainee with username: {}", username);
        traineeService.delete(username);
    }

    public Optional<Trainee> selectTrainee(String username) {
        log.info("Facade: selecting trainee with username: {}", username);
        return traineeService.selectTrainee(username);
    }

    public List<Trainee> findAllTrainees() {
        return traineeService.findAll();
    }

    public Trainer createTrainer(Trainer trainer) {
        log.info("Facade: creating trainer");
        return trainerService.create(trainer);
    }

    public Trainer updateTrainer(Trainer trainer) {
        log.info("Facade: updating trainer");
        return trainerService.update(trainer);
    }

    public Optional<Trainer> selectTrainer(String username) {
        log.info("Facade: selecting trainer with Username: {}", username);
        return trainerService.selectTrainer(username);
    }

    public List<Trainer> findAllTrainers() {
        return trainerService.findAll();
    }

    public Training createTraining(Training training) {
        log.info("Facade: creating training");
        return trainingService.create(training);
    }

    public List<Training> findAllTrainings() {
        return trainingService.findAll();
    }
}
