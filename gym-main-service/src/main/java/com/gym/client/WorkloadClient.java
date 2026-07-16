package com.gym.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class WorkloadClient {

    private final WorkloadFeignClient workloadFeignClient;

    @CircuitBreaker(name = "workloadService", fallbackMethod = "sendFallback")
    public void send(WorkloadRequest req) {
        workloadFeignClient.sendWorkload(req);
        log.info("Sent {} workload for trainer={}", req.getActionType(), req.getTrainerUsername());
    }

    @SuppressWarnings("unused")
    void sendFallback(WorkloadRequest req, Throwable t) {
        log.error("[CB] trainer-workload-service unreachable, skipping {} notification for trainer={}: {}",
                req.getActionType(), req.getTrainerUsername(), t.getMessage());
    }
}
