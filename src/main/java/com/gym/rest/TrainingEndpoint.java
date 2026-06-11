package com.gym.rest;

import com.gym.exception.ValidationException;
import com.gym.facade.GymFacade;
import com.gym.model.Trainee;
import com.gym.model.Trainer;
import com.gym.model.Training;
import com.gym.model.TrainingType;
import com.gym.rest.dto.training.TrainingRequestDto;
import com.gym.rest.dto.training.TrainingTypeDto;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@Tag(name = "Training Controller", description = "Endpoints for handling trainings and training types")
public class TrainingEndpoint {

    private final GymFacade gymFacade;

    public TrainingEndpoint(GymFacade gymFacade) {
        this.gymFacade = gymFacade;
    }

    @PostMapping("/trainings")
    @Operation(summary = "Add a new training", description = "Creates a new training record mapping a trainee and trainer.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully added training"),
            @ApiResponse(responseCode = "400", description = "Invalid input or missing entities"),
            @ApiResponse(responseCode = "401", description = "Unauthorized access")
    })
    public ResponseEntity<Void> addTraining(
            @RequestHeader(value = "username", required = false) String authUsername,
            @RequestHeader(value = "password", required = false) String authPassword,
            @Valid @RequestBody TrainingRequestDto requestDto) {

        Trainee trainee = gymFacade.selectTrainee(authUsername, authPassword, requestDto.getTraineeUsername())
                .orElseThrow(() -> new ValidationException("Trainee not found for username: " + requestDto.getTraineeUsername()));

        Trainer trainer = gymFacade.selectTrainer(authUsername, authPassword, requestDto.getTrainerUsername())
                .orElseThrow(() -> new ValidationException("Trainer not found for username: " + requestDto.getTrainerUsername()));

        Training training = new Training();
        training.setTrainee(trainee);
        training.setTrainer(trainer);
        training.setTrainingName(requestDto.getTrainingName());
        training.setTrainingDate(requestDto.getTrainingDate());
        training.setTrainingDuration(requestDto.getTrainingDuration());

        training.setTrainingType(trainer.getSpecialization());

        gymFacade.createTraining(authUsername, authPassword, training);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/training-types")
    @Operation(summary = "Get all Training types", description = "Fetches a static list of all available training types from the system.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of training types", content = @Content(schema = @Schema(implementation = TrainingTypeDto.class)))
    })
    public ResponseEntity<List<TrainingTypeDto>> getTrainingTypes() {

        List<TrainingTypeDto> types = Arrays.stream(TrainingType.values())
                .map(type -> new TrainingTypeDto(type.name(), (long) type.ordinal() + 1))
                .collect(Collectors.toList());

        return ResponseEntity.ok(types);
    }
}