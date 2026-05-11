package com.gym.service.impl;

import com.gym.dao.TraineeDao;
import com.gym.model.Trainee;
import com.gym.utilities.UserUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraineeServiceImplTest {

    @Mock
    private TraineeDao traineeDao;

    @Mock
    private UserUtils userUtils;

    @InjectMocks
    private TraineeServiceImpl traineeService;

    private Trainee trainee;

    @BeforeEach
    void setUp() {
        trainee = new Trainee();
        trainee.setId(1L);
        trainee.setFirstName("Alice");
        trainee.setLastName("Brown");
        trainee.setDateOfBirth(LocalDate.of(1995, 4, 12));
        trainee.setAddress("123 Main St");
    }

    @Test
    void create_ShouldSetUsernamePasswordAndActive_AndSave() {
        when(userUtils.generateUsername("Alice", "Brown")).thenReturn("alice.brown");
        when(userUtils.generatePassword()).thenReturn("pass123456");
        when(traineeDao.save(trainee)).thenReturn(trainee);

        Trainee result = traineeService.create(trainee);

        assertThat(result.getUsername()).isEqualTo("alice.brown");
        assertThat(result.getPassword()).isEqualTo("pass123456");
        assertThat(result.isActive()).isTrue();
        verify(traineeDao).save(trainee);
    }

    @Test
    void update_ShouldDelegateToDao_AndReturnUpdatedTrainee() {
        trainee.setUsername("alice.brown");
        when(traineeDao.update(trainee)).thenReturn(trainee);

        Trainee result = traineeService.update(trainee);

        assertThat(result).isEqualTo(trainee);
        verify(traineeDao).update(trainee);
    }

    @Test
    void delete_ShouldDelegateToDao() {
        traineeService.delete(1L);

        verify(traineeDao).delete(1L);
    }

    @Test
    void findAll_ShouldReturnAllTrainees() {
        when(traineeDao.findAll()).thenReturn(List.of(trainee));

        List<Trainee> result = traineeService.findAll();

        assertThat(result).hasSize(1).contains(trainee);
        verify(traineeDao).findAll();
    }

    @Test
    void selectTrainee_ShouldReturnTrainee_WhenExists() {
        when(traineeDao.findById(1L)).thenReturn(Optional.of(trainee));

        Optional<Trainee> result = traineeService.selectTrainee(1L);

        assertThat(result).isPresent().contains(trainee);
    }

    @Test
    void selectTrainee_ShouldReturnEmpty_WhenNotExists() {
        when(traineeDao.findById(99L)).thenReturn(Optional.empty());

        Optional<Trainee> result = traineeService.selectTrainee(99L);

        assertThat(result).isEmpty();
    }
}