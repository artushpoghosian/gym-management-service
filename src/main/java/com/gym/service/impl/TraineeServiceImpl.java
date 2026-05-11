package com.gym.service.impl;

import com.gym.dao.TraineeDao;
import com.gym.model.Trainee;
import com.gym.service.TraineeService;
import com.gym.utilities.UserUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TraineeServiceImpl implements TraineeService {

    private TraineeDao traineeDao;
    private UserUtils userUtils;

    @Autowired
    public void setTraineeDao(TraineeDao traineeDao) {
        this.traineeDao = traineeDao;
    }

    @Autowired
    public void setUserUtils(UserUtils userUtils) {
        this.userUtils = userUtils;
    }

    @Override
    public Trainee create(Trainee trainee) {
        log.info("Creating Trainee: {} {}", trainee.getFirstName(), trainee.getLastName());
        String username = userUtils.generateUsername(trainee.getFirstName(), trainee.getLastName());
        String password = userUtils.generatePassword();

        trainee.setUsername(username);
        trainee.setPassword(password);
        trainee.setActive(true);
        traineeDao.save(trainee);
        return trainee;
    }

    @Override
    public Trainee update(Trainee trainee) {
        log.info("Updating Trainee profile with Username: {}", trainee.getUsername());
        return traineeDao.update(trainee);
    }

    @Override
    public void delete(Long id) {
        log.info("Deleting Trainee with id: {}", id);
        traineeDao.delete(id);
    }

    @Override
    public List<Trainee> findAll() {
        log.info("Finding all Trainees");
        return traineeDao.findAll();
    }

    @Override
    public Optional<Trainee> selectTrainee(Long id) {
        log.info("Selecting Trainee with id: {}", id);
        return traineeDao.findById(id);
    }

}
