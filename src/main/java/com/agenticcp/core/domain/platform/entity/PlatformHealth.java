package com.agenticcp.core.domain.platform.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "platform_health")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformHealth extends BaseEntity {

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private HealthStatus status;

    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    @Column(name = "cpu_usage_percent")
    private Double cpuUsagePercent;

    @Column(name = "memory_usage_percent")
    private Double memoryUsagePercent;

    @Column(name = "disk_usage_percent")
    private Double diskUsagePercent;

    @Column(name = "error_count")
    private Long errorCount;

    @Column(name = "last_check_time")
    private LocalDateTime lastCheckTime;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON for additional health metrics

    public enum HealthStatus {
        HEALTHY,
        WARNING,
        CRITICAL,
        UNKNOWN
    }
}
