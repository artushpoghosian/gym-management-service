package com.gym.utilities;

import com.gym.dao.TraineeDao;
import com.gym.dao.TrainerDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Predicate;

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

    public String generateUsername(String firstName, String lastName, Predicate<String> usernameExists) {
        final String baseUsername = firstName.toLowerCase() + "." + lastName.toLowerCase();

        String username = baseUsername;
        int s = 1;
        while (usernameExists.test(username)) {
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