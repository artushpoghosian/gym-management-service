package com.gym.rest.dto.trainee;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class TraineeUpdateRequestDto {
    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    private LocalDate dateOfBirth;
    private String address;

    @NotNull(message = "Is Active status is required")
    private Boolean isActive;
}
