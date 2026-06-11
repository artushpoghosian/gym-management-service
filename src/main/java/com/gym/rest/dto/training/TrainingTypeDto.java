package com.gym.rest.dto.training;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainingTypeDto {
    private String trainingType;
    private Long trainingTypeId;
}