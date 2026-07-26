package com.gym.workload.service;

import com.gym.workload.document.MonthSummary;
import com.gym.workload.document.TrainerWorkload;
import com.gym.workload.dto.ActionType;
import com.gym.workload.dto.WorkloadRequest;
import com.gym.workload.repository.TrainerWorkloadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
@DataMongoTest
@Import(WorkloadService.class)
@Testcontainers(disabledWithoutDocker = true)
class WorkloadServiceIntegrationTest {

    @Container
    @ServiceConnection
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7");

    private static final String USERNAME = "trainer.jane";

    @Autowired
    private WorkloadService workloadService;

    @Autowired
    private TrainerWorkloadRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    void newEvent_forUnknownTrainer_createsRecordWithYearMonthBucketFromTrainingDate() {
        workloadService.process(addEvent(LocalDate.of(2026, 7, 10), 60));

        TrainerWorkload doc = repository.findByUsername(USERNAME).orElseThrow();
        assertThat(doc.getFirstName()).isEqualTo("Jane");
        assertThat(doc.getLastName()).isEqualTo("Doe");
        assertThat(doc.isActive()).isTrue();
        assertThat(doc.getYears()).singleElement().satisfies(year -> {
            assertThat(year.getYear()).isEqualTo(2026);
            assertThat(year.getMonths()).singleElement()
                    .extracting(MonthSummary::getMonth, MonthSummary::getSummaryDuration)
                    .containsExactly(7, 60L);
        });
    }

    @Test
    void newEvent_forExistingBucket_addsDurationToStoredValue() {
        workloadService.process(addEvent(LocalDate.of(2026, 7, 1), 60));
        workloadService.process(addEvent(LocalDate.of(2026, 7, 20), 30));

        assertThat(monthDuration(2026, 7)).isEqualTo(90);
    }

    @Test
    void newEvent_forNewBucketOnExistingTrainer_createsBucketAndLeavesOthersIntact() {
        workloadService.process(addEvent(LocalDate.of(2026, 7, 1), 60));

        workloadService.process(addEvent(LocalDate.of(2026, 8, 1), 45));
        workloadService.process(addEvent(LocalDate.of(2027, 1, 1), 30));

        assertThat(monthDuration(2026, 7)).isEqualTo(60);
        assertThat(monthDuration(2026, 8)).isEqualTo(45);
        assertThat(monthDuration(2027, 1)).isEqualTo(30);
    }

    @Test
    void process_refreshesTrainerPersonalDataFromEvent() {
        workloadService.process(addEvent(LocalDate.of(2026, 7, 1), 60));

        WorkloadRequest updated = addEvent(LocalDate.of(2026, 7, 2), 30);
        updated.setTrainerLastName("Smith");
        updated.setActive(false);
        workloadService.process(updated);

        TrainerWorkload doc = repository.findByUsername(USERNAME).orElseThrow();
        assertThat(doc.getLastName()).isEqualTo("Smith");
        assertThat(doc.isActive()).isFalse();
        assertThat(monthDuration(2026, 7)).isEqualTo(90);
    }

    private WorkloadRequest addEvent(LocalDate date, int minutes) {
        WorkloadRequest req = new WorkloadRequest();
        req.setTrainerUsername(USERNAME);
        req.setTrainerFirstName("Jane");
        req.setTrainerLastName("Doe");
        req.setActive(true);
        req.setTrainingDate(date);
        req.setTrainingDurationMinutes(minutes);
        req.setActionType(ActionType.ADD);
        return req;
    }

    private long monthDuration(int year, int month) {
        return repository.findByUsername(USERNAME)
                .stream()
                .flatMap(w -> w.getYears().stream())
                .filter(y -> y.getYear() == year)
                .flatMap(y -> y.getMonths().stream())
                .filter(m -> m.getMonth() == month)
                .mapToLong(MonthSummary::getSummaryDuration)
                .findFirst()
                .orElse(0L);
    }
}
