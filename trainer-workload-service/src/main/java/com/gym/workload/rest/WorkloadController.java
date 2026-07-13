package com.gym.workload.rest;

import com.gym.workload.dto.WorkloadRequest;
import com.gym.workload.model.TrainerSummary;
import com.gym.workload.service.WorkloadService;
import com.gym.workload.store.TrainerSummaryStore;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trainer-workload")
@RequiredArgsConstructor
public class WorkloadController {

    private final WorkloadService workloadService;
    private final TrainerSummaryStore store;

    @PostMapping
    public ResponseEntity<Void> updateWorkload(@Valid @RequestBody WorkloadRequest request) {
        workloadService.process(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{username}")
    public ResponseEntity<TrainerSummary> getSummary(@PathVariable String username) {
        return store.find(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
