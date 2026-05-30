package com.gym.service.impl;

import com.gym.dao.TraineeDao;
import com.gym.dao.TrainerDao;
import com.gym.exception.AuthenticationException;
import com.gym.exception.ValidationException;
import com.gym.model.Trainer;
import com.gym.model.Training;
import com.gym.model.TrainingType;
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
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
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
    private final String authUser = "auth.trainer";
    private final String authPass = "password123";

    @BeforeEach
    void setUp() {
        trainer = new Trainer();
        trainer.setId(1L);
        trainer.setFirstName("John");
        trainer.setLastName("Smith");
        trainer.setUsername("john.smith");
        trainer.setPassword("password123");
        trainer.setSpecialization(TrainingType.YOGA);
        trainer.setActive(true);
    }

    private void mockAuthenticationSuccess() {
        Trainer authTrainer = new Trainer();
        authTrainer.setUsername(authUser);
        authTrainer.setPassword(authPass);
        when(trainerDao.findById(authUser)).thenReturn(Optional.of(authTrainer));
    }

    private void mockAuthenticationFailure() {
        when(trainerDao.findById(authUser)).thenReturn(Optional.empty());
    }

    @Test
    void create_ShouldSetUsernamePasswordAndActive_AndSaveTrainer() {
        Trainer newTrainer = new Trainer();
        newTrainer.setFirstName("John");
        newTrainer.setLastName("Smith");
        newTrainer.setSpecialization(TrainingType.YOGA);

        when(userUtils.generateUsername(eq("John"), eq("Smith"), any(Predicate.class)))
                .thenReturn("john.smith");
        when(userUtils.generatePassword()).thenReturn("password123");
        when(trainerDao.save(newTrainer)).thenReturn(newTrainer);

        Trainer result = trainerService.create(newTrainer);

        assertThat(result.getUsername()).isEqualTo("john.smith");
        assertThat(result.getPassword()).isEqualTo("password123");
        assertThat(result.isActive()).isTrue();
        verify(trainerDao).save(newTrainer);
    }

    @Test
    void create_WhenFirstNameIsBlank_ShouldThrowValidationException() {
        trainer.setFirstName("");
        assertThatThrownBy(() -> trainerService.create(trainer))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("First name is required");
        verify(trainerDao, never()).save(any());
    }

    @Test
    void create_WhenLastNameIsNull_ShouldThrowValidationException() {
        trainer.setLastName(null);
        assertThatThrownBy(() -> trainerService.create(trainer))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Last name is required");
        verify(trainerDao, never()).save(any());
    }

    @Test
    void create_WhenSpecializationIsNull_ShouldThrowValidationException() {
        trainer.setSpecialization(null);
        assertThatThrownBy(() -> trainerService.create(trainer))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Specialization is required");
        verify(trainerDao, never()).save(any());
    }

    @Test
    void matchCredentials_ShouldReturnTrue_WhenCredentialsMatch() {
        when(trainerDao.findById("john.smith")).thenReturn(Optional.of(trainer));
        boolean result = trainerService.matchCredentials("john.smith", "password123");
        assertThat(result).isTrue();
    }

    @Test
    void matchCredentials_ShouldReturnFalse_WhenPasswordIsIncorrect() {
        when(trainerDao.findById("john.smith")).thenReturn(Optional.of(trainer));
        boolean result = trainerService.matchCredentials("john.smith", "wrongPass");
        assertThat(result).isFalse();
    }

    @Test
    void matchCredentials_ShouldReturnFalse_WhenTrainerNotFound() {
        when(trainerDao.findById("unknown.user")).thenReturn(Optional.empty());
        boolean result = trainerService.matchCredentials("unknown.user", "password123");
        assertThat(result).isFalse();
    }

    @Test
    void selectTrainer_ShouldReturnTrainer_WhenAuthenticatedAndExists() {
        mockAuthenticationSuccess();
        String targetUser = "target.trainer";
        Trainer targetTrainer = new Trainer();
        when(trainerDao.findById(targetUser)).thenReturn(Optional.of(targetTrainer));

        Optional<Trainer> result = trainerService.selectTrainer(authUser, authPass, targetUser);

        assertThat(result).isPresent().contains(targetTrainer);
    }

    @Test
    void selectTrainer_ShouldThrowAuthenticationException_WhenAuthenticationFails() {
        mockAuthenticationFailure();
        assertThatThrownBy(() -> trainerService.selectTrainer(authUser, authPass, "target.trainer"))
                .isInstanceOf(AuthenticationException.class);
    }

    @Test
    void changePassword_ShouldUpdatePassword_WhenAuthenticatedAndValid() {
        Trainer authTrainer = new Trainer();
        authTrainer.setUsername("john.smith");
        authTrainer.setPassword("oldPass");
        when(trainerDao.findById("john.smith")).thenReturn(Optional.of(authTrainer));

        trainerService.changePassword("john.smith", "oldPass", "newPass");

        verify(trainerDao).updatePassword("john.smith", "newPass");
    }

    @Test
    void changePassword_ShouldThrowValidationException_WhenNewPasswordIsBlank() {
        Trainer authTrainer = new Trainer();
        authTrainer.setUsername("john.smith");
        authTrainer.setPassword("oldPass");
        when(trainerDao.findById("john.smith")).thenReturn(Optional.of(authTrainer));

        assertThatThrownBy(() -> trainerService.changePassword("john.smith", "oldPass", "   "))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("New password must not be blank");
    }

    @Test
    void update_ShouldReturnUpdatedTrainer_WhenAuthenticatedAndValid() {
        mockAuthenticationSuccess();
        when(trainerDao.update(trainer)).thenReturn(trainer);

        Trainer result = trainerService.update(authUser, authPass, trainer);

        assertThat(result).isEqualTo(trainer);
        verify(trainerDao).update(trainer);
    }

    @Test
    void update_ShouldThrowValidationException_WhenRequiredFieldsAreMissing() {
        mockAuthenticationSuccess();
        trainer.setLastName("");

        assertThatThrownBy(() -> trainerService.update(authUser, authPass, trainer))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void setActive_ShouldSucceed_WhenTargetStateIsDifferent() {
        mockAuthenticationSuccess();
        String targetUser = "target.trainer";
        Trainer targetTrainer = new Trainer();
        targetTrainer.setActive(false);
        when(trainerDao.findById(targetUser)).thenReturn(Optional.of(targetTrainer));

        trainerService.setActive(authUser, authPass, targetUser, true);

        verify(trainerDao).updateStatus(targetUser, true);
    }

    @Test
    void setActive_ShouldThrowValidationException_WhenTrainerNotFound() {
        mockAuthenticationSuccess();
        String targetUser = "unknown.trainer";
        when(trainerDao.findById(targetUser)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trainerService.setActive(authUser, authPass, targetUser, true))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Trainer not found");
    }

    @Test
    void setActive_ShouldThrowValidationException_WhenStateIsAlreadySame() {
        mockAuthenticationSuccess();
        String targetUser = "target.trainer";
        Trainer targetTrainer = new Trainer();
        targetTrainer.setActive(true);
        when(trainerDao.findById(targetUser)).thenReturn(Optional.of(targetTrainer));

        assertThatThrownBy(() -> trainerService.setActive(authUser, authPass, targetUser, true))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("is already active");
        verify(trainerDao, never()).updateStatus(anyString(), anyBoolean());
    }

    @Test
    void getTrainings_ShouldReturnTrainings_WhenAuthenticated() {
        mockAuthenticationSuccess();
        String targetUser = "target.trainer";
        LocalDate from = LocalDate.now().minusDays(2);
        LocalDate to = LocalDate.now();
        List<Training> expectedTrainings = List.of(new Training());

        when(trainerDao.findTrainingsByCriteria(targetUser, from, to, "Alice"))
                .thenReturn(expectedTrainings);

        List<Training> result = trainerService.getTrainings(authUser, authPass, targetUser, from, to, "Alice");

        assertThat(result).isEqualTo(expectedTrainings);
    }

    @Test
    void findAll_ShouldReturnAllTrainers() {
        List<Trainer> allTrainers = List.of(trainer);
        when(trainerDao.findAll()).thenReturn(allTrainers);

        List<Trainer> result = trainerService.findAll();

        assertThat(result).isEqualTo(allTrainers);
    }
}