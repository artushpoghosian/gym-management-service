package com.gym.config;

import com.gym.model.Trainee;
import com.gym.model.Trainer;
import com.gym.model.Training;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
public class StorageConfig {

    @Bean(name = "trainerStorage")
    public Map<String, Trainer> trainerStorage() {
        return new HashMap<>();
    }

    @Bean(name = "traineeStorage")
    public Map<String, Trainee> traineeStorage() {
        return new HashMap<>();
    }

    @Bean(name = "trainingStorage")
    public Map<String , Training> trainingStorage() {
        return new HashMap<>();
    }
}
