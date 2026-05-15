package com.gym.service.impl;

import com.gym.dao.TraineeDao;
import com.gym.dao.TrainerDao;
import com.gym.model.Trainer;
import com.gym.utilities.UserUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainerServiceImplTest {

    @Mock
    private TrainerDao trainerDao;

    @Mock
    private TraineeDao traineeDao;

    @Mock
    private UserUtils userUtils;

    @InjectMocks
    private TrainerServiceImpl trainerService;

    private Trainer trainer;

    @BeforeEach
    void setUp() {
        trainer = new Trainer();
        trainer.setId(1L);
        trainer.setFirstName("John");
        trainer.setLastName("Smith");
        trainer.setSpecialization("Yoga");
    }

    @Test
    void create_ShouldSetUsernamePasswordAndActive_AndSave() {
        when(userUtils.generateUsername(eq("John"), eq("Smith"), any(Predicate.class)))
                .thenReturn("john.smith");
        when(userUtils.generatePassword()).thenReturn("rand123456");
        when(trainerDao.save(trainer)).thenReturn(trainer);

        Trainer result = trainerService.create(trainer);

        assertThat(result.getUsername()).isEqualTo("john.smith");
        assertThat(result.getPassword()).isEqualTo("rand123456");
        assertThat(result.isActive()).isTrue();
        verify(trainerDao).save(trainer);
    }

    @Test
    void create_WhenDuplicateName_ShouldUseSuffixedUsername() {
        when(userUtils.generateUsername(eq("John"), eq("Smith"), any(Predicate.class)))
                .thenReturn("john.smith1");
        when(userUtils.generatePassword()).thenReturn("rand123456");
        when(trainerDao.save(trainer)).thenReturn(trainer);

        Trainer result = trainerService.create(trainer);

        assertThat(result.getUsername()).isEqualTo("john.smith1");
        verify(trainerDao).save(trainer);
    }

    @Test
    void update_ShouldDelegateToDao_AndReturnUpdatedTrainer() {
        trainer.setUsername("john.smith");
        when(trainerDao.update(trainer)).thenReturn(trainer);

        Trainer result = trainerService.update(trainer);

        assertThat(result).isEqualTo(trainer);
        verify(trainerDao).update(trainer);
    }

    @Test
    void findAll_ShouldReturnAllTrainers() {
        when(trainerDao.findAll()).thenReturn(List.of(trainer));

        List<Trainer> result = trainerService.findAll();

        assertThat(result).hasSize(1).contains(trainer);
        verify(trainerDao).findAll();
    }

    @Test
    void selectTrainer_ShouldReturnTrainer_WhenExists() {
        String username = "john.smith";
        when(trainerDao.findById(username)).thenReturn(Optional.of(trainer));

        Optional<Trainer> result = trainerService.selectTrainer(username);

        assertThat(result).isPresent().contains(trainer);
        verify(trainerDao).findById(username);
    }

    @Test
    void selectTrainer_ShouldReturnEmpty_WhenNotExists() {
        String username = "nonexistent.user";
        when(trainerDao.findById(username)).thenReturn(Optional.empty());

        Optional<Trainer> result = trainerService.selectTrainer(username);

        assertThat(result).isEmpty();
        verify(trainerDao).findById(username);
    }
}