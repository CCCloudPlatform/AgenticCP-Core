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
public class ComponentHealthStatus {
    private String component;
    private HealthStatus status;
    private String message;
    private Map<String, Object> details;
    private LocalDateTime timestamp;
    private Long responseTime;
}
