package com.gym.service.impl;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainerServiceImplTest {

    @Mock
    private TrainerDao trainerDao;

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
        when(userUtils.generateUsername("John", "Smith")).thenReturn("john.smith");
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
        when(userUtils.generateUsername("John", "Smith")).thenReturn("john.smith1");
        when(userUtils.generatePassword()).thenReturn("rand123456");
        when(trainerDao.save(trainer)).thenReturn(trainer);

        Trainer result = trainerService.create(trainer);

        assertThat(result.getUsername()).isEqualTo("john.smith1");
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
    void delete_ShouldDelegateToDao() {
        trainerService.delete(1L);

        verify(trainerDao).delete(1L);
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
        when(trainerDao.findById(1L)).thenReturn(Optional.of(trainer));

        Optional<Trainer> result = trainerService.selectTrainer(1L);

        assertThat(result).isPresent().contains(trainer);
    }

    @Test
    void selectTrainer_ShouldReturnEmpty_WhenNotExists() {
        when(trainerDao.findById(99L)).thenReturn(Optional.empty());

        Optional<Trainer> result = trainerService.selectTrainer(99L);

        assertThat(result).isEmpty();
    }
}