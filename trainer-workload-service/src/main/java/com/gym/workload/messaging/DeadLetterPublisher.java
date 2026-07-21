package com.gym.workload.messaging;

import com.gym.workload.dto.WorkloadRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class DeadLetterPublisher {

    private static final String TRANSACTION_ID = "transactionId";

    private final JmsTemplate jmsTemplate;

    @Value("${workload.queue}")
    private String sourceQueue;

    @Value("${workload.dlq}")
    private String dlq;

    public void send(WorkloadRequest request, String reason) {
        jmsTemplate.convertAndSend(dlq, request, message -> {
            message.setStringProperty("dlqReason", reason);
            message.setStringProperty("originalQueue", sourceQueue);
            String transactionId = MDC.get(TRANSACTION_ID);
            if (transactionId != null) {
                message.setStringProperty(TRANSACTION_ID, transactionId);
            }
            return message;
        });
        log.warn("[DLQ] routed invalid message to {}: {}", dlq, reason);
    }
}
