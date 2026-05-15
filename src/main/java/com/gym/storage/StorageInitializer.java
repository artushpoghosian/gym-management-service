package com.gym.storage;

import com.gym.model.Trainee;
import com.gym.model.Trainer;
import com.gym.model.Training;
import com.gym.model.TrainingType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.Map;

@Component
@Slf4j
public class StorageInitializer implements BeanPostProcessor {

    @Value("${com.gym.storage.init.file.trainers}")
    private String trainersPath;

    @Value("${com.gym.storage.init.file.trainees}")
    private String traineesPath;

    @Value("${com.gym.storage.init.file.trainings}")
    private String trainingsPath;

    @Override
    @SuppressWarnings("unchecked")
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        switch (beanName) {
            case "trainerStorage":
                log.info("Loading trainer data from: {}", trainersPath);
                loadTrainers((Map<String, Trainer>) bean, trainersPath);
                break;
            case "traineeStorage":
                log.info("Loading trainee data from: {}", traineesPath);
                loadTrainees((Map<String, Trainee>) bean, traineesPath);
                break;
            case "trainingStorage":
                log.info("Loading training data from: {}", trainingsPath);
                loadTrainings((Map<String, Training>) bean, trainingsPath);
                break;
            default:
                break;
        }
        return bean;
    }

    private void loadTrainers(Map<String, Trainer> storage, String path) {
        try (BufferedReader reader = openReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",");
                Trainer trainer = new Trainer();
                trainer.setId(Long.parseLong(parts[0].trim()));
                trainer.setFirstName(parts[1].trim());
                trainer.setLastName(parts[2].trim());
                trainer.setSpecialization(parts[3].trim());
                String username = trainer.getFirstName().toLowerCase() + "." + trainer.getLastName().toLowerCase();
                trainer.setUsername(username);
                trainer.setActive(true);
                storage.put(username, trainer);
                log.debug("Loaded trainer: {}", username);
            }
        } catch (Exception e) {
            log.error("Failed to load trainers from {}: {}", path, e.getMessage());
        }
    }

    private void loadTrainees(Map<String, Trainee> storage, String path) {
        try (BufferedReader reader = openReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",");
                Trainee trainee = new Trainee();
                trainee.setId(Long.parseLong(parts[0].trim()));
                trainee.setFirstName(parts[1].trim());
                trainee.setLastName(parts[2].trim());
                trainee.setDateOfBirth(LocalDate.parse(parts[3].trim()));
                trainee.setAddress(parts[4].trim());
                String username = trainee.getFirstName().toLowerCase() + "." + trainee.getLastName().toLowerCase();
                trainee.setUsername(username);
                trainee.setActive(true);
                storage.put(username, trainee);
                log.debug("Loaded trainee: {}", username);
            }
        } catch (Exception e) {
            log.error("Failed to load trainees from {}: {}", path, e.getMessage());
        }
    }

    private void loadTrainings(Map<String, Training> storage, String path) {
        try (BufferedReader reader = openReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",");
                Training training = new Training();
                training.setId(Long.parseLong(parts[0].trim()));
                training.setTrainerId(Long.parseLong(parts[1].trim()));
                training.setTraineeId(Long.parseLong(parts[2].trim()));
                training.setTrainingName(parts[3].trim());
                String typeString = parts[4].trim().toUpperCase();
                training.setTrainingType(TrainingType.valueOf(typeString));
                training.setTrainingDate(LocalDate.parse(parts[5].trim()));
                training.setTrainingDuration(Integer.parseInt(parts[6].trim()));
                storage.put(training.getTrainingName(), training);
                log.debug("Loaded training: {}", training.getTrainingName());
            }
        } catch (Exception e) {
            log.error("Failed to load trainings from {}: {}", path, e.getMessage());
        }
    }

    private BufferedReader openReader(String path) throws Exception {
        String resourcePath = path.startsWith("classpath:") ? path.substring("classpath:".length()) : path;
        return new BufferedReader(
                new InputStreamReader(new ClassPathResource(resourcePath).getInputStream())
        );
    }
}