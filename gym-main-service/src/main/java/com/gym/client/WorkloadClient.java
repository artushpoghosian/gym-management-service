package com.gym.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class WorkloadClient {

    private final WorkloadMessagePublisher workloadMessagePublisher;

    public void send(WorkloadRequest req) {
        try {
            workloadMessagePublisher.publish(req);
        } catch (Exception e) {
            log.error("Failed to publish {} notification for trainer={}: {}",
                    req.getActionType(), req.getTrainerUsername(), e.getMessage());
        }
    }
}
