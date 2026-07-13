package com.gym.service;

import com.gym.model.Trainee;
import com.gym.model.Trainer;
import com.gym.model.Training;
import com.gym.model.TrainingType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TraineeService {

    Trainee create(Trainee trainee);
    boolean matchCredentials(String username, String password);
    Optional<Trainee> findByUsername(String username);
    Optional<Trainee> selectTrainee(String authUsername, String authPassword, String targetUsername);
    void changePassword(String username, String oldPassword, String newPassword);
    Trainee update(String authUsername, String authPassword, Trainee trainee);
    void setActive(String authUsername, String authPassword, String targetUsername, boolean active);
    void delete(String authUsername, String authPassword, String targetUsername);
    List<Training> getTrainings(String authUsername, String authPassword,
                                String targetUsername, LocalDate fromDate, LocalDate toDate,
                                String trainerName, TrainingType trainingType);
    List<Trainer> getUnassignedTrainers(String authUsername, String authPassword, String targetUsername);
    Trainee updateTrainersList(String authUsername, String authPassword,
                               String targetUsername, List<String> trainerUsernames);
    List<Trainee> findAll();
}
