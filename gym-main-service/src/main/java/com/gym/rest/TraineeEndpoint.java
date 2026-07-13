package com.gym.rest;

import com.gym.exception.ResourceNotFoundException;
import com.gym.facade.GymFacade;
import com.gym.model.Trainee;
import com.gym.model.Trainer;
import com.gym.model.Training;
import com.gym.model.TrainingType;
import com.gym.rest.dto.trainee.TraineeProfileResponseDto;
import com.gym.rest.dto.trainee.TraineeRegistrationRequestDto;
import com.gym.rest.dto.trainee.TraineeRegistrationResponseDto;
import com.gym.rest.dto.trainee.TraineeStatusPatchDto;
import com.gym.rest.dto.trainee.TraineeUpdateRequestDto;
import com.gym.rest.dto.trainer.TrainerShortDto;
import com.gym.rest.dto.training.TrainingResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/trainees")
@Tag(name = "Trainee Management System")
@RequiredArgsConstructor
@Slf4j
public class TraineeEndpoint {

    private final GymFacade gymFacade;

    @PostMapping
    @Operation(summary = "Register a new Trainee", description = "Creates a new trainee profile and returns generated credentials.")
    public ResponseEntity<TraineeRegistrationResponseDto> registerTrainee(
            @Valid @RequestBody TraineeRegistrationRequestDto requestDto) {

        log.info("Endpoint: Registering new trainee: {} {}", requestDto.getFirstName(), requestDto.getLastName());

        Trainee newTrainee = new Trainee();
        newTrainee.setFirstName(requestDto.getFirstName());
        newTrainee.setLastName(requestDto.getLastName());
        newTrainee.setDateOfBirth(requestDto.getDateOfBirth());
        newTrainee.setAddress(requestDto.getAddress());

        Trainee createdTrainee = gymFacade.createTrainee(newTrainee);

        return ResponseEntity.ok(new TraineeRegistrationResponseDto(
                createdTrainee.getUsername(), createdTrainee.getPassword()));
    }

    @GetMapping("/{username}")
    @Operation(summary = "Get Trainee Profile", description = "Retrieves profile details...")    public ResponseEntity<TraineeProfileResponseDto> getTraineeProfile(
            @Parameter(description = "Target Trainee Username") @PathVariable String username,
            @RequestHeader("X-Auth-Username") String authUser,
            @RequestHeader("X-Auth-Password") String authPass) {

        log.info("Endpoint: Fetching profile for trainee: {}", username);
        Trainee trainee = gymFacade.selectTrainee(authUser, authPass, username)
                .orElseThrow(() -> new ResourceNotFoundException("Trainee not found: " + username));

        TraineeProfileResponseDto response = new TraineeProfileResponseDto();
        response.setFirstName(trainee.getFirstName());
        response.setLastName(trainee.getLastName());
        response.setDateOfBirth(trainee.getDateOfBirth());
        response.setAddress(trainee.getAddress());
        response.setActive(trainee.isActive());

        response.setTrainers(List.of());

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{username}")
    @Operation(summary = "Update Trainee Profile", description = "Updates details of an existing trainee.")
    public ResponseEntity<TraineeProfileResponseDto> updateTraineeProfile(
            @PathVariable String username,
            @RequestHeader("X-Auth-Username") String authUser,
            @RequestHeader("X-Auth-Password") String authPass,
            @Valid @RequestBody TraineeUpdateRequestDto requestDto) {

        log.info("Endpoint: Updating profile for trainee: {}", username);

        Trainee traineeToUpdate = new Trainee();
        traineeToUpdate.setUsername(username);
        traineeToUpdate.setFirstName(requestDto.getFirstName());
        traineeToUpdate.setLastName(requestDto.getLastName());
        traineeToUpdate.setDateOfBirth(requestDto.getDateOfBirth());
        traineeToUpdate.setAddress(requestDto.getAddress());
        traineeToUpdate.setActive(requestDto.getIsActive());

        Trainee updatedTrainee = gymFacade.updateTrainee(authUser, authPass, traineeToUpdate);

        TraineeProfileResponseDto response = new TraineeProfileResponseDto();
        response.setFirstName(updatedTrainee.getFirstName());
        response.setLastName(updatedTrainee.getLastName());
        response.setDateOfBirth(updatedTrainee.getDateOfBirth());
        response.setAddress(updatedTrainee.getAddress());
        response.setActive(updatedTrainee.isActive());
        response.setTrainers(List.of());

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{username}")
    @Operation(summary = "Delete Trainee Profile", description = "Hard deletes a trainee profile and cascaded trainings.")
    public ResponseEntity<Void> deleteTraineeProfile(
            @PathVariable String username,
            @RequestHeader("X-Auth-Username") String authUser,
            @RequestHeader("X-Auth-Password") String authPass) {

        log.info("Endpoint: Deleting profile for trainee: {}", username);
        gymFacade.deleteTrainee(authUser, authPass, username);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PatchMapping("/{username}/status")
    @Operation(summary = "Activate/Deactivate Trainee", description = "Changes the active status of a trainee.")
    public ResponseEntity<Void> updateTraineeStatus(
            @PathVariable String username,
            @RequestHeader("X-Auth-Username") String authUser,
            @RequestHeader("X-Auth-Password") String authPass,
            @Valid @RequestBody TraineeStatusPatchDto statusDto) {

        log.info("Endpoint: Patching status for trainee: {}", username);
        gymFacade.setTraineeActive(authUser, authPass, username, statusDto.getIsActive());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/{username}/trainers/not-assigned")
    @Operation(summary = "Get Unassigned Active Trainers", description = "Retrieves active trainers not currently assigned to the trainee.")
    public ResponseEntity<List<TrainerShortDto>> getUnassignedTrainers(
            @PathVariable String username,
            @RequestHeader("X-Auth-Username") String authUser,
            @RequestHeader("X-Auth-Password") String authPass) {

        log.info("Endpoint: Fetching unassigned trainers for trainee: {}", username);
        List<Trainer> trainers = gymFacade.getUnassignedTrainers(authUser, authPass, username);

        List<TrainerShortDto> dtos = trainers.stream()
                .map(t -> new TrainerShortDto(t.getUsername(), t.getFirstName(), t.getLastName(), t.getSpecialization().name()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/{username}/trainers")
    @Operation(summary = "Update Trainee's Trainer List", description = "Replaces the list of trainers assigned to a trainee.")
    public ResponseEntity<List<TrainerShortDto>> updateTraineeTrainers(
            @PathVariable String username,
            @RequestHeader("X-Auth-Username") String authUser,
            @RequestHeader("X-Auth-Password") String authPass,
            @RequestBody List<String> trainerUsernames) {

        log.info("Endpoint: Updating assigned trainers for trainee: {}", username);
        Trainee trainee = gymFacade.updateTraineeTrainersList(authUser, authPass, username, trainerUsernames);

        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/{username}/trainings")
    @Operation(summary = "Get Trainee Trainings List", description = "Retrieves a filtered list of trainings for a specific trainee.")
    public ResponseEntity<List<TrainingResponseDto>> getTraineeTrainings(
            @PathVariable String username,
            @RequestHeader("X-Auth-Username") String authUser,
            @RequestHeader("X-Auth-Password") String authPass,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodTo,
            @RequestParam(required = false) String trainerName,
            @RequestParam(required = false) TrainingType trainingType) {

        log.info("Endpoint: Fetching trainings for trainee: {}", username);
        List<Training> trainings = gymFacade.getTraineeTrainings(
                authUser, authPass, username, periodFrom, periodTo, trainerName, trainingType);

        List<TrainingResponseDto> responseList = trainings.stream().map(t -> {
            TrainingResponseDto dto = new TrainingResponseDto();
            dto.setTrainingName(t.getTrainingName());
            dto.setTrainingDate(t.getTrainingDate());
            dto.setTrainingType(t.getTrainingType().name());
            dto.setTrainingDuration(t.getTrainingDuration());
            dto.setTrainerName(t.getTrainer().getUsername());
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(responseList);
    }
}
