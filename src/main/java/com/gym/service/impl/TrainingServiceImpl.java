package com.gym.service.impl;

import com.gym.dao.TrainingDao;
import com.gym.model.Training;
import com.gym.service.TrainingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrainingServiceImpl implements TrainingService {

    private TrainingDao trainingDao;

    @Autowired
    public void setTrainingDao(TrainingDao trainingDao) {
        this.trainingDao = trainingDao;
    }

    @Override
    public Training create(Training training) {
        log.info("Creating Training: {}", training.getTrainingName());
        return trainingDao.save(training);
    }

    @Override
    public void delete(Long id) {
        log.info("Deleting Training with id: {}", id);
        trainingDao.delete(id);
    }

    @Override
    public List<Training> findAll() {
        log.info("Finding all Trainings");
        return trainingDao.findAll();
    }

    @Override
    public Optional<Training> selectTraining(Long id) {
        log.info("Selecting Training with id: {}", id);
        return trainingDao.findById(id);
    }
}
