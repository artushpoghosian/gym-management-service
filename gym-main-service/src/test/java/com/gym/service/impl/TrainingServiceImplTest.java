package com.gym.service.impl;

import com.gym.client.ActionType;
import com.gym.client.WorkloadClient;
import com.gym.client.WorkloadRequest;
import com.gym.dao.TraineeDao;
import com.gym.dao.TrainerDao;
import com.gym.dao.TrainingDao;
import com.gym.exception.AuthenticationException;
import com.gym.exception.ValidationException;
import com.gym.model.Trainee;
import com.gym.model.Trainer;
import com.gym.model.Training;
import com.gym.model.TrainingType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingServiceImplTest {

    @Mock
    private TrainingDao trainingDao;

    @Mock
    private TraineeDao traineeDao;

    @Mock
    private TrainerDao trainerDao;

    @Mock
    private Counter counter;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private WorkloadClient workloadClient;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @InjectMocks
    private TrainingServiceImpl trainingService;

    private Training training;
    private final String authUser = "test.user";
    private final String authPass = "password";

    @BeforeEach
    void setUp() {
        Trainee trainee = new Trainee();
        trainee.setUsername("trainee.one");

        Trainer trainer = new Trainer();
        trainer.setUsername("trainer.one");

        training = new Training();
        training.setId(1L);
        training.setTrainingName("Morning Yoga");
        training.setTrainingDate(LocalDate.of(2026, 6, 1));
        training.setTrainingDuration(60);
        training.setTrainee(trainee);
        training.setTrainer(trainer);
        training.setTrainingType(TrainingType.YOGA);
    }

    private void mockTraineeAuthenticationSuccess() {
        Trainee authTrainee = new Trainee();
        authTrainee.setUsername(authUser);
        authTrainee.setPassword("$2a$10$hashedauthpass");
        when(traineeDao.findById(authUser)).thenReturn(Optional.of(authTrainee));
        when(passwordEncoder.matches(authPass, "$2a$10$hashedauthpass")).thenReturn(true);
    }

    private void mockTrainerAuthenticationSuccess() {
        when(traineeDao.findById(authUser)).thenReturn(Optional.empty());
        Trainer authTrainer = new Trainer();
        authTrainer.setUsername(authUser);
        authTrainer.setPassword("$2a$10$hashedauthpass");
        when(trainerDao.findById(authUser)).thenReturn(Optional.of(authTrainer));
        when(passwordEncoder.matches(authPass, "$2a$10$hashedauthpass")).thenReturn(true);
    }

    private void mockAuthenticationFailure() {
        when(traineeDao.findById(authUser)).thenReturn(Optional.empty());
        when(trainerDao.findById(authUser)).thenReturn(Optional.empty());
    }

    @Test
    void create_ShouldSaveTraining_WhenAuthenticatedAsTraineeAndFieldsValid() {
        mockTraineeAuthenticationSuccess();
        when(trainingDao.save(training)).thenReturn(training);
        when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(counter);

        Training result = trainingService.create(authUser, authPass, training);

        assertThat(result).isEqualTo(training);
        verify(trainingDao).save(training);
    }

    @Test
    void create_ShouldSaveTraining_WhenAuthenticatedAsTrainerAndFieldsValid() {
        mockTrainerAuthenticationSuccess();
        when(trainingDao.save(training)).thenReturn(training);
        when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(counter);

        Training result = trainingService.create(authUser, authPass, training);

        assertThat(result).isEqualTo(training);
        verify(trainingDao).save(training);
    }

    @Test
    void create_ShouldNotifyWorkloadWithActionAdd() {
        mockTraineeAuthenticationSuccess();
        when(trainingDao.save(training)).thenReturn(training);
        when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(counter);

        trainingService.create(authUser, authPass, training);

        ArgumentCaptor<WorkloadRequest> captor = ArgumentCaptor.forClass(WorkloadRequest.class);
        verify(workloadClient).send(captor.capture());

        WorkloadRequest sent = captor.getValue();
        assertThat(sent.getActionType()).isEqualTo(ActionType.ADD);
        assertThat(sent.getTrainerUsername()).isEqualTo("trainer.one");
        assertThat(sent.getTrainingDate()).isEqualTo(training.getTrainingDate());
        assertThat(sent.getTrainingDurationMinutes()).isEqualTo(60);
    }

    @Test
    void delete_ShouldDeleteTrainingAndNotifyWorkloadWithActionDelete() {
        mockTraineeAuthenticationSuccess();
        when(trainingDao.findByName("Morning Yoga")).thenReturn(Optional.of(training));

        trainingService.delete(authUser, authPass, "Morning Yoga");

        verify(trainingDao).delete(training);

        ArgumentCaptor<WorkloadRequest> captor = ArgumentCaptor.forClass(WorkloadRequest.class);
        verify(workloadClient).send(captor.capture());

        WorkloadRequest sent = captor.getValue();
        assertThat(sent.getActionType()).isEqualTo(ActionType.DELETE);
        assertThat(sent.getTrainerUsername()).isEqualTo("trainer.one");
        assertThat(sent.getTrainingDurationMinutes()).isEqualTo(60);
    }

    @Test
    void delete_ShouldThrowValidationException_WhenTrainingNotFound() {
        mockTraineeAuthenticationSuccess();
        when(trainingDao.findByName("Unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trainingService.delete(authUser, authPass, "Unknown"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Training not found");

        verify(trainingDao, never()).delete(any());
        verify(workloadClient, never()).send(any());
    }

    @Test
    void delete_ShouldThrowAuthenticationException_WhenNotAuthenticated() {
        mockAuthenticationFailure();

        assertThatThrownBy(() -> trainingService.delete(authUser, authPass, "Morning Yoga"))
                .isInstanceOf(AuthenticationException.class);

        verify(trainingDao, never()).delete(any());
        verify(workloadClient, never()).send(any());
    }

    @Test
    void create_ShouldThrowAuthenticationException_WhenUserNotFoundAnywhere() {
        mockAuthenticationFailure();

        assertThatThrownBy(() -> trainingService.create(authUser, authPass, training))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Invalid username or password");
        verify(trainingDao, never()).save(any());
    }

    @Test
    void create_ShouldThrowAuthenticationException_WhenTraineePasswordWrongAndTrainerPasswordWrong() {
        Trainee badTrainee = new Trainee();
        badTrainee.setUsername(authUser);
        badTrainee.setPassword("wrong_pass");
        when(traineeDao.findById(authUser)).thenReturn(Optional.of(badTrainee));

        Trainer badTrainer = new Trainer();
        badTrainer.setUsername(authUser);
        badTrainer.setPassword("also_wrong");
        when(trainerDao.findById(authUser)).thenReturn(Optional.of(badTrainer));

        assertThatThrownBy(() -> trainingService.create(authUser, authPass, training))
                .isInstanceOf(AuthenticationException.class);
        verify(trainingDao, never()).save(any());
    }

    @Test
    void create_WhenTrainingNameIsBlank_ShouldThrowValidationException() {
        mockTraineeAuthenticationSuccess();
        training.setTrainingName("   ");

        assertThatThrownBy(() -> trainingService.create(authUser, authPass, training))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Training name is required");
        verify(trainingDao, never()).save(any());
    }

    @Test
    void create_WhenTrainingNameIsNull_ShouldThrowValidationException() {
        mockTraineeAuthenticationSuccess();
        training.setTrainingName(null);

        assertThatThrownBy(() -> trainingService.create(authUser, authPass, training))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Training name is required");
        verify(trainingDao, never()).save(any());
    }

    @Test
    void create_WhenTraineeIsNull_ShouldThrowValidationException() {
        mockTraineeAuthenticationSuccess();
        training.setTrainee(null);

        assertThatThrownBy(() -> trainingService.create(authUser, authPass, training))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Trainee is required");
        verify(trainingDao, never()).save(any());
    }

    @Test
    void create_WhenTrainerIsNull_ShouldThrowValidationException() {
        mockTraineeAuthenticationSuccess();
        training.setTrainer(null);

        assertThatThrownBy(() -> trainingService.create(authUser, authPass, training))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Trainer is required");
        verify(trainingDao, never()).save(any());
    }

    @Test
    void create_WhenTrainingTypeIsNull_ShouldThrowValidationException() {
        mockTraineeAuthenticationSuccess();
        training.setTrainingType(null);

        assertThatThrownBy(() -> trainingService.create(authUser, authPass, training))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Training type is required");
        verify(trainingDao, never()).save(any());
    }

    @Test
    void create_WhenTrainingDateIsNull_ShouldThrowValidationException() {
        mockTraineeAuthenticationSuccess();
        training.setTrainingDate(null);

        assertThatThrownBy(() -> trainingService.create(authUser, authPass, training))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Training date is required");
        verify(trainingDao, never()).save(any());
    }

    @Test
    void findByName_ShouldReturnTraining_WhenExists() {
        when(trainingDao.findByName("Morning Yoga")).thenReturn(Optional.of(training));

        Optional<Training> result = trainingService.findByName("Morning Yoga");

        assertThat(result).isPresent().contains(training);
    }

    @Test
    void findByName_ShouldReturnEmpty_WhenNotExists() {
        when(trainingDao.findByName("Unknown")).thenReturn(Optional.empty());

        Optional<Training> result = trainingService.findByName("Unknown");

        assertThat(result).isEmpty();
    }

    @Test
    void findAll_ShouldReturnAllTrainings() {
        List<Training> trainings = List.of(training);
        when(trainingDao.findAll()).thenReturn(trainings);

        List<Training> result = trainingService.findAll();

        assertThat(result).isEqualTo(trainings);
    }

    @Test
    void selectTraining_ShouldReturnTraining_WhenExists() {
        when(trainingDao.findByName("Morning Yoga")).thenReturn(Optional.of(training));

        Optional<Training> result = trainingService.selectTraining("Morning Yoga");

        assertThat(result).isPresent().contains(training);
    }

    @Test
    void selectTraining_ShouldReturnEmpty_WhenNotExists() {
        when(trainingDao.findByName("Unknown")).thenReturn(Optional.empty());

        Optional<Training> result = trainingService.selectTraining("Unknown");

        assertThat(result).isEmpty();
    }
}
