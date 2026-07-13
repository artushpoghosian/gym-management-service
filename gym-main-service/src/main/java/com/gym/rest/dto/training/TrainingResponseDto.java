package com.gym.rest.dto.training;

import lombok.Data;
import java.time.LocalDate;

@Data
public class TrainingResponseDto {
    private String trainingName;
    private LocalDate trainingDate;
    private String trainingType;
    private int trainingDuration;
    private String trainerName;
}
