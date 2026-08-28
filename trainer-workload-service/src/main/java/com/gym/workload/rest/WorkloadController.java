package com.gym.workload.rest;

import com.gym.workload.document.TrainerWorkload;
import com.gym.workload.repository.TrainerWorkloadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trainer-workload")
@RequiredArgsConstructor
public class WorkloadController {

    private final TrainerWorkloadRepository repository;

    @GetMapping("/{username}")
    public ResponseEntity<TrainerWorkload> getSummary(@PathVariable String username) {
        return repository.findByUsername(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public List<TrainerWorkload> searchByName(@RequestParam String firstName,
                                              @RequestParam String lastName) {
        return repository.findByFirstNameAndLastName(firstName, lastName);
    }
}
