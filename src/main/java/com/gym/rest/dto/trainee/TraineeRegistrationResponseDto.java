package com.gym.rest.dto.trainee;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TraineeRegistrationResponseDto {
    private String username;
    private String password;
}