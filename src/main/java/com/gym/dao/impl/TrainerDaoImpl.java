package com.gym.dao.impl;

import com.gym.dao.TrainerDao;
import com.gym.model.Trainee;
import com.gym.model.Trainer;
import com.gym.model.Training;
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
public class TrainerDaoImpl implements TrainerDao {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public Trainer save(Trainer trainer) {
        log.debug("Saving trainer with username: {}", trainer.getUsername());
        if (trainer.getId() == null) {
            entityManager.persist(trainer);
            return trainer;
        }
        return entityManager.merge(trainer);
    }

    @Override
    @Transactional
    public Trainer update(Trainer trainer) {
        log.debug("Updating trainer: {}", trainer.getUsername());
        return entityManager.merge(trainer);
    }

    @Override
    @Transactional
    public void delete(String username) {
        log.debug("Deleting trainer with username: {}", username);
        findById(username).ifPresent(trainer -> entityManager.remove(
                entityManager.contains(trainer) ? trainer : entityManager.merge(trainer)
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Trainer> findById(String username) {
        log.info("Finding trainer with username: {}", username);
        List<Trainer> result = entityManager
                .createQuery("SELECT t FROM Trainer t WHERE t.username = :username", Trainer.class)
                .setParameter("username", username)
                .getResultList();
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Trainer> findAll() {
        log.info("Finding all trainers");
        return entityManager.createQuery("SELECT t FROM Trainer t", Trainer.class).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        log.debug("Checking if username: {} exists", username);
        Long count = entityManager
                .createQuery("SELECT COUNT(t) FROM Trainer t WHERE t.username = :username", Long.class)
                .setParameter("username", username)
                .getSingleResult();
        return count > 0;
    }

    @Override
    @Transactional
    public void updatePassword(String username, String newPassword) {
        log.info("Updating password for trainer username: {}", username);
        entityManager.createQuery("UPDATE User u SET u.password = :password WHERE u.username = :username")
                .setParameter("password", newPassword)
                .setParameter("username", username)
                .executeUpdate();
    }

    @Override
    @Transactional
    public void updateStatus(String username, boolean isActive) {
        log.info("Updating active status to {} for trainer username: {}", isActive, username);
        entityManager.createQuery("UPDATE User u SET u.isActive = :isActive WHERE u.username = :username")
                .setParameter("isActive", isActive)
                .setParameter("username", username)
                .executeUpdate();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Training> findTrainingsByCriteria(String username, LocalDate fromDate, LocalDate toDate, String traineeName) {
        log.info("Filtering trainings for trainer: {}", username);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Training> cq = cb.createQuery(Training.class);
        Root<Training> root = cq.from(Training.class);
        List<Predicate> predicates = new ArrayList<>();

        Join<Training, Trainer> trainerJoin = root.join("trainer");
        predicates.add(cb.equal(trainerJoin.get("username"), username));

        if (fromDate != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("trainingDate"), fromDate));
        }
        if (toDate != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("trainingDate"), toDate));
        }
        if (traineeName != null && !traineeName.trim().isEmpty()) {
            Join<Training, Trainee> traineeJoin = root.join("trainee");
            predicates.add(cb.equal(traineeJoin.get("username"), traineeName));
        }

        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        return entityManager.createQuery(cq).getResultList();
    }
}