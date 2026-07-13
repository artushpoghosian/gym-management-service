package com.gym.workload.model;

import lombok.Data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class TrainerSummary {

    private String username;
    private String firstName;
    private String lastName;
    private boolean active;

    private Map<Integer, Map<Integer, Long>> years = new ConcurrentHashMap<>();
}
