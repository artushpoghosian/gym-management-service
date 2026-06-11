package com.gym.rest.dto.trainee;

import com.gym.rest.dto.trainer.TrainerShortDto;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class TraineeProfileResponseDto {
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String address;
    private boolean isActive;
    private List<TrainerShortDto> trainers;
}