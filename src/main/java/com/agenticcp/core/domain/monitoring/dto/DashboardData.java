package com.agenticcp.core.domain.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 모니터링 대시보드 통합 데이터
 * 
 * @author AgenticCP Team
 * @since 2025-10-20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardData {
    
    /**
     * 전체 시스템 상태 요약
     */
    private HealthSummary healthSummary;
    
    /**
     * 서비스별 상태
     */
    private List<ServiceStatus> serviceStatuses;
    
    /**
     * 최근 알림 (상위 5개)
     */
    private List<AlertSummary> recentAlerts;
    
    /**
     * 주요 메트릭 요약
     */
    private MetricSummary metricSummary;
    
    /**
     * 유지보수 모드 상태
     */
    private MaintenanceStatus maintenanceStatus;
    
    /**
     * 로그 요약
     */
    private LogSummary logSummary;
    
    /**
     * 대시보드 생성 시간
     */
    private LocalDateTime generatedAt;
    
    /**
     * 테넌트 ID
     */
    private String tenantId;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HealthSummary {
        private String overallStatus; // HEALTHY, WARNING, CRITICAL, UNKNOWN, MAINTENANCE
        private int totalServices;
        private int healthyServices;
        private int warningServices;
        private int criticalServices;
        private int maintenanceServices;
        private LocalDateTime lastCheckTime;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceStatus {
        private String serviceName;
        private String status; // HEALTHY, WARNING, CRITICAL, UNKNOWN, MAINTENANCE
        private String message;
        private LocalDateTime lastCheckTime;
        private Map<String, Object> details;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlertSummary {
        private Long id;
        private String level; // INFO, WARNING, ERROR, CRITICAL
        private String message;
        private String source;
        private LocalDateTime timestamp;
        private boolean resolved;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricSummary {
        private double averageResponseTime;
        private double successRate;
        private long totalRequests;
        private long errorCount;
        private LocalDateTime lastUpdated;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MaintenanceStatus {
        private boolean isActive;
        private String message;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String initiatedBy;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LogSummary {
        private long totalLogs;
        private long successLogs;
        private long failureLogs;
        private long warningLogs;
        private long errorLogs;
        private LocalDateTime lastLogTime;
    }
}
