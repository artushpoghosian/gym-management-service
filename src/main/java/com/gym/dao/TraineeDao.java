package com.gym.dao;

import com.gym.model.Trainee;
import com.gym.model.Trainer;
import com.gym.model.Training;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TraineeDao {
    Trainee save(Trainee trainee);
    Trainee update(Trainee trainee);
    void delete(String username);
    Optional<Trainee> findById(String username);
    List<Trainee> findAll();
    boolean existsByUsername(String username);
    void updatePassword(String username, String newPassword);
    void updateStatus(String username, boolean isActive);
    List<Training> findTrainingsByCriteria(String username, LocalDate fromDate, LocalDate toDate, String trainerName, String trainingTypeName);
    List<Trainer> findTrainersNotAssignedToTrainee(String username);
    void updateTraineeTrainers(String username, List<Trainer> trainers);
}
