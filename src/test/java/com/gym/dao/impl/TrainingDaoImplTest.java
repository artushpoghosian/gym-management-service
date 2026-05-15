package com.gym.dao.impl;

import com.gym.model.Training;
import com.gym.model.TrainingType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TrainingDaoImplTest {

    private TrainingDaoImpl trainingDao;
    private Map<String, Training> storage;

    private Training training;

    @BeforeEach
    void setUp() {
        storage = new HashMap<>();
        trainingDao = new TrainingDaoImpl();
        trainingDao.setTrainingStorage(storage);

        training = new Training();
        training.setId(1L);
        training.setTrainerId(1L);
        training.setTraineeId(1L);
        training.setTrainingName("Morning Yoga");
        training.setTrainingType(TrainingType.YOGA);
        training.setTrainingDate(LocalDate.of(2024, 6, 1));
        training.setTrainingDuration(60);
    }

    @Test
    void save_ShouldStoreTraining_ByTrainingName() {
        Training result = trainingDao.save(training);

        assertThat(result).isEqualTo(training);
        assertThat(storage).containsKey("Morning Yoga");
    }

    @Test
    void findByName_ShouldReturnTraining_WhenExists() {
        storage.put("Morning Yoga", training);

        Optional<Training> result = trainingDao.findByName("Morning Yoga");

        assertThat(result).isPresent().contains(training);
    }

    @Test
    void findByName_ShouldReturnEmpty_WhenNotExists() {
        Optional<Training> result = trainingDao.findByName("Unknown Training");

        assertThat(result).isEmpty();
    }

    @Test
    void findAll_ShouldReturnAllTrainings() {
        storage.put("Morning Yoga", training);

        List<Training> result = trainingDao.findAll();

        assertThat(result).hasSize(1).contains(training);
    }

    @Test
    void findAll_ShouldReturnEmpty_WhenStorageEmpty() {
        List<Training> result = trainingDao.findAll();

        assertThat(result).isEmpty();
    }
}