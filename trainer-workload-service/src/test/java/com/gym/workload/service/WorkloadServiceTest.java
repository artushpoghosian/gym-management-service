package com.gym.workload.service;

import com.gym.workload.dto.ActionType;
import com.gym.workload.dto.WorkloadRequest;
import com.gym.workload.model.TrainerSummary;
import com.gym.workload.store.TrainerSummaryStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class WorkloadServiceTest {

    private static final String USERNAME = "trainer.jane";

    private TrainerSummaryStore store;
    private WorkloadService workloadService;

    @BeforeEach
    void setUp() {
        store = new TrainerSummaryStore();
        workloadService = new WorkloadService(store);
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

    private long monthTotal(int year, int month) {
        return store.find(USERNAME)
                .map(s -> s.getYears().getOrDefault(year, java.util.Map.of()).getOrDefault(month, 0L))
                .orElse(0L);
    }

    @Test
    void add_createsSummaryAndIncreasesMonthBucket() {
        workloadService.process(request(ActionType.ADD, LocalDate.of(2026, 7, 10), 60));

        assertThat(monthTotal(2026, 7)).isEqualTo(60);
    }

    @Test
    void add_accumulatesInSameMonthBucket() {
        workloadService.process(request(ActionType.ADD, LocalDate.of(2026, 7, 1), 60));
        workloadService.process(request(ActionType.ADD, LocalDate.of(2026, 7, 20), 30));

        assertThat(monthTotal(2026, 7)).isEqualTo(90);
    }

    @Test
    void add_separatesDifferentMonthsAndYears() {
        workloadService.process(request(ActionType.ADD, LocalDate.of(2026, 7, 1), 60));
        workloadService.process(request(ActionType.ADD, LocalDate.of(2026, 8, 1), 45));
        workloadService.process(request(ActionType.ADD, LocalDate.of(2027, 7, 1), 30));

        assertThat(monthTotal(2026, 7)).isEqualTo(60);
        assertThat(monthTotal(2026, 8)).isEqualTo(45);
        assertThat(monthTotal(2027, 7)).isEqualTo(30);
    }

    @Test
    void delete_decreasesMonthBucket() {
        workloadService.process(request(ActionType.ADD, LocalDate.of(2026, 7, 1), 90));
        workloadService.process(request(ActionType.DELETE, LocalDate.of(2026, 7, 1), 30));

        assertThat(monthTotal(2026, 7)).isEqualTo(60);
    }

    @Test
    void delete_neverGoesBelowZero() {
        workloadService.process(request(ActionType.ADD, LocalDate.of(2026, 7, 1), 30));
        workloadService.process(request(ActionType.DELETE, LocalDate.of(2026, 7, 1), 90));

        assertThat(monthTotal(2026, 7)).isZero();
    }

    @Test
    void delete_forUnknownTrainer_createsZeroedBucketNotNegative() {
        workloadService.process(request(ActionType.DELETE, LocalDate.of(2026, 7, 1), 60));

        assertThat(monthTotal(2026, 7)).isZero();
    }

    @Test
    void process_updatesTrainerPersonalDataOnEveryCall() {
        workloadService.process(request(ActionType.ADD, LocalDate.of(2026, 7, 1), 60));

        WorkloadRequest updated = request(ActionType.ADD, LocalDate.of(2026, 7, 2), 30);
        updated.setTrainerLastName("Smith");
        updated.setActive(false);
        workloadService.process(updated);

        TrainerSummary summary = store.find(USERNAME).orElseThrow();
        assertThat(summary.getFirstName()).isEqualTo("Jane");
        assertThat(summary.getLastName()).isEqualTo("Smith");
        assertThat(summary.isActive()).isFalse();
    }
}
