package com.gym.dao.impl;

import com.gym.model.Training;
import com.gym.model.TrainingType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainingDaoImplTest {

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private TrainingDaoImpl trainingDao;

    private Training training;

    @BeforeEach
    void setUp() {
        training = new Training();
        training.setTrainingName("Morning Yoga");
        training.setTrainingType(TrainingType.YOGA);
        training.setTrainingDate(LocalDate.of(2024, 6, 1));
        training.setTrainingDuration(60);
    }

    @Test
    void save_ShouldPersist_WhenIdIsNull() {
        training.setId(null);

        Training result = trainingDao.save(training);

        verify(entityManager).persist(training);
        verify(entityManager, never()).merge(any());
        assertThat(result).isEqualTo(training);
    }

    @Test
    void save_ShouldMerge_WhenIdIsNotNull() {
        training.setId(1L);
        Training merged = new Training();
        when(entityManager.merge(training)).thenReturn(merged);

        Training result = trainingDao.save(training);

        verify(entityManager).merge(training);
        verify(entityManager, never()).persist(any());
        assertThat(result).isEqualTo(merged);
    }

    @Test
    void findByName_ShouldReturnTraining_WhenExists() {
        mockFindByNameQuery(List.of(training));

        Optional<Training> result = trainingDao.findByName("Morning Yoga");

        assertThat(result).isPresent().contains(training);
    }

    @Test
    void findByName_ShouldReturnEmpty_WhenNotExists() {
        mockFindByNameQuery(List.of());

        Optional<Training> result = trainingDao.findByName("Unknown Training");

        assertThat(result).isEmpty();
    }

    @Test
    void findAll_ShouldReturnAllTrainings() {
        TypedQuery<Training> query = mock(TypedQuery.class);
        when(entityManager.createQuery("SELECT t FROM Training t", Training.class)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(training));

        List<Training> result = trainingDao.findAll();

        assertThat(result).hasSize(1).contains(training);
    }

    @Test
    void findAll_ShouldReturnEmptyList_WhenNoTrainings() {
        TypedQuery<Training> query = mock(TypedQuery.class);
        when(entityManager.createQuery("SELECT t FROM Training t", Training.class)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());

        List<Training> result = trainingDao.findAll();

        assertThat(result).isEmpty();
    }

    private void mockFindByNameQuery(List<Training> results) {
        TypedQuery<Training> query = mock(TypedQuery.class);
        when(entityManager.createQuery(
                "SELECT t FROM Training t WHERE t.trainingName = :name", Training.class))
                .thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(results);
    }
}
