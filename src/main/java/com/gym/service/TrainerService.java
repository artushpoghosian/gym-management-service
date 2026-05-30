package com.gym.service;

import com.gym.model.Trainer;
import com.gym.model.Training;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TrainerService {

    Trainer create(Trainer trainer);
    boolean matchCredentials(String username, String password);
    Optional<Trainer> selectTrainer(String authUsername, String authPassword, String targetUsername);
    void changePassword(String username, String oldPassword, String newPassword);
    Trainer update(String authUsername, String authPassword, Trainer trainer);
    void setActive(String authUsername, String authPassword, String targetUsername, boolean active);
    List<Training> getTrainings(String authUsername, String authPassword,
                                String targetUsername, LocalDate fromDate, LocalDate toDate,
                                String traineeName);
    List<Trainer> findAll();
}
