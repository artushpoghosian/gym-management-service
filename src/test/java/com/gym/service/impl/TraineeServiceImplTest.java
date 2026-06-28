package com.gym.service.impl;

import com.gym.dao.TraineeDao;
import com.gym.dao.TrainerDao;
import com.gym.exception.AuthenticationException;
import com.gym.exception.ValidationException;
import com.gym.model.Trainee;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraineeServiceImplTest {

    @Mock
    private TraineeDao traineeDao;

    @Mock
    private TrainerDao trainerDao;

    @Mock
    private UserUtils userUtils;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private TraineeServiceImpl traineeService;

    private Trainee trainee;
    private final String authUser = "auth.trainee";
    private final String authPass = "password123";

    @BeforeEach
    void setUp() {
        trainee = new Trainee();
        trainee.setId(1L);
        trainee.setFirstName("Alice");
        trainee.setLastName("Brown");
        trainee.setUsername("alice.brown");
        trainee.setPassword("$2a$10$hashedpassword");
        trainee.setDateOfBirth(LocalDate.of(1995, 4, 12));
        trainee.setAddress("123 Main St");
        trainee.setActive(true);
    }

    private void mockAuthenticationSuccess() {
        Trainee authTrainee = new Trainee();
        authTrainee.setUsername(authUser);
        authTrainee.setPassword("$2a$10$hashedauthpass");
        when(traineeDao.findById(authUser)).thenReturn(Optional.of(authTrainee));
        when(passwordEncoder.matches(authPass, "$2a$10$hashedauthpass")).thenReturn(true);
    }

    private void mockAuthenticationFailure() {
        when(traineeDao.findById(authUser)).thenReturn(Optional.empty());
    }

    @Test
    void create_ShouldSetUsernamePasswordAndActive_AndSave() {
        Trainee newTrainee = new Trainee();
        newTrainee.setFirstName("Alice");
        newTrainee.setLastName("Brown");

        when(userUtils.generateUsername(eq("Alice"), eq("Brown"), any(Predicate.class)))
                .thenReturn("alice.brown");
        when(userUtils.generatePassword()).thenReturn("pass123456");
        when(passwordEncoder.encode("pass123456")).thenReturn("$2a$10$encoded");
        when(traineeDao.save(newTrainee)).thenReturn(newTrainee);

        Trainee result = traineeService.create(newTrainee);

        assertThat(result.getUsername()).isEqualTo("alice.brown");
        assertThat(result.getPassword()).isEqualTo("pass123456"); // plain password returned
        assertThat(result.isActive()).isTrue();
        verify(traineeDao).save(newTrainee);
    }

    @Test
    void create_WhenFirstNameBlank_ShouldThrowValidationException() {
        trainee.setFirstName("");
        assertThatThrownBy(() -> traineeService.create(trainee))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("First name is required");
        verify(traineeDao, never()).save(any());
    }

    @Test
    void create_WhenLastNameNull_ShouldThrowValidationException() {
        trainee.setLastName(null);
        assertThatThrownBy(() -> traineeService.create(trainee))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Last name is required");
        verify(traineeDao, never()).save(any());
    }

    @Test
    void matchCredentials_ShouldReturnTrue_WhenCredentialsMatch() {
        when(traineeDao.findById("alice.brown")).thenReturn(Optional.of(trainee));
        when(passwordEncoder.matches("pass123456", trainee.getPassword())).thenReturn(true);

        boolean result = traineeService.matchCredentials("alice.brown", "pass123456");

        assertThat(result).isTrue();
    }

    @Test
    void matchCredentials_ShouldReturnFalse_WhenPasswordIncorrect() {
        when(traineeDao.findById("alice.brown")).thenReturn(Optional.of(trainee));
        when(passwordEncoder.matches("wrongpass", trainee.getPassword())).thenReturn(false);

        boolean result = traineeService.matchCredentials("alice.brown", "wrongpass");

        assertThat(result).isFalse();
    }

    @Test
    void matchCredentials_ShouldReturnFalse_WhenUserNotFound() {
        when(traineeDao.findById("unknown")).thenReturn(Optional.empty());
        boolean result = traineeService.matchCredentials("unknown", "pass");
        assertThat(result).isFalse();
    }

    @Test
    void selectTrainee_ShouldReturnTrainee_WhenAuthenticatedAndExists() {
        mockAuthenticationSuccess();
        String targetUser = "target.trainee";
        Trainee targetTrainee = new Trainee();
        when(traineeDao.findById(targetUser)).thenReturn(Optional.of(targetTrainee));

        Optional<Trainee> result = traineeService.selectTrainee(authUser, authPass, targetUser);

        assertThat(result).isPresent().contains(targetTrainee);
    }

    @Test
    void selectTrainee_ShouldThrowAuthenticationException_WhenAuthenticationFails() {
        mockAuthenticationFailure();
        assertThatThrownBy(() -> traineeService.selectTrainee(authUser, authPass, "target.trainee"))
                .isInstanceOf(AuthenticationException.class);
    }

    @Test
    void changePassword_ShouldUpdatePassword_WhenAuthenticatedAndValid() {
        Trainee authTrainee = new Trainee();
        authTrainee.setUsername("alice.brown");
        authTrainee.setPassword("$2a$10$hashedold");
        when(traineeDao.findById("alice.brown")).thenReturn(Optional.of(authTrainee));
        when(passwordEncoder.matches("oldPass", "$2a$10$hashedold")).thenReturn(true);
        when(passwordEncoder.encode("newPass")).thenReturn("$2a$10$hashednew");

        traineeService.changePassword("alice.brown", "oldPass", "newPass");

        verify(traineeDao).updatePassword("alice.brown", "$2a$10$hashednew");
    }

    @Test
    void changePassword_ShouldThrowValidationException_WhenNewPasswordIsBlank() {
        Trainee authTrainee = new Trainee();
        authTrainee.setUsername("alice.brown");
        authTrainee.setPassword("$2a$10$hashedold");
        when(traineeDao.findById("alice.brown")).thenReturn(Optional.of(authTrainee));
        when(passwordEncoder.matches("oldPass", "$2a$10$hashedold")).thenReturn(true);

        assertThatThrownBy(() -> traineeService.changePassword("alice.brown", "oldPass", " "))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("New password must not be blank");
    }

    @Test
    void update_ShouldReturnUpdatedTrainee_WhenAuthenticatedAndValid() {
        mockAuthenticationSuccess();
        when(traineeDao.update(trainee)).thenReturn(trainee);

        Trainee result = traineeService.update(authUser, authPass, trainee);

        assertThat(result).isEqualTo(trainee);
        verify(traineeDao).update(trainee);
    }

    @Test
    void update_ShouldThrowValidationException_WhenRequiredFieldsMissing() {
        mockAuthenticationSuccess();
        trainee.setFirstName(null);

        assertThatThrownBy(() -> traineeService.update(authUser, authPass, trainee))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void setActive_ShouldSucceed_WhenStateIsDifferent() {
        mockAuthenticationSuccess();
        String targetUser = "target.trainee";
        Trainee targetTrainee = new Trainee();
        targetTrainee.setActive(false);
        when(traineeDao.findById(targetUser)).thenReturn(Optional.of(targetTrainee));

        traineeService.setActive(authUser, authPass, targetUser, true);
    }

    @Test
    void setActive_ShouldThrowValidationException_WhenTraineeNotFound() {
        mockAuthenticationSuccess();
        String targetUser = "unknown.trainee";
        when(traineeDao.findById(targetUser)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> traineeService.setActive(authUser, authPass, targetUser, true))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Trainee not found");
    }

    @Test
    void setActive_ShouldThrowValidationException_WhenStateIsSame() {
        mockAuthenticationSuccess();
        String targetUser = "target.trainee";
        Trainee targetTrainee = new Trainee();
        targetTrainee.setActive(true);
        when(traineeDao.findById(targetUser)).thenReturn(Optional.of(targetTrainee));

        assertThatThrownBy(() -> traineeService.setActive(authUser, authPass, targetUser, true))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("is already active");
    }

    @Test
    void delete_ShouldDelegateToDao_WhenAuthenticated() {
        mockAuthenticationSuccess();
        String targetUser = "target.trainee";

        traineeService.delete(authUser, authPass, targetUser);

        verify(traineeDao).delete(targetUser);
    }

    @Test
    void getTrainings_ShouldReturnTrainings_WhenAuthenticated() {
        mockAuthenticationSuccess();
        String targetUser = "target.trainee";
        LocalDate from = LocalDate.now().minusDays(1);
        LocalDate to = LocalDate.now().plusDays(1);
        List<Training> expectedTrainings = List.of(new Training());
        when(traineeDao.findTrainingsByCriteria(targetUser, from, to, "John", "YOGA"))
                .thenReturn(expectedTrainings);

        List<Training> result = traineeService.getTrainings(
                authUser, authPass, targetUser, from, to, "John", TrainingType.YOGA);

        assertThat(result).isEqualTo(expectedTrainings);
    }

    @Test
    void getTrainings_ShouldPassNullTypeName_WhenTrainingTypeIsNull() {
        mockAuthenticationSuccess();
        String targetUser = "target.trainee";
        List<Training> expectedTrainings = List.of(new Training());
        when(traineeDao.findTrainingsByCriteria(targetUser, null, null, null, null))
                .thenReturn(expectedTrainings);

        List<Training> result = traineeService.getTrainings(
                authUser, authPass, targetUser, null, null, null, null);

        assertThat(result).isEqualTo(expectedTrainings);
    }

    @Test
    void getUnassignedTrainers_ShouldReturnTrainers_WhenAuthenticated() {
        mockAuthenticationSuccess();
        String targetUser = "target.trainee";
        List<Trainer> expectedTrainers = List.of(new Trainer());
        when(traineeDao.findTrainersNotAssignedToTrainee(targetUser)).thenReturn(expectedTrainers);

        List<Trainer> result = traineeService.getUnassignedTrainers(authUser, authPass, targetUser);

        assertThat(result).isEqualTo(expectedTrainers);
    }

    @Test
    void updateTrainersList_ShouldUpdateAndReturnTrainee_WhenValid() {
        mockAuthenticationSuccess();
        String targetUser = "target.trainee";
        Trainee targetTrainee = new Trainee();

        when(traineeDao.findById(targetUser))
                .thenReturn(Optional.of(targetTrainee))
                .thenReturn(Optional.of(targetTrainee));

        Trainer trainer1 = new Trainer();
        when(trainerDao.findById("trainer1")).thenReturn(Optional.of(trainer1));

        Trainee result = traineeService.updateTrainersList(authUser, authPass, targetUser, List.of("trainer1"));

        verify(traineeDao).updateTraineeTrainers(targetUser, List.of(trainer1));
        assertThat(result).isNotNull();
    }

    @Test
    void updateTrainersList_ShouldThrowValidationException_WhenTraineeNotFound() {
        mockAuthenticationSuccess();
        String targetUser = "target.trainee";
        when(traineeDao.findById(targetUser)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> traineeService.updateTrainersList(authUser, authPass, targetUser, List.of("trainer1")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Trainee not found");
    }

    @Test
    void updateTrainersList_ShouldThrowValidationException_WhenTrainerNotFound() {
        mockAuthenticationSuccess();
        String targetUser = "target.trainee";
        Trainee targetTrainee = new Trainee();
        when(traineeDao.findById(targetUser)).thenReturn(Optional.of(targetTrainee));
        when(trainerDao.findById("unknown.trainer")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> traineeService.updateTrainersList(authUser, authPass, targetUser, List.of("unknown.trainer")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Trainer not found");
    }

    @Test
    void findAll_ShouldReturnAllTrainees() {
        List<Trainee> allTrainees = List.of(trainee);
        when(traineeDao.findAll()).thenReturn(allTrainees);

        List<Trainee> result = traineeService.findAll();

        assertThat(result).isEqualTo(allTrainees);
    }
}
