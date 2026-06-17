package com.gym.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

@Slf4j
@Component("memory")
public class MemoryHealthIndicator implements HealthIndicator {

    private final MemoryMXBean memoryMXBean;
    private final double warnThreshold;
    private final double criticalThreshold;

    public MemoryHealthIndicator(
            @Value("${health.memory.warn-threshold:0.80}") double warnThreshold,
            @Value("${health.memory.critical-threshold:0.90}") double criticalThreshold) {
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();
        this.warnThreshold = warnThreshold;
        this.criticalThreshold = criticalThreshold;
    }

    @Override
    public Health health() {
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        long used = heapUsage.getUsed();
        long max = heapUsage.getMax();

        if (max <= 0) {
            return Health.unknown()
                    .withDetail("reason", "JVM did not report a maximum heap size")
                    .build();
        }

        double usageRatio = (double) used / max;

        Health.Builder builder = usageRatio >= criticalThreshold ? Health.down() : Health.up();

        if (usageRatio >= criticalThreshold) {
            log.warn("Heap usage critical: {}% (threshold {}%)",
                    Math.round(usageRatio * 100), Math.round(criticalThreshold * 100));
        } else if (usageRatio >= warnThreshold) {
            log.warn("Heap usage elevated: {}% (warn threshold {}%)",
                    Math.round(usageRatio * 100), Math.round(warnThreshold * 100));
        }

        return builder
                .withDetail("usedBytes", used)
                .withDetail("maxBytes", max)
                .withDetail("usageRatio", Math.round(usageRatio * 10000) / 10000.0)
                .withDetail("warnThreshold", warnThreshold)
                .withDetail("criticalThreshold", criticalThreshold)
                .build();
    }
}