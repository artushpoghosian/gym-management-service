package com.gym.workload.service;

import com.gym.workload.dto.ActionType;
import com.gym.workload.dto.WorkloadRequest;
import com.gym.workload.repository.TrainerInfo;
import com.gym.workload.repository.TrainerWorkloadRepository;
import com.gym.workload.repository.WorkloadDelta;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkloadService {

    private final TrainerWorkloadRepository repository;

    public void process(WorkloadRequest request) {
        int year = request.getTrainingDate().getYear();
        int month = request.getTrainingDate().getMonthValue();
        long minutes = request.getTrainingDurationMinutes();
        boolean subtract = request.getActionType() == ActionType.DELETE;

        repository.applyWorkload(
                new TrainerInfo(
                        request.getTrainerUsername(),
                        request.getTrainerFirstName(),
                        request.getTrainerLastName(),
                        request.getActive()),
                new WorkloadDelta(year, month, minutes, subtract));

        log.info("{} {} min for trainer={} {}/{}",
                subtract ? "DELETE" : "ADD", minutes, request.getTrainerUsername(), year, month);
    }
}
