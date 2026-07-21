package com.gym.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class WorkloadMessagePublisher {

    private final JmsTemplate jmsTemplate;
    private final WorkloadMessagePostProcessor postProcessor;

    @Value("${workload.queue}")
    private String queue;

    public void publish(WorkloadRequest request) {
        jmsTemplate.convertAndSend(queue, request, postProcessor);
        log.info("Published {} workload for trainer={}", request.getActionType(), request.getTrainerUsername());
    }
}
