package com.gym.utilities;

import com.gym.dao.TraineeDao;
import com.gym.dao.TrainerDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class UserUtils {

    private TrainerDao trainerDao;
    private TraineeDao traineeDao;

    @Autowired
    public void setTrainerDao(TrainerDao trainerDao) {
        this.trainerDao = trainerDao;
    }

    @Autowired
    public void setTraineeDao(TraineeDao traineeDao) {
        this.traineeDao = traineeDao;
    }

    public String generateUsername(String firstName, String lastName) {
        final String baseUsername = firstName.toLowerCase() + "." + lastName.toLowerCase();

        List<String> existingUsernames = new ArrayList<>();
        trainerDao.findAll().forEach(t -> existingUsernames.add(t.getUsername()));
        traineeDao.findAll().forEach(t -> existingUsernames.add(t.getUsername()));

        String username = baseUsername;
        int s = 1;
        while (existingUsernames.contains(username)) {
            username = baseUsername + s;
            s++;
        }

        log.info("Generated username: {}", username);
        return username;
    }

    public String generatePassword() {
        String password = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        log.info("Generated password: {}", password);
        return password;
    }
}