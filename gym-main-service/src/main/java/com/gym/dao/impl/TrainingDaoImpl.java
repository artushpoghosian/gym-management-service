package com.gym.dao.impl;

import com.gym.dao.TrainingDao;
import com.gym.model.Training;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Slf4j
public class TrainingDaoImpl implements TrainingDao {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public Training save(Training training) {
        log.debug("Saving training with name: {}", training.getTrainingName());
        if (training.getId() == null) {
            entityManager.persist(training);
            return training;
        }
        return entityManager.merge(training);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Training> findByName(String trainingName) {
        log.info("Finding training with name: {}", trainingName);
        List<Training> result = entityManager
                .createQuery("SELECT t FROM Training t WHERE t.trainingName = :name", Training.class)
                .setParameter("name", trainingName)
                .getResultList();
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Training> findAll() {
        log.info("Finding all trainings");
        return entityManager.createQuery("SELECT t FROM Training t", Training.class).getResultList();
    }

    @Override
    @Transactional
    public void delete(Training training) {
        log.debug("Deleting training with name: {}", training.getTrainingName());
        entityManager.remove(entityManager.contains(training) ? training : entityManager.merge(training));
    }
}
