package com.gym.rest;

import com.gym.exception.AuthenticationException;
import com.gym.facade.GymFacade;
import com.gym.rest.dto.auth.LoginRequestDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthEndpoint {

    private final GymFacade gymFacade;

    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginRequestDto loginRequest) {
        log.info("Received login request for username: {}", loginRequest.getUsername());

        boolean isTrainee = gymFacade.traineeMatchCredentials(loginRequest.getUsername(), loginRequest.getPassword());
        boolean isTrainer = gymFacade.trainerMatchCredentials(loginRequest.getUsername(), loginRequest.getPassword());

        if (!isTrainee && !isTrainer) {
            log.error("Authentication failed for username: {}", loginRequest.getUsername());
            throw new AuthenticationException("Invalid username or password");
        }

        log.info("Successfully authenticated username: {}", loginRequest.getUsername());
        return new ResponseEntity<>(HttpStatus.OK);
    }
}