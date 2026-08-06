package com.gym.workload.bdd.support;

import com.gym.workload.dto.WorkloadRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class QueueSender {

    private final JmsTemplate jmsTemplate;
    private final JwtMinter jwtMinter;

    @Value("${workload.queue}")
    private String queue;

    public void sendValid(WorkloadRequest request) {
        send(request, jwtMinter.validToken("gym-management-service"));
    }

    public void send(WorkloadRequest request, String token) {
        jmsTemplate.convertAndSend(queue, request, message -> {
            if (token != null) {
                message.setStringProperty("jwtToken", token);
            }
            message.setStringProperty("transactionId", UUID.randomUUID().toString());
            return message;
        });
    }
}
