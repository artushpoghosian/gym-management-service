package com.gym.workload.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "trainer_workloads")
@CompoundIndex(name = "idx_first_last", def = "{'firstName': 1, 'lastName': 1}")
public class TrainerWorkload {

    @Id
    private String username;

    private String firstName;
    private String lastName;
    private boolean active;

    private List<YearSummary> years = new ArrayList<>();

    @Version
    private Long version;
}
