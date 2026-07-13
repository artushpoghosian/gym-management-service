package com.gym.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
@Component("databaseResponsiveness")
public class DatabaseResponsivenessHealthIndicator implements HealthIndicator {

    private static final int VALIDATION_TIMEOUT_SECONDS = 2;

    private final DataSource dataSource;

    public DatabaseResponsivenessHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        long start = System.currentTimeMillis();
        try (Connection connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(VALIDATION_TIMEOUT_SECONDS);
            long elapsedMs = System.currentTimeMillis() - start;

            if (!valid) {
                log.warn("Database connection validation failed (not valid within {}s)", VALIDATION_TIMEOUT_SECONDS);
                return Health.down()
                        .withDetail("reason", "Connection did not validate within timeout")
                        .withDetail("timeoutSeconds", VALIDATION_TIMEOUT_SECONDS)
                        .build();
            }

            return Health.up()
                    .withDetail("responseTimeMs", elapsedMs)
                    .withDetail("database", safeProductName(connection))
                    .build();
        } catch (SQLException ex) {
            log.error("Database health check failed", ex);
            return Health.down(ex)
                    .withDetail("reason", ex.getMessage())
                    .build();
        }
    }

    private String safeProductName(Connection connection) {
        try {
            return connection.getMetaData().getDatabaseProductName();
        } catch (SQLException ex) {
            log.debug("Could not read database product name for health details", ex);
            return "unknown";
        }
    }
}
