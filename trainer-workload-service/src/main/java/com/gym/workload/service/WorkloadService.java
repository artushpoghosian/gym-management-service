package com.gym.workload.service;

import com.gym.workload.dto.ActionType;
import com.gym.workload.dto.WorkloadRequest;
import com.gym.workload.model.TrainerSummary;
import com.gym.workload.store.TrainerSummaryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkloadService {

    private final TrainerSummaryStore store;

    public void process(WorkloadRequest request) {
        TrainerSummary summary = store.getOrCreate(request.getTrainerUsername());

        summary.setFirstName(request.getTrainerFirstName());
        summary.setLastName(request.getTrainerLastName());
        summary.setActive(request.isActive());

        int year = request.getTrainingDate().getYear();
        int month = request.getTrainingDate().getMonthValue();
        long duration = request.getTrainingDurationMinutes();

        Map<Integer, Long> months = summary.getYears()
                .computeIfAbsent(year, k -> new ConcurrentHashMap<>());

        if (request.getActionType() == ActionType.ADD) {
            months.merge(month, duration, Long::sum);
            log.info("ADD {} min for trainer={} {}/{}", duration, request.getTrainerUsername(), year, month);
        } else {
            months.compute(month, (k, current) ->
                    current == null ? 0L : Math.max(0, current - duration));
            log.info("DELETE {} min for trainer={} {}/{}", duration, request.getTrainerUsername(), year, month);
        }
    }
}
