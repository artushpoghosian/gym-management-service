package com.gym.client;

import com.gym.security.JwtService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
@RequiredArgsConstructor
public class WorkloadClient {

    private static final String WORKLOAD_URL =
            "http://trainer-workload-service/api/trainer-workload";

    private final RestTemplate restTemplate;
    private final JwtService jwtService;

    @CircuitBreaker(name = "workloadService", fallbackMethod = "sendFallback")
    public void send(WorkloadRequest req) {
        String token = jwtService.generateServiceToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        String txId = MDC.get("transactionId");
        if (txId != null) {
            headers.set("X-Transaction-Id", txId);
        }

        restTemplate.exchange(
                WORKLOAD_URL,
                HttpMethod.POST,
                new HttpEntity<>(req, headers),
                Void.class);

        log.info("Sent {} workload for trainer={}", req.getActionType(), req.getTrainerUsername());
    }

    @SuppressWarnings("unused")
    void sendFallback(WorkloadRequest req, Throwable t) {
        log.error("[CB] trainer-workload-service unreachable, skipping {} notification for trainer={}: {}",
                req.getActionType(), req.getTrainerUsername(), t.getMessage());
    }
}
