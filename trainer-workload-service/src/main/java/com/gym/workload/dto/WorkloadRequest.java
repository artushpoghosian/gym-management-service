package com.gym.workload.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;

@Data
public class WorkloadRequest {

    @NotBlank
    private String trainerUsername;

    @NotBlank
    private String trainerFirstName;

    @NotBlank
    private String trainerLastName;

    @NotNull
    @JsonProperty("active")
    private Boolean active;

    @NotNull
    private LocalDate trainingDate;

    @Positive
    private int trainingDurationMinutes;

    @NotNull
    private ActionType actionType;
}
