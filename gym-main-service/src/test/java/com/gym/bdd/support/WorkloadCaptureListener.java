package com.gym.bdd.support;

import com.gym.client.WorkloadRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkloadCaptureListener {

    private final PublishedWorkloadMessages published;

    @JmsListener(destination = "${workload.queue}")
    public void capture(@Payload WorkloadRequest request) {
        published.add(request);
    }
}
