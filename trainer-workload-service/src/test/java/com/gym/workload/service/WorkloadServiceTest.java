package com.gym.workload.service;

import com.gym.workload.dto.ActionType;
import com.gym.workload.dto.WorkloadRequest;
import com.gym.workload.repository.TrainerInfo;
import com.gym.workload.repository.TrainerWorkloadRepository;
import com.gym.workload.repository.WorkloadDelta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

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

        verify(repository).applyWorkload(
                new TrainerInfo("trainer.jane", "Jane", "Doe", true),
                new WorkloadDelta(2026, 7, 60L, false));
    }

    @Test
    void delete_delegatesToRepositoryWithSubtractTrue() {
        workloadService.process(request(ActionType.DELETE, LocalDate.of(2026, 7, 10), 30));

        verify(repository).applyWorkload(
                new TrainerInfo("trainer.jane", "Jane", "Doe", true),
                new WorkloadDelta(2026, 7, 30L, true));
    }

    @Test
    void process_forwardsTrainerPersonalDataAndDate() {
        WorkloadRequest req = request(ActionType.ADD, LocalDate.of(2027, 3, 5), 45);
        req.setTrainerLastName("Smith");
        req.setActive(false);

        workloadService.process(req);

        verify(repository).applyWorkload(
                new TrainerInfo("trainer.jane", "Jane", "Smith", false),
                new WorkloadDelta(2027, 3, 45L, false));
    }
}
