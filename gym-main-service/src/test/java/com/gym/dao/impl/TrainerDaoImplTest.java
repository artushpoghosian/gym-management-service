package com.gym.dao.impl;

import com.gym.model.Trainee;
import com.gym.model.Trainer;
import com.gym.model.Training;
import com.gym.model.TrainingType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainerDaoImplTest {

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private TrainerDaoImpl trainerDao;

    private Trainer trainer;

    @BeforeEach
    void setUp() {
        trainer = new Trainer();
        trainer.setId(1L);
        trainer.setFirstName("John");
        trainer.setLastName("Smith");
        trainer.setUsername("john.smith");
        trainer.setPassword("password123");
        trainer.setSpecialization(TrainingType.YOGA);
        trainer.setActive(true);
    }

    @Test
    void save_ShouldPersist_WhenIdIsNull() {
        trainer.setId(null);

        Trainer result = trainerDao.save(trainer);

        verify(entityManager).persist(trainer);
        verify(entityManager, never()).merge(any());
        assertThat(result).isEqualTo(trainer);
    }

    @Test
    void save_ShouldMerge_WhenIdIsNotNull() {
        Trainer merged = new Trainer();
        when(entityManager.merge(trainer)).thenReturn(merged);

        Trainer result = trainerDao.save(trainer);

        verify(entityManager).merge(trainer);
        verify(entityManager, never()).persist(any());
        assertThat(result).isEqualTo(merged);
    }

    @Test
    void update_ShouldMergeAndReturnTrainer() {
        trainer.setSpecialization(TrainingType.PILATES);
        when(entityManager.merge(trainer)).thenReturn(trainer);

        Trainer result = trainerDao.update(trainer);

        verify(entityManager).merge(trainer);
        assertThat(result.getSpecialization()).isEqualTo(TrainingType.PILATES);
    }

    @Test
    void delete_ShouldRemoveTrainer_WhenFoundByUsername() {
        mockFindByUsernameQuery(List.of(trainer));
        when(entityManager.contains(trainer)).thenReturn(true);

        trainerDao.delete("john.smith");

        verify(entityManager).remove(trainer);
    }

    @Test
    void delete_ShouldMergeBeforeRemove_WhenNotInPersistenceContext() {
        mockFindByUsernameQuery(List.of(trainer));
        when(entityManager.contains(trainer)).thenReturn(false);
        when(entityManager.merge(trainer)).thenReturn(trainer);

        trainerDao.delete("john.smith");

        verify(entityManager).merge(trainer);
        verify(entityManager).remove(trainer);
    }

    @Test
    void delete_ShouldDoNothing_WhenTrainerNotFound() {
        mockFindByUsernameQuery(List.of());

        trainerDao.delete("nonexistent");

        verify(entityManager, never()).remove(any());
    }

    @Test
    void findById_ShouldReturnTrainer_WhenExists() {
        mockFindByUsernameQuery(List.of(trainer));

        Optional<Trainer> result = trainerDao.findById("john.smith");

        assertThat(result).isPresent().contains(trainer);
    }

    @Test
    void findById_ShouldReturnEmpty_WhenNotExists() {
        mockFindByUsernameQuery(List.of());

        Optional<Trainer> result = trainerDao.findById("unknown");

        assertThat(result).isEmpty();
    }

    @Test
    void findAll_ShouldReturnAllTrainers() {
        TypedQuery<Trainer> query = mock(TypedQuery.class);
        when(entityManager.createQuery("SELECT t FROM Trainer t", Trainer.class)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(trainer));

        List<Trainer> result = trainerDao.findAll();

        assertThat(result).hasSize(1).contains(trainer);
    }

    @Test
    void findAll_ShouldReturnEmptyList_WhenNoTrainersExist() {
        TypedQuery<Trainer> query = mock(TypedQuery.class);
        when(entityManager.createQuery("SELECT t FROM Trainer t", Trainer.class)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());

        List<Trainer> result = trainerDao.findAll();

        assertThat(result).isEmpty();
    }

    @Test
    void existsByUsername_ShouldReturnTrue_WhenExists() {
        TypedQuery<Long> query = mock(TypedQuery.class);
        when(entityManager.createQuery(
                "SELECT COUNT(t) FROM Trainer t WHERE t.username = :username", Long.class))
                .thenReturn(query);
        when(query.setParameter("username", "john.smith")).thenReturn(query);
        when(query.getSingleResult()).thenReturn(1L);

        assertThat(trainerDao.existsByUsername("john.smith")).isTrue();
    }

    @Test
    void existsByUsername_ShouldReturnFalse_WhenNotExists() {
        TypedQuery<Long> query = mock(TypedQuery.class);
        when(entityManager.createQuery(
                "SELECT COUNT(t) FROM Trainer t WHERE t.username = :username", Long.class))
                .thenReturn(query);
        when(query.setParameter("username", "unknown")).thenReturn(query);
        when(query.getSingleResult()).thenReturn(0L);

        assertThat(trainerDao.existsByUsername("unknown")).isFalse();
    }

    @Test
    void updatePassword_ShouldExecuteJpqlUpdate() {
        jakarta.persistence.Query jpqlQuery = mock(jakarta.persistence.Query.class);
        when(entityManager.createQuery(
                "UPDATE User u SET u.password = :password WHERE u.username = :username"))
                .thenReturn(jpqlQuery);
        when(jpqlQuery.setParameter("password", "newPass")).thenReturn(jpqlQuery);
        when(jpqlQuery.setParameter("username", "john.smith")).thenReturn(jpqlQuery);

        trainerDao.updatePassword("john.smith", "newPass");

        verify(jpqlQuery).executeUpdate();
    }

    @Test
    void updateStatus_ShouldExecuteJpqlUpdate_WhenDeactivating() {
        jakarta.persistence.Query jpqlQuery = mock(jakarta.persistence.Query.class);
        when(entityManager.createQuery(
                "UPDATE User u SET u.isActive = :isActive WHERE u.username = :username"))
                .thenReturn(jpqlQuery);
        when(jpqlQuery.setParameter("isActive", false)).thenReturn(jpqlQuery);
        when(jpqlQuery.setParameter("username", "john.smith")).thenReturn(jpqlQuery);

        trainerDao.updateStatus("john.smith", false);

        verify(jpqlQuery).executeUpdate();
    }

    @Test
    void updateStatus_ShouldExecuteJpqlUpdate_WhenActivating() {
        jakarta.persistence.Query jpqlQuery = mock(jakarta.persistence.Query.class);
        when(entityManager.createQuery(
                "UPDATE User u SET u.isActive = :isActive WHERE u.username = :username"))
                .thenReturn(jpqlQuery);
        when(jpqlQuery.setParameter("isActive", true)).thenReturn(jpqlQuery);
        when(jpqlQuery.setParameter("username", "john.smith")).thenReturn(jpqlQuery);

        trainerDao.updateStatus("john.smith", true);

        verify(jpqlQuery).executeUpdate();
    }

    @Test
    void findTrainingsByCriteria_ShouldReturnTrainings_WithAllFilters() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<Training> cq = mock(CriteriaQuery.class);
        Root<Training> root = mock(Root.class);
        Join<Training, Trainer> trainerJoin = mock(Join.class);
        Join<Training, Trainee> traineeJoin = mock(Join.class);
        Path<Object> pathMock = mock(Path.class);
        Predicate predicate = mock(Predicate.class);
        TypedQuery<Training> typedQuery = mock(TypedQuery.class);
        List<Training> expected = List.of(new Training());

        when(entityManager.getCriteriaBuilder()).thenReturn(cb);
        when(cb.createQuery(Training.class)).thenReturn(cq);
        when(cq.from(Training.class)).thenReturn(root);
        doReturn(trainerJoin).when(root).join("trainer");
        doReturn(traineeJoin).when(root).join("trainee");
        doReturn(pathMock).when(trainerJoin).get("username");
        doReturn(pathMock).when(traineeJoin).get("username");
        doReturn(pathMock).when(root).get(anyString());

        lenient().doReturn(predicate).when(cb).equal(any(), any());
        lenient().doReturn(predicate).when(cb).greaterThanOrEqualTo(any(), any(LocalDate.class));
        lenient().doReturn(predicate).when(cb).lessThanOrEqualTo(any(), any(LocalDate.class));
        lenient().doReturn(predicate).when(cb).and(any(Predicate[].class));

        when(entityManager.createQuery(cq)).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(expected);

        List<Training> result = trainerDao.findTrainingsByCriteria(
                "john.smith",
                LocalDate.now().minusDays(5),
                LocalDate.now(),
                "alice.brown"
        );

        assertThat(result).isEqualTo(expected);
        verify(cq).where(any(Predicate.class));
    }

    @Test
    void findTrainingsByCriteria_ShouldApplyOnlyFromDateFilter_WhenOnlyFromDateProvided() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<Training> cq = mock(CriteriaQuery.class);
        Root<Training> root = mock(Root.class);
        Join<Training, Trainer> trainerJoin = mock(Join.class);
        Path<Object> pathMock = mock(Path.class);
        Predicate predicate = mock(Predicate.class);
        TypedQuery<Training> typedQuery = mock(TypedQuery.class);
        List<Training> expected = List.of(new Training());

        when(entityManager.getCriteriaBuilder()).thenReturn(cb);
        when(cb.createQuery(Training.class)).thenReturn(cq);
        when(cq.from(Training.class)).thenReturn(root);
        doReturn(trainerJoin).when(root).join("trainer");
        doReturn(pathMock).when(trainerJoin).get("username");
        doReturn(pathMock).when(root).get("trainingDate");

        lenient().doReturn(predicate).when(cb).equal(any(), any());
        lenient().doReturn(predicate).when(cb).greaterThanOrEqualTo(any(), any(LocalDate.class));
        lenient().doReturn(predicate).when(cb).and(any(Predicate[].class));

        when(entityManager.createQuery(cq)).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(expected);

        List<Training> result = trainerDao.findTrainingsByCriteria(
                "john.smith", LocalDate.now().minusDays(3), null, null
        );

        assertThat(result).isEqualTo(expected);
        verify(cb).greaterThanOrEqualTo(any(), any(LocalDate.class));
        verify(cb, never()).lessThanOrEqualTo(any(), any(LocalDate.class));
    }

    @Test
    void findTrainingsByCriteria_ShouldApplyOnlyToDateFilter_WhenOnlyToDateProvided() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<Training> cq = mock(CriteriaQuery.class);
        Root<Training> root = mock(Root.class);
        Join<Training, Trainer> trainerJoin = mock(Join.class);
        Path<Object> pathMock = mock(Path.class);
        Predicate predicate = mock(Predicate.class);
        TypedQuery<Training> typedQuery = mock(TypedQuery.class);
        List<Training> expected = List.of(new Training());

        when(entityManager.getCriteriaBuilder()).thenReturn(cb);
        when(cb.createQuery(Training.class)).thenReturn(cq);
        when(cq.from(Training.class)).thenReturn(root);
        doReturn(trainerJoin).when(root).join("trainer");
        doReturn(pathMock).when(trainerJoin).get("username");
        doReturn(pathMock).when(root).get("trainingDate");

        lenient().doReturn(predicate).when(cb).equal(any(), any());
        lenient().doReturn(predicate).when(cb).lessThanOrEqualTo(any(), any(LocalDate.class));
        lenient().doReturn(predicate).when(cb).and(any(Predicate[].class));

        when(entityManager.createQuery(cq)).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(expected);

        List<Training> result = trainerDao.findTrainingsByCriteria(
                "john.smith", null, LocalDate.now(), null
        );

        assertThat(result).isEqualTo(expected);
        verify(cb).lessThanOrEqualTo(any(), any(LocalDate.class));
        verify(cb, never()).greaterThanOrEqualTo(any(), any(LocalDate.class));
    }

    @Test
    void findTrainingsByCriteria_ShouldSkipTraineeJoin_WhenTraineeNameIsBlank() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<Training> cq = mock(CriteriaQuery.class);
        Root<Training> root = mock(Root.class);
        Join<Training, Trainer> trainerJoin = mock(Join.class);
        Path<Object> pathMock = mock(Path.class);
        Predicate predicate = mock(Predicate.class);
        TypedQuery<Training> typedQuery = mock(TypedQuery.class);
        List<Training> expected = List.of(new Training());

        when(entityManager.getCriteriaBuilder()).thenReturn(cb);
        when(cb.createQuery(Training.class)).thenReturn(cq);
        when(cq.from(Training.class)).thenReturn(root);
        doReturn(trainerJoin).when(root).join("trainer");
        doReturn(pathMock).when(trainerJoin).get("username");

        lenient().doReturn(predicate).when(cb).equal(any(), any());
        lenient().doReturn(predicate).when(cb).and(any(Predicate[].class));

        when(entityManager.createQuery(cq)).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(expected);

        List<Training> result = trainerDao.findTrainingsByCriteria(
                "john.smith", null, null, "   "
        );

        assertThat(result).isEqualTo(expected);
        verify(root, never()).join("trainee");
    }

    @Test
    void findTrainingsByCriteria_ShouldReturnTrainings_WithNoOptionalFilters() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<Training> cq = mock(CriteriaQuery.class);
        Root<Training> root = mock(Root.class);
        Join<Training, Trainer> trainerJoin = mock(Join.class);
        Path<Object> pathMock = mock(Path.class);
        Predicate predicate = mock(Predicate.class);
        TypedQuery<Training> typedQuery = mock(TypedQuery.class);
        List<Training> expected = List.of(new Training());

        when(entityManager.getCriteriaBuilder()).thenReturn(cb);
        when(cb.createQuery(Training.class)).thenReturn(cq);
        when(cq.from(Training.class)).thenReturn(root);
        doReturn(trainerJoin).when(root).join("trainer");
        doReturn(pathMock).when(trainerJoin).get("username");

        lenient().doReturn(predicate).when(cb).equal(any(), any());
        lenient().doReturn(predicate).when(cb).and(any(Predicate[].class));

        when(entityManager.createQuery(cq)).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(expected);

        List<Training> result = trainerDao.findTrainingsByCriteria(
                "john.smith", null, null, null
        );

        assertThat(result).isEqualTo(expected);
    }

    private void mockFindByUsernameQuery(List<Trainer> results) {
        TypedQuery<Trainer> query = mock(TypedQuery.class);
        when(entityManager.createQuery(
                "SELECT t FROM Trainer t WHERE t.username = :username", Trainer.class))
                .thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(results);
    }
}
