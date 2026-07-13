package com.gym.client;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class WorkloadRequest {

    private String trainerUsername;
    private String trainerFirstName;
    private String trainerLastName;
    private boolean isActive;
    private LocalDate trainingDate;
    private int trainingDurationMinutes;
    private ActionType actionType;
}
