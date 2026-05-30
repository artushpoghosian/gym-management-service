package com.gym.dao;

import com.gym.model.Trainer;
import com.gym.model.Training;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TrainerDao {
    Trainer save(Trainer trainer);
    Trainer update(Trainer trainer);
    void delete(String username);
    Optional<Trainer> findById(String username);
    List<Trainer> findAll();
    boolean existsByUsername(String username);
    void updatePassword(String username, String newPassword);
    void updateStatus(String username, boolean isActive);
    List<Training> findTrainingsByCriteria(String username, LocalDate fromDate, LocalDate toDate, String traineeName);
}
