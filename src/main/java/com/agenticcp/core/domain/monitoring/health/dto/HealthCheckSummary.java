package com.agenticcp.core.domain.monitoring.health.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthCheckSummary {
    private Long totalServices;
    private Long healthyServices;
    private Long warningServices;
    private Long criticalServices;
    private Long unknownServices;
    private LocalDateTime lastUpdated;
}
