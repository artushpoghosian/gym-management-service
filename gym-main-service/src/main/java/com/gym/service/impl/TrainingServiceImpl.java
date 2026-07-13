package com.gym.service.impl;

import com.gym.client.ActionType;
import com.gym.client.WorkloadClient;
import com.gym.client.WorkloadRequest;
import com.gym.dao.TraineeDao;
import com.gym.dao.TrainerDao;
import com.gym.dao.TrainingDao;
import com.gym.exception.AuthenticationException;
import com.gym.exception.ValidationException;
import com.gym.model.Training;
import com.gym.service.TrainingService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrainingServiceImpl implements TrainingService {

    private final TrainingDao trainingDao;
    private final TraineeDao traineeDao;
    private final TrainerDao trainerDao;
    private final MeterRegistry meterRegistry;
    private final WorkloadClient workloadClient;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public Training create(String authUsername, String authPassword, Training training) {
        log.info("[OP] createTraining called: name={}", training.getTrainingName());
        authenticate(authUsername, authPassword);
        validateRequiredFields(training);
        Training saved = trainingDao.save(training);

        meterRegistry.counter("gym.trainings.created").increment();
        meterRegistry.counter("gym.trainings.created.by.type",
                "trainingType", saved.getTrainingType().name()
        ).increment();
        log.debug("Incremented training-created metrics for type: {}", saved.getTrainingType());

        notifyWorkload(saved, ActionType.ADD);

        log.info("[OP] createTraining completed: trainingId={} trainer={}",
                saved.getId(), saved.getTrainer().getUsername());
        return saved;
    }

    @Override
    @Transactional
    public void delete(String authUsername, String authPassword, String trainingName) {
        log.info("[OP] deleteTraining called: name={}", trainingName);
        authenticate(authUsername, authPassword);

        Training training = trainingDao.findByName(trainingName)
                .orElseThrow(() -> new ValidationException("Training not found: " + trainingName));

        trainingDao.delete(training);

        notifyWorkload(training, ActionType.DELETE);

        log.info("[OP] deleteTraining completed: name={} trainer={}",
                trainingName, training.getTrainer().getUsername());
    }

    private void notifyWorkload(Training training, ActionType actionType) {
        WorkloadRequest req = WorkloadRequest.builder()
                .trainerUsername(training.getTrainer().getUsername())
                .trainerFirstName(training.getTrainer().getFirstName())
                .trainerLastName(training.getTrainer().getLastName())
                .isActive(training.getTrainer().isActive())
                .trainingDate(training.getTrainingDate())
                .trainingDurationMinutes(training.getTrainingDuration())
                .actionType(actionType)
                .build();

        workloadClient.send(req);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Training> findByName(String trainingName) {
        log.info("Finding Training with name: {}", trainingName);
        return trainingDao.findByName(trainingName);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Training> findAll() {
        log.info("Finding all Trainings");
        return trainingDao.findAll();
    }

    @Override
    public Optional<Training> selectTraining(String trainingName) {
        log.info("Finding Training with name: {}", trainingName);
        return trainingDao.findByName(trainingName);
    }

    private void authenticate(String username, String password) {
        boolean isTrainee = traineeDao.findById(username)
                .map(t -> passwordEncoder.matches(password, t.getPassword()))
                .orElse(false);

        if (isTrainee) return;

        boolean isTrainer = trainerDao.findById(username)
                .map(t -> passwordEncoder.matches(password, t.getPassword()))
                .orElse(false);

        if (!isTrainer) {
            log.warn("Authentication failed for user: {}", username);
            throw new AuthenticationException("Invalid username or password: " + username);
        }
    }

    private void validateRequiredFields(Training training) {
        if (training.getTrainingName() == null || training.getTrainingName().isBlank()) {
            throw new ValidationException("Training name is required");
        }
        if (training.getTrainee() == null) {
            throw new ValidationException("Trainee is required");
        }
        if (training.getTrainer() == null) {
            throw new ValidationException("Trainer is required");
        }
        if (training.getTrainingType() == null) {
            throw new ValidationException("Training type is required");
        }
        if (training.getTrainingDate() == null) {
            throw new ValidationException("Training date is required");
        }
    }
}
