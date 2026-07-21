package com.gym.workload.rest;

import com.gym.workload.model.TrainerSummary;
import com.gym.workload.store.TrainerSummaryStore;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trainer-workload")
@RequiredArgsConstructor
public class WorkloadController {

    private final TrainerSummaryStore store;

    @GetMapping("/{username}")
    public ResponseEntity<TrainerSummary> getSummary(@PathVariable String username) {
        return store.find(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
