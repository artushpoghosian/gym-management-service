package com.gym.service.impl;

import com.gym.dao.TrainingDao;
import com.gym.model.Training;
import com.gym.model.TrainingType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingServiceImplTest {

    @Mock
    private TrainingDao trainingDao;

    @InjectMocks
    private TrainingServiceImpl trainingService;

    private Training training;

    @BeforeEach
    void setUp() {
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
    void create_ShouldSaveAndReturnTraining() {
        when(trainingDao.save(training)).thenReturn(training);

        Training result = trainingService.create(training);

        assertThat(result).isEqualTo(training);
        assertThat(result.getTrainingName()).isEqualTo("Morning Yoga");
        verify(trainingDao).save(training);
    }

    @Test
    void findAll_ShouldReturnAllTrainings() {
        when(trainingDao.findAll()).thenReturn(List.of(training));

        List<Training> result = trainingService.findAll();

        assertThat(result).hasSize(1).contains(training);
        verify(trainingDao).findAll();
    }

    @Test
    void selectTraining_ShouldReturnTraining_WhenExists() {
        when(trainingDao.findByName("Morning Yoga")).thenReturn(Optional.of(training));

        Optional<Training> result = trainingService.selectTraining("Morning Yoga");

        assertThat(result).isPresent().contains(training);
        verify(trainingDao).findByName("Morning Yoga");
    }

    @Test
    void selectTraining_ShouldReturnEmpty_WhenNotExists() {
        when(trainingDao.findByName("Unknown")).thenReturn(Optional.empty());

        Optional<Training> result = trainingService.selectTraining("Unknown");

        assertThat(result).isEmpty();
        verify(trainingDao).findByName("Unknown");
    }
}