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
public class HealthStatusResponse {
    private HealthStatus overallStatus;
    private LocalDateTime timestamp;
    private Map<String, HealthIndicatorResult> components;
    private Long responseTime;
    private String message;
}
