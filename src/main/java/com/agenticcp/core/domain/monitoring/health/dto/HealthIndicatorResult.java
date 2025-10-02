package com.agenticcp.core.domain.monitoring.health.dto;

import com.agenticcp.core.domain.platform.entity.PlatformHealth.HealthStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthIndicatorResult {
    private HealthStatus status;
    private String message;
    private Map<String, Object> details;
    private LocalDateTime timestamp;
    
    public static HealthIndicatorResult healthy(String message) {
        return HealthIndicatorResult.builder()
                .status(HealthStatus.HEALTHY)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static HealthIndicatorResult healthy(String message, Map<String, Object> details) {
        return HealthIndicatorResult.builder()
                .status(HealthStatus.HEALTHY)
                .message(message)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static HealthIndicatorResult warning(String message) {
        return HealthIndicatorResult.builder()
                .status(HealthStatus.WARNING)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static HealthIndicatorResult warning(String message, Map<String, Object> details) {
        return HealthIndicatorResult.builder()
                .status(HealthStatus.WARNING)
                .message(message)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static HealthIndicatorResult critical(String message) {
        return HealthIndicatorResult.builder()
                .status(HealthStatus.CRITICAL)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static HealthIndicatorResult critical(String message, Map<String, Object> details) {
        return HealthIndicatorResult.builder()
                .status(HealthStatus.CRITICAL)
                .message(message)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
