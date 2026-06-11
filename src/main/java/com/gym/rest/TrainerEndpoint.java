package com.gym.rest;

import com.gym.facade.GymFacade;
import com.gym.model.Trainer;
import com.gym.model.Training;
import com.gym.rest.dto.trainer.TrainerProfileResponseDto;
import com.gym.rest.dto.trainer.TrainerRegistrationRequestDto;
import com.gym.rest.dto.trainer.TrainerRegistrationResponseDto;
import com.gym.rest.dto.trainer.TrainerShortDto;
import com.gym.rest.dto.trainer.TrainerStatusPatchDto;
import com.gym.rest.dto.trainer.TrainerUpdateRequestDto;
import com.gym.rest.dto.training.TrainingResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@RequestMapping("/trainers")
@Tag(name = "Trainer Management System")
@RequiredArgsConstructor
@Slf4j
public class TrainerEndpoint {

    private final GymFacade gymFacade;

    @PostMapping
    @Operation(summary = "Register a new Trainer", description = "Creates a new trainer profile and returns generated credentials.")
    public ResponseEntity<TrainerRegistrationResponseDto> registerTrainer(
            @Valid @RequestBody TrainerRegistrationRequestDto requestDto) {

        log.info("Endpoint: Registering new trainer: {} {}", requestDto.getFirstName(), requestDto.getLastName());

        Trainer newTrainer = new Trainer();
        newTrainer.setFirstName(requestDto.getFirstName());
        newTrainer.setLastName(requestDto.getLastName());
        newTrainer.setSpecialization(requestDto.getSpecialization());

        Trainer createdTrainer = gymFacade.createTrainer(newTrainer);

        return ResponseEntity.ok(new TrainerRegistrationResponseDto(
                createdTrainer.getUsername(), createdTrainer.getPassword()));
    }

    @GetMapping("/{username}")
    @Operation(summary = "Get Trainer Profile", description = "Retrieves profile details of a specific trainer.")
    public ResponseEntity<TrainerProfileResponseDto> getTrainerProfile(
            @Parameter(description = "Target Trainer Username") @PathVariable String username,
            @RequestHeader("X-Auth-Username") String authUser,
            @RequestHeader("X-Auth-Password") String authPass) {

        log.info("Endpoint: Fetching profile for trainer: {}", username);
        Trainer trainer = gymFacade.selectTrainer(authUser, authPass, username)
                .orElseThrow(() -> new RuntimeException("Trainer not found"));

        TrainerProfileResponseDto response = new TrainerProfileResponseDto();
        response.setFirstName(trainer.getFirstName());
        response.setLastName(trainer.getLastName());
        response.setSpecialization(trainer.getSpecialization().name());
        response.setActive(trainer.isActive());

        response.setTrainees(List.of());

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{username}")
    @Operation(summary = "Update Trainer Profile", description = "Updates details of an existing trainer. Specialization is included but typically read-only at the database level.")
    public ResponseEntity<TrainerProfileResponseDto> updateTrainerProfile(
            @PathVariable String username,
            @RequestHeader("X-Auth-Username") String authUser,
            @RequestHeader("X-Auth-Password") String authPass,
            @Valid @RequestBody TrainerUpdateRequestDto requestDto) {

        log.info("Endpoint: Updating profile for trainer: {}", username);

        Trainer trainerToUpdate = new Trainer();
        trainerToUpdate.setUsername(username);
        trainerToUpdate.setFirstName(requestDto.getFirstName());
        trainerToUpdate.setLastName(requestDto.getLastName());
        trainerToUpdate.setSpecialization(requestDto.getSpecialization());
        trainerToUpdate.setActive(requestDto.getIsActive());

        Trainer updatedTrainer = gymFacade.updateTrainer(authUser, authPass, trainerToUpdate);

        TrainerProfileResponseDto response = new TrainerProfileResponseDto();
        response.setFirstName(updatedTrainer.getFirstName());
        response.setLastName(updatedTrainer.getLastName());
        response.setSpecialization(updatedTrainer.getSpecialization().name());
        response.setActive(updatedTrainer.isActive());
        response.setTrainees(List.of());

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{username}/status")
    @Operation(summary = "Activate/Deactivate Trainer", description = "Changes the active status of a trainer.")
    public ResponseEntity<Void> updateTrainerStatus(
            @PathVariable String username,
            @RequestHeader("X-Auth-Username") String authUser,
            @RequestHeader("X-Auth-Password") String authPass,
            @Valid @RequestBody TrainerStatusPatchDto statusDto) {

        log.info("Endpoint: Patching status for trainer: {}", username);
        gymFacade.setTrainerActive(authUser, authPass, username, statusDto.getIsActive());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/{username}/trainings")
    @Operation(summary = "Get Trainer Trainings List", description = "Retrieves a filtered list of trainings for a specific trainer.")
    public ResponseEntity<List<TrainingResponseDto>> getTrainerTrainings(
            @PathVariable String username,
            @RequestHeader("X-Auth-Username") String authUser,
            @RequestHeader("X-Auth-Password") String authPass,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodTo,
            @RequestParam(required = false) String traineeName) {

        log.info("Endpoint: Fetching trainings for trainer: {}", username);
        List<Training> trainings = gymFacade.getTrainerTrainings(
                authUser, authPass, username, periodFrom, periodTo, traineeName);

        List<TrainingResponseDto> responseList = trainings.stream().map(t -> {
            TrainingResponseDto dto = new TrainingResponseDto();
            dto.setTrainingName(t.getTrainingName());
            dto.setTrainingDate(t.getTrainingDate());
            dto.setTrainingType(t.getTrainingType().name());
            dto.setTrainingDuration(t.getTrainingDuration());

            if (t.getTrainee() != null) {
                dto.setTrainerName(t.getTrainee().getUsername());
            }
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(responseList);
    }

    @GetMapping("/not-assigned")
    @Operation(summary = "Get Not Assigned Active Trainers", description = "Retrieves a list of active trainers who are not currently assigned to the specified trainee profile.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved unassigned trainers list",
                    content = @Content(schema = @Schema(implementation = TrainerShortDto.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized access"),
            @ApiResponse(responseCode = "404", description = "Trainee profile not found")
    })
    public ResponseEntity<List<TrainerShortDto>> getNotAssignedTrainers(
            @Parameter(name = "Username of the trainee to cross-reference unassigned trainers against", required = true) @RequestParam String traineeUsername,
            @RequestHeader("X-Auth-Username") String authUser,
            @RequestHeader("X-Auth-Password") String authPass) {

        log.info("Endpoint: Fetching unassigned trainers for trainee: {}", traineeUsername);
        List<Trainer> unassignedTrainers = gymFacade.getUnassignedTrainers(authUser, authPass, traineeUsername);

        List<TrainerShortDto> responseList = unassignedTrainers.stream().map(t -> {
            TrainerShortDto dto = new TrainerShortDto();
            dto.setUsername(t.getUsername());
            dto.setFirstName(t.getFirstName());
            dto.setLastName(t.getLastName());
            dto.setSpecialization(t.getSpecialization().name());
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(responseList);
    }
}