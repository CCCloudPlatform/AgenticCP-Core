package com.agenticcp.core.domain.monitoring.health.dto;

import com.agenticcp.core.domain.platform.entity.PlatformHealth.HealthStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HealthIndicatorResultTest {

    @Test
    void healthy_WithMessage_ShouldCreateHealthyResult() {
        // When
        HealthIndicatorResult result = HealthIndicatorResult.healthy("Database is healthy");

        // Then
        assertThat(result.getStatus()).isEqualTo(HealthStatus.HEALTHY);
        assertThat(result.getMessage()).isEqualTo("Database is healthy");
        assertThat(result.getTimestamp()).isNotNull();
        assertThat(result.getDetails()).isNull();
    }

    @Test
    void healthy_WithMessageAndDetails_ShouldCreateHealthyResult() {
        // Given
        Map<String, Object> details = new HashMap<>();
        details.put("connectionCount", 10);
        details.put("responseTime", 150L);

        // When
        HealthIndicatorResult result = HealthIndicatorResult.healthy("Database is healthy", details);

        // Then
        assertThat(result.getStatus()).isEqualTo(HealthStatus.HEALTHY);
        assertThat(result.getMessage()).isEqualTo("Database is healthy");
        assertThat(result.getDetails()).isEqualTo(details);
        assertThat(result.getTimestamp()).isNotNull();
    }

    @Test
    void warning_WithMessage_ShouldCreateWarningResult() {
        // When
        HealthIndicatorResult result = HealthIndicatorResult.warning("High memory usage");

        // Then
        assertThat(result.getStatus()).isEqualTo(HealthStatus.WARNING);
        assertThat(result.getMessage()).isEqualTo("High memory usage");
        assertThat(result.getTimestamp()).isNotNull();
        assertThat(result.getDetails()).isNull();
    }

    @Test
    void warning_WithMessageAndDetails_ShouldCreateWarningResult() {
        // Given
        Map<String, Object> details = new HashMap<>();
        details.put("memoryUsage", 85.5);

        // When
        HealthIndicatorResult result = HealthIndicatorResult.warning("High memory usage", details);

        // Then
        assertThat(result.getStatus()).isEqualTo(HealthStatus.WARNING);
        assertThat(result.getMessage()).isEqualTo("High memory usage");
        assertThat(result.getDetails()).isEqualTo(details);
        assertThat(result.getTimestamp()).isNotNull();
    }

    @Test
    void critical_WithMessage_ShouldCreateCriticalResult() {
        // When
        HealthIndicatorResult result = HealthIndicatorResult.critical("Database connection failed");

        // Then
        assertThat(result.getStatus()).isEqualTo(HealthStatus.CRITICAL);
        assertThat(result.getMessage()).isEqualTo("Database connection failed");
        assertThat(result.getTimestamp()).isNotNull();
        assertThat(result.getDetails()).isNull();
    }

    @Test
    void critical_WithMessageAndDetails_ShouldCreateCriticalResult() {
        // Given
        Map<String, Object> details = new HashMap<>();
        details.put("errorCode", "CONNECTION_TIMEOUT");
        details.put("retryCount", 3);

        // When
        HealthIndicatorResult result = HealthIndicatorResult.critical("Database connection failed", details);

        // Then
        assertThat(result.getStatus()).isEqualTo(HealthStatus.CRITICAL);
        assertThat(result.getMessage()).isEqualTo("Database connection failed");
        assertThat(result.getDetails()).isEqualTo(details);
        assertThat(result.getTimestamp()).isNotNull();
    }

    @Test
    void builder_ShouldCreateResultWithAllFields() {
        // Given
        Map<String, Object> details = new HashMap<>();
        details.put("test", "value");
        LocalDateTime timestamp = LocalDateTime.now();

        // When
        HealthIndicatorResult result = HealthIndicatorResult.builder()
                .status(HealthStatus.HEALTHY)
                .message("Test message")
                .details(details)
                .timestamp(timestamp)
                .build();

        // Then
        assertThat(result.getStatus()).isEqualTo(HealthStatus.HEALTHY);
        assertThat(result.getMessage()).isEqualTo("Test message");
        assertThat(result.getDetails()).isEqualTo(details);
        assertThat(result.getTimestamp()).isEqualTo(timestamp);
    }
}

