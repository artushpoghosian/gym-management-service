package com.gym.dao.impl;

import com.gym.model.Trainee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TraineeDaoImplTest {

    private TraineeDaoImpl traineeDao;
    private Map<String, Trainee> storage;

    private Trainee trainee;

    @BeforeEach
    void setUp() {
        storage = new HashMap<>();
        traineeDao = new TraineeDaoImpl();
        traineeDao.setTraineeStorage(storage);

        trainee = new Trainee();
        trainee.setId(1L);
        trainee.setFirstName("Alice");
        trainee.setLastName("Brown");
        trainee.setUsername("alice.brown");
        trainee.setDateOfBirth(LocalDate.of(1995, 4, 12));
        trainee.setAddress("123 Main St");
        trainee.setActive(true);
    }

    @Test
    void save_ShouldStoreTrainee_ByUsername() {
        Trainee result = traineeDao.save(trainee);

        assertThat(result).isEqualTo(trainee);
        assertThat(storage).containsKey("alice.brown");
    }

    @Test
    void update_ShouldUpdateTrainee_WhenExists() {
        storage.put("alice.brown", trainee);
        trainee.setAddress("999 New St");

        Trainee result = traineeDao.update(trainee);

        assertThat(result.getAddress()).isEqualTo("999 New St");
        assertThat(storage.get("alice.brown").getAddress()).isEqualTo("999 New St");
    }

    @Test
    void update_ShouldReturnNull_WhenNotExists() {
        Trainee result = traineeDao.update(trainee);

        assertThat(result).isNull();
    }

    @Test
    void delete_ShouldRemoveTrainee_ByUsername() {
        storage.put("alice.brown", trainee);

        traineeDao.delete("alice.brown");

        assertThat(storage).doesNotContainKey("alice.brown");
    }

    @Test
    void delete_ShouldDoNothing_WhenUsernameNotFound() {
        storage.put("alice.brown", trainee);

        traineeDao.delete("unknown.user");

        assertThat(storage).containsKey("alice.brown");
    }

    @Test
    void findByUsername_ShouldReturnTrainee_WhenExists() {
        storage.put("alice.brown", trainee);

        Optional<Trainee> result = traineeDao.findById("alice.brown");

        assertThat(result).isPresent().contains(trainee);
    }

    @Test
    void findByUsername_ShouldReturnEmpty_WhenNotExists() {
        Optional<Trainee> result = traineeDao.findById("unknown.user");

        assertThat(result).isEmpty();
    }

    @Test
    void existsByUsername_ShouldReturnTrue_WhenExists() {
        storage.put("alice.brown", trainee);

        assertThat(traineeDao.existsByUsername("alice.brown")).isTrue();
    }

    @Test
    void existsByUsername_ShouldReturnFalse_WhenNotExists() {
        assertThat(traineeDao.existsByUsername("unknown.user")).isFalse();
    }

    @Test
    void findAll_ShouldReturnAllTrainees() {
        storage.put("alice.brown", trainee);

        List<Trainee> result = traineeDao.findAll();

        assertThat(result).hasSize(1).contains(trainee);
    }

    @Test
    void findAll_ShouldReturnEmpty_WhenStorageEmpty() {
        List<Trainee> result = traineeDao.findAll();

        assertThat(result).isEmpty();
    }
}