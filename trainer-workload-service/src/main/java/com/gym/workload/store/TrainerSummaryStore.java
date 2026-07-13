package com.gym.workload.store;

import com.gym.workload.model.TrainerSummary;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TrainerSummaryStore {

    private final Map<String, TrainerSummary> store = new ConcurrentHashMap<>();

    public TrainerSummary getOrCreate(String username) {
        return store.computeIfAbsent(username, k -> {
            TrainerSummary summary = new TrainerSummary();
            summary.setUsername(username);
            return summary;
        });
    }

    public Optional<TrainerSummary> find(String username) {
        return Optional.ofNullable(store.get(username));
    }
}
