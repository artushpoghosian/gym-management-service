package com.gym.workload.repository;

import com.gym.workload.document.MonthSummary;
import com.gym.workload.document.TrainerWorkload;
import com.gym.workload.document.YearSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@Testcontainers(disabledWithoutDocker = true)
class TrainerWorkloadRepositoryTest {

    @Container
    @ServiceConnection
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7");

    private static final String USERNAME = "trainer.jane";

    @Autowired
    private TrainerWorkloadRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    void findByFirstNameAndLastName_returnsMatchingTrainers() {
        repository.save(trainer("trainer.jane", "Jane", "Doe"));
        repository.save(trainer("trainer.john", "John", "Doe"));

        assertThat(repository.findByFirstNameAndLastName("Jane", "Doe"))
                .extracting(TrainerWorkload::getUsername)
                .containsExactly("trainer.jane");
        assertThat(repository.findByFirstNameAndLastName("No", "Body")).isEmpty();
    }

    @Test
    void compoundIndexOnFirstAndLastNameIsCreated() {
        repository.save(trainer("trainer.jane", "Jane", "Doe"));

        assertThat(mongoTemplate.indexOps(TrainerWorkload.class).getIndexInfo())
                .extracting(IndexInfo::getName)
                .contains("idx_first_last");
    }

    private TrainerWorkload trainer(String username, String firstName, String lastName) {
        TrainerWorkload w = new TrainerWorkload();
        w.setUsername(username);
        w.setFirstName(firstName);
        w.setLastName(lastName);
        w.setActive(true);
        return w;
    }

    @Test
    void findByUsername_returnsSavedDocument() {
        TrainerWorkload workload = new TrainerWorkload();
        workload.setUsername(USERNAME);
        workload.setFirstName("Jane");
        workload.setLastName("Doe");
        workload.setActive(true);
        workload.setYears(List.of(new YearSummary(2026, List.of(new MonthSummary(7, 60L)))));
        repository.save(workload);

        Optional<TrainerWorkload> found = repository.findByUsername(USERNAME);
        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("Jane");
        assertThat(found.get().getYears()).singleElement()
                .satisfies(y -> assertThat(y.getMonths()).singleElement()
                        .extracting(MonthSummary::getMonth, MonthSummary::getSummaryDuration)
                        .containsExactly(7, 60L));
    }

    @Test
    void findByUsername_unknown_returnsEmpty() {
        assertThat(repository.findByUsername("nobody")).isEmpty();
    }

    @Test
    void apply_add_createsDocumentAndBucket() {
        add(2026, 7, 60);

        assertThat(monthTotal(2026, 7)).isEqualTo(60);
        TrainerWorkload doc = repository.findByUsername(USERNAME).orElseThrow();
        assertThat(doc.getFirstName()).isEqualTo("Jane");
        assertThat(doc.isActive()).isTrue();
    }

    @Test
    void apply_add_accumulatesInSameMonthBucket() {
        add(2026, 7, 60);
        add(2026, 7, 30);

        assertThat(monthTotal(2026, 7)).isEqualTo(90);
    }

    @Test
    void apply_add_separatesDifferentMonthsAndYears() {
        add(2026, 7, 60);
        add(2026, 8, 45);
        add(2027, 7, 30);

        assertThat(monthTotal(2026, 7)).isEqualTo(60);
        assertThat(monthTotal(2026, 8)).isEqualTo(45);
        assertThat(monthTotal(2027, 7)).isEqualTo(30);
    }

    @Test
    void apply_delete_decreasesMonthBucket() {
        add(2026, 7, 90);
        delete(2026, 7, 30);

        assertThat(monthTotal(2026, 7)).isEqualTo(60);
    }

    @Test
    void apply_delete_neverGoesBelowZero() {
        add(2026, 7, 30);
        delete(2026, 7, 90);

        assertThat(monthTotal(2026, 7)).isZero();
    }

    @Test
    void apply_delete_forUnknownTrainer_createsZeroedBucketNotNegative() {
        delete(2026, 7, 60);

        assertThat(monthTotal(2026, 7)).isZero();
        assertThat(repository.findByUsername(USERNAME)).isPresent();
    }

    @Test
    void apply_refreshesPersonalDataOnEveryCall() {
        repository.applyWorkload(USERNAME, "Jane", "Doe", true, 2026, 7, 60L, false);
        repository.applyWorkload(USERNAME, "Jane", "Smith", false, 2026, 7, 30L, false);

        TrainerWorkload doc = repository.findByUsername(USERNAME).orElseThrow();
        assertThat(doc.getFirstName()).isEqualTo("Jane");
        assertThat(doc.getLastName()).isEqualTo("Smith");
        assertThat(doc.isActive()).isFalse();
        assertThat(monthTotal(2026, 7)).isEqualTo(90);
    }

    private void add(int year, int month, long minutes) {
        repository.applyWorkload(USERNAME, "Jane", "Doe", true, year, month, minutes, false);
    }

    private void delete(int year, int month, long minutes) {
        repository.applyWorkload(USERNAME, "Jane", "Doe", true, year, month, minutes, true);
    }

    private long monthTotal(int year, int month) {
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
