package com.gym.rest.dto.trainer;

import com.gym.model.TrainingType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TrainerUpdateRequestDto {
    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotNull(message = "Specialization is required")
    private TrainingType specialization;

    @NotNull(message = "Is Active status is required")
    private Boolean isActive;
}
