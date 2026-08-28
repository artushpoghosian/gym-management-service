package com.gym.workload.repository;

import com.gym.workload.document.TrainerWorkload;
import com.mongodb.client.model.UpdateOptions;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class TrainerWorkloadRepositoryImpl implements TrainerWorkloadRepositoryCustom {

    private static final String COND = "$cond";

    private final MongoTemplate mongoTemplate;

    @Override
    public Optional<TrainerWorkload> findByUsername(String username) {
        Query query = new Query(Criteria.where("username").is(username));
        return Optional.ofNullable(mongoTemplate.findOne(query, TrainerWorkload.class));
    }

    @Override
    public void applyWorkload(TrainerInfo trainer, WorkloadDelta delta) {

        int year = delta.year();
        int month = delta.month();
        long minutes = delta.minutes();
        boolean subtract = delta.subtract();

        long freshDuration = subtract ? 0L : minutes;
        Document newMonth = new Document("month", month).append("summaryDuration", freshDuration);

        Object existingMonthDuration = subtract
                ? new Document("$max", List.of(0L, new Document("$subtract", List.of("$$m.summaryDuration", minutes))))
                : new Document("$add", List.of("$$m.summaryDuration", minutes));

        Document monthsUpdated = new Document(COND, List.of(
                new Document("$in", List.of(month, "$$y.months.month")),
                new Document("$map", new Document("input", "$$y.months").append("as", "m").append("in",
                        new Document(COND, List.of(
                                new Document("$eq", List.of("$$m.month", month)),
                                new Document("month", month).append("summaryDuration", existingMonthDuration),
                                "$$m")))),
                new Document("$concatArrays", List.of("$$y.months", List.of(newMonth)))));

        Document yearsUpdated = new Document("$let", new Document()
                .append("vars", new Document("yrs", new Document("$ifNull", List.of("$years", List.of()))))
                .append("in", new Document(COND, List.of(
                        new Document("$in", List.of(year, "$$yrs.year")),
                        new Document("$map", new Document("input", "$$yrs").append("as", "y").append("in",
                                new Document(COND, List.of(
                                        new Document("$eq", List.of("$$y.year", year)),
                                        new Document("year", year).append("months", monthsUpdated),
                                        "$$y")))),
                        new Document("$concatArrays", List.of("$$yrs",
                                List.of(new Document("year", year).append("months", List.of(newMonth)))))))));

        List<Document> pipeline = List.of(
                new Document("$set", new Document()
                        .append("firstName", trainer.firstName())
                        .append("lastName", trainer.lastName())
                        .append("active", trainer.active())),
                new Document("$set", new Document("years", yearsUpdated)));

        mongoTemplate.getCollection(mongoTemplate.getCollectionName(TrainerWorkload.class))
                .updateOne(new Document("_id", trainer.username()), pipeline, new UpdateOptions().upsert(true));
    }
}
