package com.gym.workload.repository;

public record WorkloadDelta(int year, int month, long minutes, boolean subtract) {
}
