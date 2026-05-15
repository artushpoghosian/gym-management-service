package com.gym.dao.impl;

import com.gym.model.Trainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TrainerDaoImplTest {

    private TrainerDaoImpl trainerDao;
    private Map<String, Trainer> storage;

    private Trainer trainer;

    @BeforeEach
    void setUp() {
        storage = new HashMap<>();
        trainerDao = new TrainerDaoImpl();
        trainerDao.setTrainerStorage(storage);

        trainer = new Trainer();
        trainer.setId(1L);
        trainer.setFirstName("John");
        trainer.setLastName("Smith");
        trainer.setUsername("john.smith");
        trainer.setSpecialization("Yoga");
        trainer.setActive(true);
    }

    @Test
    void save_ShouldStoreTrainer_ByUsername() {
        Trainer result = trainerDao.save(trainer);

        assertThat(result).isEqualTo(trainer);
        assertThat(storage).containsKey("john.smith");
    }

    @Test
    void update_ShouldUpdateTrainer_WhenExists() {
        storage.put("john.smith", trainer);
        trainer.setSpecialization("Pilates");

        Trainer result = trainerDao.update(trainer);

        assertThat(result.getSpecialization()).isEqualTo("Pilates");
        assertThat(storage.get("john.smith").getSpecialization()).isEqualTo("Pilates");
    }

    @Test
    void update_ShouldReturnNull_WhenNotExists() {
        Trainer result = trainerDao.update(trainer);

        assertThat(result).isNull();
    }

    @Test
    void findByUsername_ShouldReturnTrainer_WhenExists() {
        storage.put("john.smith", trainer);

        Optional<Trainer> result = trainerDao.findById("john.smith");

        assertThat(result).isPresent().contains(trainer);
    }

    @Test
    void findByUsername_ShouldReturnEmpty_WhenNotExists() {
        Optional<Trainer> result = trainerDao.findById("unknown.user");

        assertThat(result).isEmpty();
    }

    @Test
    void existsByUsername_ShouldReturnTrue_WhenExists() {
        storage.put("john.smith", trainer);

        assertThat(trainerDao.existsByUsername("john.smith")).isTrue();
    }

    @Test
    void existsByUsername_ShouldReturnFalse_WhenNotExists() {
        assertThat(trainerDao.existsByUsername("unknown.user")).isFalse();
    }

    @Test
    void findAll_ShouldReturnAllTrainers() {
        storage.put("john.smith", trainer);

        List<Trainer> result = trainerDao.findAll();

        assertThat(result).hasSize(1).contains(trainer);
    }

    @Test
    void findAll_ShouldReturnEmpty_WhenStorageEmpty() {
        List<Trainer> result = trainerDao.findAll();

        assertThat(result).isEmpty();
    }
}