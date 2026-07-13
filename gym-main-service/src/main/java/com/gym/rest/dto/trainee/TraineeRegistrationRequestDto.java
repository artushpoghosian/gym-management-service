package com.gym.rest.dto.trainee;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDate;

@Data
public class TraineeRegistrationRequestDto {
    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    private LocalDate dateOfBirth;
    private String address;
}
