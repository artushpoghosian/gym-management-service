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
import static org.junit.jupiter.api.Assertions.assertThrows;

class TrainingDaoImplTest {

    private TrainingDaoImpl trainingDao;
    private Map<String, Training> storage;

    private Training training;

    @BeforeEach
    void setUp() {
        storage = new HashMap<>();
        trainingDao = new TrainingDaoImpl();
        trainingDao.setTraineeStorage(storage);

        training = new Training();
        training.setId(1L);
        training.setTrainerId(1L);
        training.setTraineeId(1L);
        training.setTrainingName("Morning Yoga");
        training.setTrainingType(new TrainingType("Yoga"));
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
    void update_ShouldUpdateTraining_WhenExists() {
        storage.put("Morning Yoga", training);
        training.setTrainingDuration(90);

        Training result = trainingDao.update(training);

        assertThat(result.getTrainingDuration()).isEqualTo(90);
        assertThat(storage.get("Morning Yoga").getTrainingDuration()).isEqualTo(90);
    }

    @Test
    void update_ShouldReturnNull_WhenNotExists() {
        Training result = trainingDao.update(training);

        assertThat(result).isNull();
    }

    @Test
    void delete_ShouldRemoveTraining_ById() {
        storage.put("Morning Yoga", training);

        trainingDao.delete(1L);

        assertThat(storage).doesNotContainKey("Morning Yoga");
    }

    @Test
    void delete_ShouldThrowException_WhenIdNotFound() {
        assertThrows(Exception.class, () -> trainingDao.delete(99L));
    }

    @Test
    void findById_ShouldReturnTraining_WhenExists() {
        storage.put("Morning Yoga", training);

        Optional<Training> result = trainingDao.findById(1L);

        assertThat(result).isPresent().contains(training);
    }

    @Test
    void findById_ShouldReturnEmpty_WhenNotExists() {
        Optional<Training> result = trainingDao.findById(99L);

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