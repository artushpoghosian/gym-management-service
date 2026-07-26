package com.gym.workload.service;

import com.gym.workload.dto.ActionType;
import com.gym.workload.dto.WorkloadRequest;
import com.gym.workload.repository.TrainerWorkloadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WorkloadServiceTest {

    private static final String USERNAME = "trainer.jane";

    private TrainerWorkloadRepository repository;
    private WorkloadService workloadService;

    @BeforeEach
    void setUp() {
        repository = mock(TrainerWorkloadRepository.class);
        workloadService = new WorkloadService(repository);
    }

    private WorkloadRequest request(ActionType action, LocalDate date, int minutes) {
        WorkloadRequest req = new WorkloadRequest();
        req.setTrainerUsername(USERNAME);
        req.setTrainerFirstName("Jane");
        req.setTrainerLastName("Doe");
        req.setActive(true);
        req.setTrainingDate(date);
        req.setTrainingDurationMinutes(minutes);
        req.setActionType(action);
        return req;
    }

    @Test
    void add_delegatesToRepositoryWithSubtractFalse() {
        workloadService.process(request(ActionType.ADD, LocalDate.of(2026, 7, 10), 60));

        verify(repository).applyWorkload("trainer.jane", "Jane", "Doe", true, 2026, 7, 60L, false);
    }

    @Test
    void delete_delegatesToRepositoryWithSubtractTrue() {
        workloadService.process(request(ActionType.DELETE, LocalDate.of(2026, 7, 10), 30));

        verify(repository).applyWorkload("trainer.jane", "Jane", "Doe", true, 2026, 7, 30L, true);
    }

    @Test
    void process_forwardsTrainerPersonalDataAndDate() {
        WorkloadRequest req = request(ActionType.ADD, LocalDate.of(2027, 3, 5), 45);
        req.setTrainerLastName("Smith");
        req.setActive(false);

        workloadService.process(req);

        ArgumentCaptor<Integer> year = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> month = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Long> minutes = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Boolean> subtract = ArgumentCaptor.forClass(Boolean.class);
        verify(repository).applyWorkload(
                org.mockito.ArgumentMatchers.eq("trainer.jane"),
                org.mockito.ArgumentMatchers.eq("Jane"),
                org.mockito.ArgumentMatchers.eq("Smith"),
                org.mockito.ArgumentMatchers.eq(false),
                year.capture(), month.capture(), minutes.capture(), subtract.capture());

        assertThat(year.getValue()).isEqualTo(2027);
        assertThat(month.getValue()).isEqualTo(3);
        assertThat(minutes.getValue()).isEqualTo(45L);
        assertThat(subtract.getValue()).isFalse();
    }
}
