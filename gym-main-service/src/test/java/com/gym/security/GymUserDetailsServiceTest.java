package com.gym.security;

import com.gym.dao.TraineeDao;
import com.gym.dao.TrainerDao;
import com.gym.model.Trainee;
import com.gym.model.Trainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GymUserDetailsServiceTest {

    @Mock
    private TraineeDao traineeDao;

    @Mock
    private TrainerDao trainerDao;

    @InjectMocks
    private GymUserDetailsService userDetailsService;

    @Test
    void loadUserByUsername_TraineeFound_ReturnsUserWithRoleTrainee() {
        Trainee trainee = new Trainee();
        trainee.setUsername("alice.brown");
        trainee.setPassword("$2a$10$hashed");
        trainee.setActive(true);
        when(traineeDao.findById("alice.brown")).thenReturn(Optional.of(trainee));

        UserDetails details = userDetailsService.loadUserByUsername("alice.brown");

        assertThat(details.getUsername()).isEqualTo("alice.brown");
        assertThat(details.getPassword()).isEqualTo("$2a$10$hashed");
        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_TRAINEE");
    }

    @Test
    void loadUserByUsername_TrainerFound_ReturnsUserWithRoleTrainer() {
        when(traineeDao.findById("john.doe")).thenReturn(Optional.empty());
        Trainer trainer = new Trainer();
        trainer.setUsername("john.doe");
        trainer.setPassword("$2a$10$hashed");
        trainer.setActive(true);
        when(trainerDao.findById("john.doe")).thenReturn(Optional.of(trainer));

        UserDetails details = userDetailsService.loadUserByUsername("john.doe");

        assertThat(details.getUsername()).isEqualTo("john.doe");
        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_TRAINER");
    }

    @Test
    void loadUserByUsername_UnknownUser_ThrowsUsernameNotFoundException() {
        when(traineeDao.findById("ghost")).thenReturn(Optional.empty());
        when(trainerDao.findById("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("ghost");
    }

    @Test
    void loadUserByUsername_InactiveUser_IsDisabled() {
        Trainee trainee = new Trainee();
        trainee.setUsername("alice.brown");
        trainee.setPassword("$2a$10$hashed");
        trainee.setActive(false);
        when(traineeDao.findById("alice.brown")).thenReturn(Optional.of(trainee));

        UserDetails details = userDetailsService.loadUserByUsername("alice.brown");

        assertThat(details.isEnabled()).isFalse();
    }
}
