package com.gym.bdd.support;

import com.gym.client.WorkloadRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class PublishedWorkloadMessages {

    private final List<WorkloadRequest> messages = new CopyOnWriteArrayList<>();

    public void add(WorkloadRequest request) {
        messages.add(request);
    }

    public List<WorkloadRequest> all() {
        return List.copyOf(messages);
    }

    public void clear() {
        messages.clear();
    }
}
