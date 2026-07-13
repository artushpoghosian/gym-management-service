package com.gym.rest.dto.trainee;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TraineeShortDto {
    private String username;
    private String firstName;
    private String lastName;
}
