package com.gym.storage;

import com.gym.model.Trainee;
import com.gym.model.Trainer;
import com.gym.model.Training;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class StorageInitializerTest {

    @InjectMocks
    private StorageInitializer storageInitializer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(storageInitializer, "trainersPath", "classpath:data/trainers.csv");
        ReflectionTestUtils.setField(storageInitializer, "traineesPath", "classpath:data/trainees.csv");
        ReflectionTestUtils.setField(storageInitializer, "trainingsPath", "classpath:data/trainings.csv");
    }

    @Test
    void postProcessAfterInitialization_trainerStorageBean_loadsTrainersAndReturnsBean() {
        Map<String, Trainer> trainerStorage = new HashMap<>();

        Object result = storageInitializer.postProcessAfterInitialization(trainerStorage, "trainerStorage");

        assertSame(trainerStorage, result);
        assertFalse(trainerStorage.isEmpty());
    }

    @Test
    void postProcessAfterInitialization_trainerStorageBean_trainerHasCorrectFields() {
        Map<String, Trainer> trainerStorage = new HashMap<>();

        storageInitializer.postProcessAfterInitialization(trainerStorage, "trainerStorage");

        assertFalse(trainerStorage.isEmpty());
        trainerStorage.values().forEach(trainer -> {
            assertNotNull(trainer.getFirstName());
            assertNotNull(trainer.getLastName());
            assertNotNull(trainer.getSpecialization());
            assertNotNull(trainer.getUsername());
            assertTrue(trainer.isActive());
        });
    }

    @Test
    void postProcessAfterInitialization_trainerStorageBean_usernameIsFirstNameDotLastName() {
        Map<String, Trainer> trainerStorage = new HashMap<>();

        storageInitializer.postProcessAfterInitialization(trainerStorage, "trainerStorage");

        trainerStorage.forEach((key, trainer) -> {
            String expectedUsername = trainer.getFirstName().toLowerCase() + "." + trainer.getLastName().toLowerCase();
            assertEquals(expectedUsername, trainer.getUsername());
            assertEquals(expectedUsername, key);
        });
    }

    @Test
    void postProcessAfterInitialization_traineeStorageBean_loadsTraineesAndReturnsBean() {
        Map<String, Trainee> traineeStorage = new HashMap<>();

        Object result = storageInitializer.postProcessAfterInitialization(traineeStorage, "traineeStorage");

        assertSame(traineeStorage, result);
        assertFalse(traineeStorage.isEmpty());
    }

    @Test
    void postProcessAfterInitialization_traineeStorageBean_traineeHasCorrectFields() {
        Map<String, Trainee> traineeStorage = new HashMap<>();

        storageInitializer.postProcessAfterInitialization(traineeStorage, "traineeStorage");

        assertFalse(traineeStorage.isEmpty());
        traineeStorage.values().forEach(trainee -> {
            assertNotNull(trainee.getFirstName());
            assertNotNull(trainee.getLastName());
            assertNotNull(trainee.getDateOfBirth());
            assertNotNull(trainee.getAddress());
            assertNotNull(trainee.getUsername());
            assertTrue(trainee.isActive());
        });
    }

    @Test
    void postProcessAfterInitialization_traineeStorageBean_usernameIsFirstNameDotLastName() {
        Map<String, Trainee> traineeStorage = new HashMap<>();

        storageInitializer.postProcessAfterInitialization(traineeStorage, "traineeStorage");

        traineeStorage.forEach((key, trainee) -> {
            String expectedUsername = trainee.getFirstName().toLowerCase() + "." + trainee.getLastName().toLowerCase();
            assertEquals(expectedUsername, trainee.getUsername());
            assertEquals(expectedUsername, key);
        });
    }

    @Test
    void postProcessAfterInitialization_trainingStorageBean_loadsTrainingsAndReturnsBean() {
        Map<String, Training> trainingStorage = new HashMap<>();

        Object result = storageInitializer.postProcessAfterInitialization(trainingStorage, "trainingStorage");

        assertSame(trainingStorage, result);
        assertFalse(trainingStorage.isEmpty());
    }

    @Test
    void postProcessAfterInitialization_trainingStorageBean_trainingHasCorrectFields() {
        Map<String, Training> trainingStorage = new HashMap<>();

        storageInitializer.postProcessAfterInitialization(trainingStorage, "trainingStorage");

        assertFalse(trainingStorage.isEmpty());
        trainingStorage.values().forEach(training -> {
            assertNotNull(training.getTrainingName());
            assertNotNull(training.getTrainingType());
            assertNotNull(training.getTrainingDate());
            assertTrue(training.getTrainingDuration() > 0);
        });
    }

    @Test
    void postProcessAfterInitialization_trainingStorageBean_keyIsTrainingName() {
        Map<String, Training> trainingStorage = new HashMap<>();

        storageInitializer.postProcessAfterInitialization(trainingStorage, "trainingStorage");

        trainingStorage.forEach((key, training) ->
                assertEquals(training.getTrainingName(), key)
        );
    }

    @Test
    void postProcessAfterInitialization_unknownBeanName_returnsBeanUnchanged() {
        Object unknownBean = new Object();

        Object result = storageInitializer.postProcessAfterInitialization(unknownBean, "unknownBean");

        assertSame(unknownBean, result);
    }

    @Test
    void postProcessAfterInitialization_unknownBeanName_doesNotModifyStorage() {
        Map<String, Object> storage = new HashMap<>();
        storage.put("existing", new Object());

        storageInitializer.postProcessAfterInitialization(storage, "someOtherBean");

        assertEquals(1, storage.size());
        assertTrue(storage.containsKey("existing"));
    }

    @Test
    void postProcessAfterInitialization_invalidTrainerFilePath_storageRemainsEmptyAndBeanReturned() {
        ReflectionTestUtils.setField(storageInitializer, "trainersPath", "classpath:data/nonexistent.csv");
        Map<String, Trainer> trainerStorage = new HashMap<>();

        Object result = storageInitializer.postProcessAfterInitialization(trainerStorage, "trainerStorage");

        assertSame(trainerStorage, result);
        assertTrue(trainerStorage.isEmpty());
    }

    @Test
    void postProcessAfterInitialization_invalidTraineeFilePath_storageRemainsEmptyAndBeanReturned() {
        ReflectionTestUtils.setField(storageInitializer, "traineesPath", "classpath:data/nonexistent.csv");
        Map<String, Trainee> traineeStorage = new HashMap<>();

        Object result = storageInitializer.postProcessAfterInitialization(traineeStorage, "traineeStorage");

        assertSame(traineeStorage, result);
        assertTrue(traineeStorage.isEmpty());
    }

    @Test
    void postProcessAfterInitialization_invalidTrainingFilePath_storageRemainsEmptyAndBeanReturned() {
        ReflectionTestUtils.setField(storageInitializer, "trainingsPath", "classpath:data/nonexistent.csv");
        Map<String, Training> trainingStorage = new HashMap<>();

        Object result = storageInitializer.postProcessAfterInitialization(trainingStorage, "trainingStorage");

        assertSame(trainingStorage, result);
        assertTrue(trainingStorage.isEmpty());
    }

    @Test
    void postProcessAfterInitialization_trainerPathWithoutClasspathPrefix_loadsTrainersSuccessfully() {
        ReflectionTestUtils.setField(storageInitializer, "trainersPath", "data/trainers.csv");
        Map<String, Trainer> trainerStorage = new HashMap<>();

        Object result = storageInitializer.postProcessAfterInitialization(trainerStorage, "trainerStorage");

        assertSame(trainerStorage, result);
        assertFalse(trainerStorage.isEmpty());
    }

    @Test
    void postProcessAfterInitialization_nullBean_returnsNull() {
        Object result = storageInitializer.postProcessAfterInitialization(null, "unknownBean");

        assertNull(result);
    }
}