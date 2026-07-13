package com.gym.facade;

import com.gym.model.Trainee;
import com.gym.model.Trainer;
import com.gym.model.Training;
import com.gym.model.TrainingType;
import com.gym.service.TraineeService;
import com.gym.service.TrainerService;
import com.gym.service.TrainingService;
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
class GymFacadeTest {

    @Mock
    private TrainerService trainerService;

    @Mock
    private TraineeService traineeService;

    @Mock
    private TrainingService trainingService;

    @InjectMocks
    private GymFacade facade;

    private static final String AUTH_USER = "auth.user";
    private static final String AUTH_PASS = "secret";
    private static final String TARGET = "target.user";

    @Test
    void createTrainee_DelegatesToTraineeService() {
        Trainee trainee = new Trainee();
        when(traineeService.create(trainee)).thenReturn(trainee);

        assertThat(facade.createTrainee(trainee)).isEqualTo(trainee);
        verify(traineeService).create(trainee);
    }

    @Test
    void traineeMatchCredentials_DelegatesToTraineeService() {
        when(traineeService.matchCredentials(TARGET, AUTH_PASS)).thenReturn(true);

        assertThat(facade.traineeMatchCredentials(TARGET, AUTH_PASS)).isTrue();
        verify(traineeService).matchCredentials(TARGET, AUTH_PASS);
    }

    @Test
    void updateTrainee_DelegatesToTraineeService() {
        Trainee trainee = new Trainee();
        when(traineeService.update(AUTH_USER, AUTH_PASS, trainee)).thenReturn(trainee);

        assertThat(facade.updateTrainee(AUTH_USER, AUTH_PASS, trainee)).isEqualTo(trainee);
        verify(traineeService).update(AUTH_USER, AUTH_PASS, trainee);
    }

    @Test
    void setTraineeActive_DelegatesToTraineeService() {
        facade.setTraineeActive(AUTH_USER, AUTH_PASS, TARGET, true);
        verify(traineeService).setActive(AUTH_USER, AUTH_PASS, TARGET, true);
    }

    @Test
    void getTraineeTrainings_DelegatesToTraineeService() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 12, 31);
        List<Training> trainings = List.of(new Training());
        when(traineeService.getTrainings(AUTH_USER, AUTH_PASS, TARGET, from, to, "trainer.x", TrainingType.YOGA))
                .thenReturn(trainings);

        assertThat(facade.getTraineeTrainings(AUTH_USER, AUTH_PASS, TARGET, from, to, "trainer.x", TrainingType.YOGA))
                .isEqualTo(trainings);
    }

    @Test
    void deleteTrainee_DelegatesToTraineeService() {
        facade.deleteTrainee(AUTH_USER, AUTH_PASS, TARGET);
        verify(traineeService).delete(AUTH_USER, AUTH_PASS, TARGET);
    }

    @Test
    void selectTrainee_DelegatesToTraineeService() {
        Trainee trainee = new Trainee();
        when(traineeService.selectTrainee(AUTH_USER, AUTH_PASS, TARGET)).thenReturn(Optional.of(trainee));

        assertThat(facade.selectTrainee(AUTH_USER, AUTH_PASS, TARGET)).contains(trainee);
    }

    @Test
    void changeTraineePassword_DelegatesToTraineeService() {
        facade.changeTraineePassword(TARGET, "old", "new");
        verify(traineeService).changePassword(TARGET, "old", "new");
    }

    @Test
    void findAllTrainees_DelegatesToTraineeService() {
        List<Trainee> trainees = List.of(new Trainee());
        when(traineeService.findAll()).thenReturn(trainees);

        assertThat(facade.findAllTrainees()).isEqualTo(trainees);
    }

    @Test
    void getUnassignedTrainers_DelegatesToTraineeService() {
        List<Trainer> trainers = List.of(new Trainer());
        when(traineeService.getUnassignedTrainers(AUTH_USER, AUTH_PASS, TARGET)).thenReturn(trainers);

        assertThat(facade.getUnassignedTrainers(AUTH_USER, AUTH_PASS, TARGET)).isEqualTo(trainers);
    }

    @Test
    void updateTraineeTrainersList_DelegatesToTraineeService() {
        Trainee trainee = new Trainee();
        List<String> trainerUsernames = List.of("trainer.a", "trainer.b");
        when(traineeService.updateTrainersList(AUTH_USER, AUTH_PASS, TARGET, trainerUsernames))
                .thenReturn(trainee);

        assertThat(facade.updateTraineeTrainersList(AUTH_USER, AUTH_PASS, TARGET, trainerUsernames))
                .isEqualTo(trainee);
    }

    @Test
    void createTrainer_DelegatesToTrainerService() {
        Trainer trainer = new Trainer();
        when(trainerService.create(trainer)).thenReturn(trainer);

        assertThat(facade.createTrainer(trainer)).isEqualTo(trainer);
        verify(trainerService).create(trainer);
    }

    @Test
    void trainerMatchCredentials_DelegatesToTrainerService() {
        when(trainerService.matchCredentials(TARGET, AUTH_PASS)).thenReturn(true);

        assertThat(facade.trainerMatchCredentials(TARGET, AUTH_PASS)).isTrue();
    }

    @Test
    void updateTrainer_DelegatesToTrainerService() {
        Trainer trainer = new Trainer();
        when(trainerService.update(AUTH_USER, AUTH_PASS, trainer)).thenReturn(trainer);

        assertThat(facade.updateTrainer(AUTH_USER, AUTH_PASS, trainer)).isEqualTo(trainer);
    }

    @Test
    void setTrainerActive_DelegatesToTrainerService() {
        facade.setTrainerActive(AUTH_USER, AUTH_PASS, TARGET, false);
        verify(trainerService).setActive(AUTH_USER, AUTH_PASS, TARGET, false);
    }

    @Test
    void getTrainerTrainings_DelegatesToTrainerService() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 12, 31);
        List<Training> trainings = List.of(new Training());
        when(trainerService.getTrainings(AUTH_USER, AUTH_PASS, TARGET, from, to, "trainee.x"))
                .thenReturn(trainings);

        assertThat(facade.getTrainerTrainings(AUTH_USER, AUTH_PASS, TARGET, from, to, "trainee.x"))
                .isEqualTo(trainings);
    }

    @Test
    void selectTrainer_DelegatesToTrainerService() {
        Trainer trainer = new Trainer();
        when(trainerService.selectTrainer(AUTH_USER, AUTH_PASS, TARGET)).thenReturn(Optional.of(trainer));

        assertThat(facade.selectTrainer(AUTH_USER, AUTH_PASS, TARGET)).contains(trainer);
    }

    @Test
    void changeTrainerPassword_DelegatesToTrainerService() {
        facade.changeTrainerPassword(TARGET, "old", "new");
        verify(trainerService).changePassword(TARGET, "old", "new");
    }

    @Test
    void findAllTrainers_DelegatesToTrainerService() {
        List<Trainer> trainers = List.of(new Trainer());
        when(trainerService.findAll()).thenReturn(trainers);

        assertThat(facade.findAllTrainers()).isEqualTo(trainers);
    }

    @Test
    void createTraining_DelegatesToTrainingService() {
        Training training = new Training();
        training.setTrainingName("Morning Yoga");
        when(trainingService.create(AUTH_USER, AUTH_PASS, training)).thenReturn(training);

        assertThat(facade.createTraining(AUTH_USER, AUTH_PASS, training)).isEqualTo(training);
        verify(trainingService).create(AUTH_USER, AUTH_PASS, training);
    }

    @Test
    void findAllTrainings_DelegatesToTrainingService() {
        List<Training> trainings = List.of(new Training());
        when(trainingService.findAll()).thenReturn(trainings);

        assertThat(facade.findAllTrainings()).isEqualTo(trainings);
    }
}
