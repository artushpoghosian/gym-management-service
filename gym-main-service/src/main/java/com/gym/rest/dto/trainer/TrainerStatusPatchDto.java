package com.gym.rest.dto.trainer;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TrainerStatusPatchDto {
    @NotNull(message = "Is Active status is required")
    private Boolean isActive;
}
