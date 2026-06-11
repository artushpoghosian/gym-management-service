package com.gym.rest.dto.trainee;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TraineeStatusPatchDto {
    @NotNull(message = "Is Active status is required")
    private Boolean isActive;
}