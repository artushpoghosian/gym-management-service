package com.gym.rest.dto.trainer;

import com.gym.rest.dto.trainee.TraineeShortDto;
import lombok.Data;
import java.util.List;

@Data
public class TrainerProfileResponseDto {
    private String firstName;
    private String lastName;
    private String specialization;
    private boolean isActive;
    private List<TraineeShortDto> trainees;
}
