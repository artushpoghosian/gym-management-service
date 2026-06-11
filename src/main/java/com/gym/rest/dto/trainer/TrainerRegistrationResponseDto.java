package com.gym.rest.dto.trainer;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TrainerRegistrationResponseDto {
    private String username;
    private String password;
}