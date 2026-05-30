package com.gym.dao.impl;

import com.gym.model.Trainee;
import com.gym.model.Trainer;
import com.gym.model.Training;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
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
import java.util.ArrayList;
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
class TraineeDaoImplTest {

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private TraineeDaoImpl traineeDao;

    private Trainee trainee;

    @BeforeEach
    void setUp() {
        trainee = new Trainee();
        trainee.setId(1L);
        trainee.setFirstName("Alice");
        trainee.setLastName("Brown");
        trainee.setUsername("alice.brown");
        trainee.setPassword("password123");
        trainee.setDateOfBirth(LocalDate.of(1995, 4, 12));
        trainee.setAddress("123 Main St");
        trainee.setActive(true);
        trainee.setTrainers(new ArrayList<>());
    }

    @Test
    void save_ShouldPersist_WhenIdIsNull() {
        trainee.setId(null);

        Trainee result = traineeDao.save(trainee);

        verify(entityManager).persist(trainee);
        verify(entityManager, never()).merge(any());
        assertThat(result).isEqualTo(trainee);
    }

    @Test
    void save_ShouldMerge_WhenIdIsNotNull() {
        Trainee merged = new Trainee();
        when(entityManager.merge(trainee)).thenReturn(merged);

        Trainee result = traineeDao.save(trainee);

        verify(entityManager).merge(trainee);
        verify(entityManager, never()).persist(any());
        assertThat(result).isEqualTo(merged);
    }

    @Test
    void update_ShouldMergeAndReturnTrainee() {
        trainee.setAddress("999 New St");
        when(entityManager.merge(trainee)).thenReturn(trainee);

        Trainee result = traineeDao.update(trainee);

        verify(entityManager).merge(trainee);
        assertThat(result).isNotNull();
        assertThat(result.getAddress()).isEqualTo("999 New St");
    }

    @Test
    void delete_ShouldRemoveTrainee_WhenFoundByUsername() {
        mockFindByUsernameQuery(List.of(trainee));
        when(entityManager.contains(trainee)).thenReturn(true);

        traineeDao.delete("alice.brown");

        verify(entityManager).remove(trainee);
    }

    @Test
    void delete_ShouldMergeBeforeRemove_WhenNotInPersistenceContext() {
        mockFindByUsernameQuery(List.of(trainee));
        when(entityManager.contains(trainee)).thenReturn(false);
        when(entityManager.merge(trainee)).thenReturn(trainee);

        traineeDao.delete("alice.brown");

        verify(entityManager).merge(trainee);
        verify(entityManager).remove(trainee);
    }

    @Test
    void delete_ShouldDoNothing_WhenTraineeNotFound() {
        mockFindByUsernameQuery(List.of());

        traineeDao.delete("nonexistent");

        verify(entityManager, never()).remove(any());
    }

    @Test
    void findById_ShouldReturnTrainee_WhenExists() {
        mockFindByUsernameQuery(List.of(trainee));

        Optional<Trainee> result = traineeDao.findById("alice.brown");

        assertThat(result).isPresent().contains(trainee);
    }

    @Test
    void findById_ShouldReturnEmpty_WhenNotExists() {
        mockFindByUsernameQuery(List.of());

        Optional<Trainee> result = traineeDao.findById("unknown");

        assertThat(result).isEmpty();
    }

    @Test
    void findAll_ShouldReturnAllTrainees() {
        TypedQuery<Trainee> query = mock(TypedQuery.class);
        when(entityManager.createQuery("SELECT t FROM Trainee t", Trainee.class)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(trainee));

        List<Trainee> result = traineeDao.findAll();

        assertThat(result).hasSize(1).contains(trainee);
    }

    @Test
    void findAll_ShouldReturnEmptyList_WhenNoTraineesExist() {
        TypedQuery<Trainee> query = mock(TypedQuery.class);
        when(entityManager.createQuery("SELECT t FROM Trainee t", Trainee.class)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());

        List<Trainee> result = traineeDao.findAll();

        assertThat(result).isEmpty();
    }

    @Test
    void existsByUsername_ShouldReturnTrue_WhenExists() {
        TypedQuery<Long> query = mock(TypedQuery.class);
        when(entityManager.createQuery(
                "SELECT COUNT(t) FROM Trainee t WHERE t.username = :username", Long.class))
                .thenReturn(query);
        when(query.setParameter("username", "alice.brown")).thenReturn(query);
        when(query.getSingleResult()).thenReturn(1L);

        assertThat(traineeDao.existsByUsername("alice.brown")).isTrue();
    }

    @Test
    void existsByUsername_ShouldReturnFalse_WhenNotExists() {
        TypedQuery<Long> query = mock(TypedQuery.class);
        when(entityManager.createQuery(
                "SELECT COUNT(t) FROM Trainee t WHERE t.username = :username", Long.class))
                .thenReturn(query);
        when(query.setParameter("username", "unknown")).thenReturn(query);
        when(query.getSingleResult()).thenReturn(0L);

        assertThat(traineeDao.existsByUsername("unknown")).isFalse();
    }

    @Test
    void updatePassword_ShouldExecuteJpqlUpdate() {
        jakarta.persistence.Query jpqlQuery = mock(jakarta.persistence.Query.class);
        when(entityManager.createQuery(
                "UPDATE User u SET u.password = :password WHERE u.username = :username"))
                .thenReturn(jpqlQuery);
        when(jpqlQuery.setParameter("password", "newPass")).thenReturn(jpqlQuery);
        when(jpqlQuery.setParameter("username", "alice.brown")).thenReturn(jpqlQuery);

        traineeDao.updatePassword("alice.brown", "newPass");

        verify(jpqlQuery).executeUpdate();
    }

    @Test
    void updateStatus_ShouldExecuteJpqlUpdate_WhenDeactivating() {
        jakarta.persistence.Query jpqlQuery = mock(jakarta.persistence.Query.class);
        when(entityManager.createQuery(
                "UPDATE User u SET u.isActive = :isActive WHERE u.username = :username"))
                .thenReturn(jpqlQuery);
        when(jpqlQuery.setParameter("isActive", false)).thenReturn(jpqlQuery);
        when(jpqlQuery.setParameter("username", "alice.brown")).thenReturn(jpqlQuery);

        traineeDao.updateStatus("alice.brown", false);

        verify(jpqlQuery).executeUpdate();
    }

    @Test
    void updateStatus_ShouldExecuteJpqlUpdate_WhenActivating() {
        jakarta.persistence.Query jpqlQuery = mock(jakarta.persistence.Query.class);
        when(entityManager.createQuery(
                "UPDATE User u SET u.isActive = :isActive WHERE u.username = :username"))
                .thenReturn(jpqlQuery);
        when(jpqlQuery.setParameter("isActive", true)).thenReturn(jpqlQuery);
        when(jpqlQuery.setParameter("username", "alice.brown")).thenReturn(jpqlQuery);

        traineeDao.updateStatus("alice.brown", true);

        verify(jpqlQuery).executeUpdate();
    }

    @Test
    void findTrainingsByCriteria_ShouldReturnTrainings_WithAllFilters() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<Training> cq = mock(CriteriaQuery.class);
        Root<Training> root = mock(Root.class);
        Join<Training, Trainee> traineeJoin = mock(Join.class);
        Join<Training, Trainer> trainerJoin = mock(Join.class);
        Path<Object> pathMock = mock(Path.class);
        Predicate predicate = mock(Predicate.class);
        TypedQuery<Training> typedQuery = mock(TypedQuery.class);
        List<Training> expected = List.of(new Training());

        when(entityManager.getCriteriaBuilder()).thenReturn(cb);
        when(cb.createQuery(Training.class)).thenReturn(cq);
        when(cq.from(Training.class)).thenReturn(root);

        doReturn(traineeJoin).when(root).join("trainee");
        doReturn(trainerJoin).when(root).join("trainer");

        when(traineeJoin.get("username")).thenReturn(pathMock);
        when(trainerJoin.get("username")).thenReturn(pathMock);
        when(root.get(anyString())).thenReturn(pathMock);

        lenient().doReturn(predicate).when(cb).equal(any(), any());

        lenient().doReturn(predicate).when(cb).greaterThanOrEqualTo(
                (Expression<? extends Comparable>) any(Expression.class),
                (Comparable) any()
        );

        lenient().doReturn(predicate).when(cb).lessThanOrEqualTo(
                (Expression<? extends Comparable>) any(Expression.class),
                (Comparable) any()
        );

        lenient().doReturn(predicate).when(cb).and(any(Predicate[].class));

        when(entityManager.createQuery(cq)).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(expected);

        List<Training> result = traineeDao.findTrainingsByCriteria(
                "alice.brown",
                LocalDate.now().minusDays(1),
                LocalDate.now(),
                "trainer.jack",
                "YOGA"
        );

        assertThat(result).isEqualTo(expected);
        verify(cq).where(any(Predicate.class));
    }

    @Test
    void findTrainingsByCriteria_ShouldApplyOnlyFromDateFilter_WhenOnlyFromDateProvided() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<Training> cq = mock(CriteriaQuery.class);
        Root<Training> root = mock(Root.class);
        Join<Training, Trainee> traineeJoin = mock(Join.class);
        Path<Object> pathMock = mock(Path.class);
        Predicate predicate = mock(Predicate.class);
        TypedQuery<Training> typedQuery = mock(TypedQuery.class);
        List<Training> expected = List.of(new Training());

        when(entityManager.getCriteriaBuilder()).thenReturn(cb);
        when(cb.createQuery(Training.class)).thenReturn(cq);
        when(cq.from(Training.class)).thenReturn(root);

        doReturn(traineeJoin).when(root).join("trainee");
        when(traineeJoin.get("username")).thenReturn(pathMock);
        when(root.get("trainingDate")).thenReturn(pathMock);

        lenient().doReturn(predicate).when(cb).equal(any(), any());

        lenient().doReturn(predicate).when(cb).greaterThanOrEqualTo(
                (Expression<? extends Comparable>) any(Expression.class),
                (Comparable) any()
        );

        lenient().doReturn(predicate).when(cb).and(any(Predicate[].class));

        when(entityManager.createQuery(cq)).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(expected);

        List<Training> result = traineeDao.findTrainingsByCriteria(
                "alice.brown",
                LocalDate.now().minusDays(7),
                null,
                null,
                null
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
        Join<Training, Trainee> traineeJoin = mock(Join.class);
        Path<Object> pathMock = mock(Path.class);
        Predicate predicate = mock(Predicate.class);
        TypedQuery<Training> typedQuery = mock(TypedQuery.class);
        List<Training> expected = List.of(new Training());

        when(entityManager.getCriteriaBuilder()).thenReturn(cb);
        when(cb.createQuery(Training.class)).thenReturn(cq);
        when(cq.from(Training.class)).thenReturn(root);

        doReturn(traineeJoin).when(root).join("trainee");
        when(traineeJoin.get("username")).thenReturn(pathMock);
        when(root.get("trainingDate")).thenReturn(pathMock);

        lenient().doReturn(predicate).when(cb).equal(any(), any());

        lenient().doReturn(predicate).when(cb).lessThanOrEqualTo(
                (Expression<? extends Comparable>) any(Expression.class),
                (Comparable) any()
        );

        lenient().doReturn(predicate).when(cb).and(any(Predicate[].class));

        when(entityManager.createQuery(cq)).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(expected);

        List<Training> result = traineeDao.findTrainingsByCriteria(
                "alice.brown", null, LocalDate.now(), null, null
        );

        assertThat(result).isEqualTo(expected);
        verify(cb).lessThanOrEqualTo(any(), any(LocalDate.class));
        verify(cb, never()).greaterThanOrEqualTo(any(), any(LocalDate.class));
    }

    @Test
    void findTrainingsByCriteria_ShouldSkipTrainerJoin_WhenTrainerNameIsBlank() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<Training> cq = mock(CriteriaQuery.class);
        Root<Training> root = mock(Root.class);
        Join<Training, Trainee> traineeJoin = mock(Join.class);
        Path<Object> pathMock = mock(Path.class);
        Predicate predicate = mock(Predicate.class);
        TypedQuery<Training> typedQuery = mock(TypedQuery.class);
        List<Training> expected = List.of(new Training());

        when(entityManager.getCriteriaBuilder()).thenReturn(cb);
        when(cb.createQuery(Training.class)).thenReturn(cq);
        when(cq.from(Training.class)).thenReturn(root);

        doReturn(traineeJoin).when(root).join("trainee");
        when(traineeJoin.get("username")).thenReturn(pathMock);

        lenient().doReturn(predicate).when(cb).equal(any(), any());
        lenient().doReturn(predicate).when(cb).and(any(Predicate[].class));

        when(entityManager.createQuery(cq)).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(expected);

        List<Training> result = traineeDao.findTrainingsByCriteria(
                "alice.brown", null, null, "   ", null
        );

        assertThat(result).isEqualTo(expected);
        verify(root, never()).join("trainer");
    }

    @Test
    void findTrainingsByCriteria_ShouldReturnTrainings_WithNoOptionalFilters() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<Training> cq = mock(CriteriaQuery.class);
        Root<Training> root = mock(Root.class);
        Join<Training, Trainee> traineeJoin = mock(Join.class);
        Path<Object> pathMock = mock(Path.class);
        Predicate predicate = mock(Predicate.class);
        TypedQuery<Training> typedQuery = mock(TypedQuery.class);
        List<Training> expected = List.of(new Training());

        when(entityManager.getCriteriaBuilder()).thenReturn(cb);
        when(cb.createQuery(Training.class)).thenReturn(cq);
        when(cq.from(Training.class)).thenReturn(root);

        doReturn(traineeJoin).when(root).join("trainee");
        when(traineeJoin.get("username")).thenReturn(pathMock);

        lenient().doReturn(predicate).when(cb).equal(any(), any());
        lenient().doReturn(predicate).when(cb).and(any(Predicate[].class));

        when(entityManager.createQuery(cq)).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(expected);

        List<Training> result = traineeDao.findTrainingsByCriteria(
                "alice.brown", null, null, null, null
        );

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void findTrainingsByCriteria_ShouldReturnEmptyList_WhenUnknownTrainingType() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<Training> cq = mock(CriteriaQuery.class);
        Root<Training> root = mock(Root.class);
        Join<Training, Trainee> traineeJoin = mock(Join.class);
        Path<Object> pathMock = mock(Path.class);
        Predicate predicate = mock(Predicate.class);

        when(entityManager.getCriteriaBuilder()).thenReturn(cb);
        when(cb.createQuery(Training.class)).thenReturn(cq);
        when(cq.from(Training.class)).thenReturn(root);

        doReturn(traineeJoin).when(root).join("trainee");
        when(traineeJoin.get("username")).thenReturn(pathMock);

        lenient().doReturn(predicate).when(cb).equal(any(), any());

        List<Training> result = traineeDao.findTrainingsByCriteria(
                "alice.brown", null, null, null, "INVALID_TYPE"
        );

        assertThat(result).isEmpty();
        verify(cq, never()).where(any(Predicate.class));
        verify(entityManager, never()).createQuery(any(CriteriaQuery.class));
    }

    @Test
    void findTrainersNotAssignedToTrainee_ShouldReturnTrainers() {
        TypedQuery<Trainer> query = mock(TypedQuery.class);
        List<Trainer> expected = List.of(new Trainer());

        when(entityManager.createQuery(
                "SELECT tr FROM Trainer tr WHERE tr NOT IN " +
                        "(SELECT t FROM Trainee tn JOIN tn.trainers t WHERE tn.username = :username)",
                Trainer.class))
                .thenReturn(query);
        when(query.setParameter("username", "alice.brown")).thenReturn(query);
        when(query.getResultList()).thenReturn(expected);

        List<Trainer> result = traineeDao.findTrainersNotAssignedToTrainee("alice.brown");

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void findTrainersNotAssignedToTrainee_ShouldReturnEmptyList_WhenAllTrainersAssigned() {
        TypedQuery<Trainer> query = mock(TypedQuery.class);

        when(entityManager.createQuery(
                "SELECT tr FROM Trainer tr WHERE tr NOT IN " +
                        "(SELECT t FROM Trainee tn JOIN tn.trainers t WHERE tn.username = :username)",
                Trainer.class))
                .thenReturn(query);
        when(query.setParameter("username", "alice.brown")).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());

        List<Trainer> result = traineeDao.findTrainersNotAssignedToTrainee("alice.brown");

        assertThat(result).isEmpty();
    }

    @Test
    void updateTraineeTrainers_ShouldMergeUpdatedTrainee() {
        Trainer trainer = new Trainer();
        trainer.setId(5L);
        List<Trainer> trainers = List.of(trainer);
        mockFindByUsernameQuery(List.of(trainee));
        when(entityManager.merge(trainee)).thenReturn(trainee);

        traineeDao.updateTraineeTrainers("alice.brown", trainers);

        assertThat(trainee.getTrainers()).isEqualTo(trainers);
        verify(entityManager).merge(trainee);
    }

    @Test
    void updateTraineeTrainers_ShouldDoNothing_WhenTraineeNotFound() {
        mockFindByUsernameQuery(List.of());

        traineeDao.updateTraineeTrainers("unknown", List.of());

        verify(entityManager, never()).merge(any());
    }

    private TypedQuery<Trainee> mockFindByUsernameQuery(List<Trainee> results) {
        TypedQuery<Trainee> query = mock(TypedQuery.class);
        when(entityManager.createQuery(
                "SELECT t FROM Trainee t WHERE t.username = :username", Trainee.class))
                .thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(results);
        return query;
    }
}