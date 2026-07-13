package com.gym.dao.impl;

import com.gym.dao.TraineeDao;
import com.gym.model.Trainee;
import com.gym.model.Trainer;
import com.gym.model.Training;
import com.gym.model.TrainingType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@Slf4j
public class TraineeDaoImpl implements TraineeDao {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public Trainee save(Trainee trainee) {
        log.debug("Saving trainee with username: {}", trainee.getUsername());
        if (trainee.getId() == null) {
            entityManager.persist(trainee);
            return trainee;
        }
        return entityManager.merge(trainee);
    }

    @Override
    @Transactional
    public Trainee update(Trainee trainee) {
        log.debug("Updating trainee: {}", trainee.getUsername());
        return entityManager.merge(trainee);
    }

    @Override
    @Transactional
    public void delete(String username) {
        log.debug("Deleting trainee with username: {}", username);
        findById(username).ifPresent(trainee -> entityManager.remove(
                entityManager.contains(trainee) ? trainee : entityManager.merge(trainee)
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Trainee> findById(String username) {
        log.info("Finding trainee with username: {}", username);
        List<Trainee> result = entityManager
                .createQuery("SELECT t FROM Trainee t WHERE t.username = :username", Trainee.class)
                .setParameter("username", username)
                .getResultList();
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Trainee> findAll() {
        log.info("Finding all trainees");
        return entityManager.createQuery("SELECT t FROM Trainee t", Trainee.class).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        log.debug("Checking if username: {} exists", username);
        Long count = entityManager
                .createQuery("SELECT COUNT(t) FROM Trainee t WHERE t.username = :username", Long.class)
                .setParameter("username", username)
                .getSingleResult();
        return count > 0;
    }

    @Override
    @Transactional
    public void updatePassword(String username, String newPassword) {
        log.info("Updating password for username: {}", username);
        entityManager.createQuery("UPDATE User u SET u.password = :password WHERE u.username = :username")
                .setParameter("password", newPassword)
                .setParameter("username", username)
                .executeUpdate();
    }

    @Override
    @Transactional
    public void updateStatus(String username, boolean isActive) {
        log.info("Updating active status to {} for username: {}", isActive, username);
        entityManager.createQuery("UPDATE User u SET u.isActive = :isActive WHERE u.username = :username")
                .setParameter("isActive", isActive)
                .setParameter("username", username)
                .executeUpdate();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Training> findTrainingsByCriteria(String username, LocalDate fromDate, LocalDate toDate,
                                                  String trainerName, String trainingTypeName) {
        log.info("Filtering trainings for trainee: {}", username);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Training> cq = cb.createQuery(Training.class);
        Root<Training> root = cq.from(Training.class);
        List<Predicate> predicates = new ArrayList<>();

        Join<Training, Trainee> traineeJoin = root.join("trainee");
        predicates.add(cb.equal(traineeJoin.get("username"), username));

        if (fromDate != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("trainingDate"), fromDate));
        }
        if (toDate != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("trainingDate"), toDate));
        }
        if (trainerName != null && !trainerName.trim().isEmpty()) {
            Join<Training, Trainer> trainerJoin = root.join("trainer");
            predicates.add(cb.equal(trainerJoin.get("username"), trainerName));
        }
        if (trainingTypeName != null && !trainingTypeName.trim().isEmpty()) {
            try {
                TrainingType trainingType = TrainingType.valueOf(trainingTypeName.toUpperCase());
                predicates.add(cb.equal(root.get("trainingType"), trainingType));
            } catch (IllegalArgumentException e) {
                log.error("Unknown Training Type: {}", trainingTypeName);
                return new ArrayList<>();
            }
        }

        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        return entityManager.createQuery(cq).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Trainer> findTrainersNotAssignedToTrainee(String username) {
        log.info("Fetching unassigned trainers for trainee via training records: {}", username);
        return entityManager.createQuery(
                        "SELECT tr FROM Trainer tr " +
                                "LEFT JOIN Training tg ON tg.trainer = tr AND tg.trainee.username = :username " +
                                "WHERE tg.id IS NULL",
                        Trainer.class)
                .setParameter("username", username)
                .getResultList();
    }

    @Override
    @Transactional
    public void updateTraineeTrainers(String username, List<Trainer> trainers) {
        log.info("Updating trainers via training assignments for trainee: {}", username);

        findById(username).ifPresent(trainee -> {
            for (Trainer trainer : trainers) {

                Long count = entityManager.createQuery(
                                "SELECT COUNT(t) FROM Training t WHERE t.trainee.username = :tUsername AND t.trainer.username = :trUsername",
                                Long.class)
                        .setParameter("tUsername", username)
                        .setParameter("trUsername", trainer.getUsername())
                        .getSingleResult();

                if (count == 0) {
                    Training training = new Training();
                    training.setTrainee(trainee);
                    training.setTrainer(trainer);
                    training.setTrainingName("Default Assignment Session");
                    training.setTrainingType(trainer.getSpecialization());
                    training.setTrainingDate(LocalDate.now());
                    training.setTrainingDuration(60);
                    entityManager.persist(training);
                }
            }
        });
    }
}
