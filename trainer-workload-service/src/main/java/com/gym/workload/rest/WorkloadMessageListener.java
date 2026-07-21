package com.gym.workload.rest;

import com.gym.workload.dto.WorkloadRequest;
import com.gym.workload.messaging.DeadLetterPublisher;
import com.gym.workload.security.JwtService;
import com.gym.workload.service.WorkloadService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class WorkloadMessageListener {

    private static final String TRANSACTION_ID = "transactionId";

    private final WorkloadService workloadService;
    private final JwtService jwtService;
    private final Validator validator;
    private final DeadLetterPublisher deadLetterPublisher;

    @JmsListener(destination = "${workload.queue}")
    public void onWorkloadMessage(@Payload WorkloadRequest request,
                                  @Header(name = "jwtToken", required = false) String jwtToken,
                                  @Header(name = TRANSACTION_ID, required = false) String transactionId) {
        MDC.put(TRANSACTION_ID, transactionId != null ? transactionId : UUID.randomUUID().toString());
        try {
            if (jwtToken == null || !jwtService.isValid(jwtToken)) {
                log.warn("[MQ] rejected workload message with missing/invalid JWT for trainer={}",
                        request.getTrainerUsername());
                return;
            }

            String violations = validate(request);
            if (violations != null) {
                log.warn("[MQ] invalid workload message (missing required information): {}", violations);
                deadLetterPublisher.send(request, violations);
                return;
            }

            log.info("[MQ] received {} workload for trainer={}", request.getActionType(), request.getTrainerUsername());
            workloadService.process(request);
        } finally {
            MDC.clear();
        }
    }

    private String validate(WorkloadRequest request) {
        Set<ConstraintViolation<WorkloadRequest>> violations = validator.validate(request);
        if (violations.isEmpty()) {
            return null;
        }
        return violations.stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .sorted()
                .collect(Collectors.joining(", "));
    }
}
