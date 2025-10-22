package com.agenticcp.core.domain.monitoring.service;

import com.agenticcp.core.domain.monitoring.dto.DashboardData;
import com.agenticcp.core.domain.monitoring.dto.LogEntryResponse;
import com.agenticcp.core.domain.monitoring.entity.Alert;
import com.agenticcp.core.domain.monitoring.entity.Metric;
import com.agenticcp.core.domain.monitoring.repository.AlertRepository;
import com.agenticcp.core.domain.monitoring.repository.MetricRepository;
import com.agenticcp.core.domain.platform.entity.PlatformHealth;
import com.agenticcp.core.domain.platform.repository.PlatformHealthRepository;
import com.agenticcp.core.domain.platform.service.MaintenanceModeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 모니터링 대시보드 서비스
 * 
 * @author AgenticCP Team
 * @since 2025-10-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonitoringDashboardService {
    
    private final HealthCheckService healthCheckService;
    private final AlertRepository alertRepository;
    private final MetricRepository metricRepository;
    private final PlatformHealthRepository platformHealthRepository;
    
    /**
     * 통합 대시보드 데이터 조회
     */
    @Cacheable(value = "dashboard", key = "#tenantId", unless = "#result == null")
    public DashboardData getDashboardData(String tenantId) {
        log.debug("대시보드 데이터 조회 시작 - tenantId: {}", tenantId);
        
        try {
            // 1. 헬스 체크 데이터 수집
            DashboardData.HealthSummary healthSummary = buildHealthSummary(tenantId);
            List<DashboardData.ServiceStatus> serviceStatuses = buildServiceStatuses(tenantId);
            
            // 2. 최근 알림 조회
            List<DashboardData.AlertSummary> recentAlerts = getRecentAlerts(tenantId, 5);
            
            // 3. 메트릭 요약
            DashboardData.MetricSummary metricSummary = buildMetricSummary(tenantId);
            
            // 4. 유지보수 모드 상태
            DashboardData.MaintenanceStatus maintenanceStatus = buildMaintenanceStatus();
            
            // 5. 로그 요약
            DashboardData.LogSummary logSummary = buildLogSummary(tenantId);
            
            return DashboardData.builder()
                    .healthSummary(healthSummary)
                    .serviceStatuses(serviceStatuses)
                    .recentAlerts(recentAlerts)
                    .metricSummary(metricSummary)
                    .maintenanceStatus(maintenanceStatus)
                    .logSummary(logSummary)
                    .generatedAt(LocalDateTime.now())
                    .tenantId(tenantId)
                    .build();
                    
        } catch (Exception e) {
            log.error("대시보드 데이터 조회 중 오류 발생 - tenantId: {}", tenantId, e);
            throw new RuntimeException("대시보드 데이터 조회 실패", e);
        }
    }
    
    /**
     * 알림 목록 조회
     */
    public List<DashboardData.AlertSummary> getAlerts(String tenantId, int page, int size, 
                                                      String level, String source, 
                                                      LocalDateTime startTime, LocalDateTime endTime) {
        log.debug("알림 목록 조회 - tenantId: {}, page: {}, size: {}", tenantId, page, size);
        
        List<Alert> alerts = alertRepository.findByTenantId(tenantId);
        // 페이지네이션을 수동으로 처리
        int start = page * size;
        int end = Math.min(start + size, alerts.size());
        List<Alert> pagedAlerts = alerts.subList(start, end);
        
        return pagedAlerts.stream()
                .map(this::convertToAlertSummary)
                .collect(Collectors.toList());
    }
    
    /**
     * 메트릭 데이터 조회
     */
    public List<Metric> getMetrics(String tenantId, int page, int size,
                                  LocalDateTime startTime, LocalDateTime endTime) {
        log.debug("메트릭 데이터 조회 - tenantId: {}, page: {}, size: {}", tenantId, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        return metricRepository.findByTenantId(tenantId, pageable).getContent();
    }
    
    /**
     * 로그 엔트리 조회
     */
    public List<LogEntryResponse> getLogs(String tenantId, int page, int size,
                                         String level, String type, String source,
                                         LocalDateTime startTime, LocalDateTime endTime) {
        log.debug("로그 엔트리 조회 - tenantId: {}, page: {}, size: {}", tenantId, page, size);
        
        List<LogEntryResponse> logs = new ArrayList<>();
        
        // 1. Alert 테이블에서 로그 조회 (알림 기반)
        List<Alert> alerts = alertRepository.findByTenantId(tenantId);
        // 페이지네이션을 수동으로 처리
        int start = page * size;
        int end = Math.min(start + size, alerts.size());
        List<Alert> pagedAlerts = alerts.subList(start, end);
        
        logs.addAll(pagedAlerts.stream()
                .map(this::convertAlertToLogEntry)
                .collect(Collectors.toList()));
        
        // 2. Metric 테이블에서 로그 조회 (성공/실패 메트릭)
        Pageable pageable = PageRequest.of(page, size);
        Page<Metric> metricPage = metricRepository.findByTenantId(tenantId, pageable);
        
        logs.addAll(metricPage.getContent().stream()
                .map(this::convertMetricToLogEntry)
                .collect(Collectors.toList()));
        
        // 시간순 정렬 및 페이지네이션
        return logs.stream()
                .sorted(Comparator.comparing(LogEntryResponse::getTimestamp).reversed())
                .skip(page * size)
                .limit(size)
                .collect(Collectors.toList());
    }
    
    // === Private Helper Methods ===
    
    private DashboardData.HealthSummary buildHealthSummary(String tenantId) {
        // 실시간 헬스 체크 수행
        healthCheckService.checkAllSystemComponents();
        
        // PlatformHealth에서 전체 상태 조회
        List<PlatformHealth> healthData = platformHealthRepository.findAll();
        
        int totalServices = healthData.size();
        int healthyServices = (int) healthData.stream()
                .filter(h -> h.getStatus() != null && "HEALTHY".equals(h.getStatus().toString()))
                .count();
        int warningServices = (int) healthData.stream()
                .filter(h -> h.getStatus() != null && "WARNING".equals(h.getStatus().toString()))
                .count();
        int criticalServices = (int) healthData.stream()
                .filter(h -> h.getStatus() != null && "CRITICAL".equals(h.getStatus().toString()))
                .count();
        int maintenanceServices = (int) healthData.stream()
                .filter(h -> h.getStatus() != null && "MAINTENANCE".equals(h.getStatus().toString()))
                .count();
        
        String overallStatus = determineOverallStatus(healthyServices, warningServices, criticalServices, maintenanceServices);
        
        return DashboardData.HealthSummary.builder()
                .overallStatus(overallStatus)
                .totalServices(totalServices)
                .healthyServices(healthyServices)
                .warningServices(warningServices)
                .criticalServices(criticalServices)
                .maintenanceServices(maintenanceServices)
                .lastCheckTime(LocalDateTime.now())
                .build();
    }
    
    private List<DashboardData.ServiceStatus> buildServiceStatuses(String tenantId) {
        List<PlatformHealth> healthData = platformHealthRepository.findAll();
        
        return healthData.stream()
                .map(h -> DashboardData.ServiceStatus.builder()
                        .serviceName(h.getServiceName())
                        .status(h.getStatus() != null ? h.getStatus().toString() : "UNKNOWN")
                        .message(h.getErrorMessage())
                        .lastCheckTime(h.getLastCheckTime())
                        .details(Map.of("metadata", h.getMetadata() != null ? h.getMetadata() : ""))
                        .build())
                .collect(Collectors.toList());
    }
    
    private List<DashboardData.AlertSummary> getRecentAlerts(String tenantId, int limit) {
        List<Alert> alerts = alertRepository.findByTenantId(tenantId);
        // 최근 limit개만 가져오기
        int end = Math.min(limit, alerts.size());
        List<Alert> recentAlerts = alerts.subList(0, end);
        
        return recentAlerts.stream()
                .map(this::convertToAlertSummary)
                .collect(Collectors.toList());
    }
    
    private DashboardData.MetricSummary buildMetricSummary(String tenantId) {
        // 최근 1시간 메트릭 데이터로 요약 생성
        LocalDateTime endTime = LocalDateTime.now();
        
        List<Metric> metrics = metricRepository.findByTenantId(tenantId, PageRequest.of(0, 1000)).getContent();
        
        if (metrics.isEmpty()) {
            return DashboardData.MetricSummary.builder()
                    .averageResponseTime(0.0)
                    .successRate(0.0)
                    .totalRequests(0L)
                    .errorCount(0L)
                    .lastUpdated(LocalDateTime.now())
                    .build();
        }
        
        // Metric 엔티티에 responseTime 필드가 없으므로 임시로 0.0 반환
        double averageResponseTime = 0.0;
        
        long totalRequests = metrics.size();
        long errorCount = metrics.stream()
                .filter(m -> m.getStatus() != null && ("ERROR".equals(m.getStatus().toString()) || "FAILURE".equals(m.getStatus().toString())))
                .count();
        
        double successRate = totalRequests > 0 ? ((double) (totalRequests - errorCount) / totalRequests) * 100 : 0.0;
        
        return DashboardData.MetricSummary.builder()
                .averageResponseTime(averageResponseTime)
                .successRate(successRate)
                .totalRequests(totalRequests)
                .errorCount(errorCount)
                .lastUpdated(LocalDateTime.now())
                .build();
    }
    
    private DashboardData.MaintenanceStatus buildMaintenanceStatus() {
        // MaintenanceModeService의 실제 메서드명 확인 필요
        boolean isActive = false; // 임시값
        
        if (!isActive) {
            return DashboardData.MaintenanceStatus.builder()
                    .isActive(false)
                    .build();
        }
        
        // 유지보수 모드가 활성화된 경우 상세 정보 조회
        return DashboardData.MaintenanceStatus.builder()
                .isActive(true)
                .message("시스템 유지보수 중입니다")
                .startTime(LocalDateTime.now().minusHours(1)) // 실제로는 DB에서 조회
                .endTime(LocalDateTime.now().plusHours(2))    // 실제로는 DB에서 조회
                .initiatedBy("admin")                         // 실제로는 DB에서 조회
                .build();
    }
    
    private DashboardData.LogSummary buildLogSummary(String tenantId) {
        // 최근 24시간 로그 통계
        LocalDateTime endTime = LocalDateTime.now();
        
        List<Alert> alerts = alertRepository.findByTenantId(tenantId);
        
        long totalLogs = alerts.size();
        long successLogs = alerts.stream()
                .filter(a -> a.getSeverity() != null && "INFO".equals(a.getSeverity().toString()))
                .count();
        long failureLogs = alerts.stream()
                .filter(a -> a.getSeverity() != null && ("ERROR".equals(a.getSeverity().toString()) || "CRITICAL".equals(a.getSeverity().toString())))
                .count();
        long warningLogs = alerts.stream()
                .filter(a -> a.getSeverity() != null && "WARNING".equals(a.getSeverity().toString()))
                .count();
        long errorLogs = alerts.stream()
                .filter(a -> a.getSeverity() != null && "ERROR".equals(a.getSeverity().toString()))
                .count();
        
        LocalDateTime lastLogTime = alerts.stream()
                .map(Alert::getCreatedAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        
        return DashboardData.LogSummary.builder()
                .totalLogs(totalLogs)
                .successLogs(successLogs)
                .failureLogs(failureLogs)
                .warningLogs(warningLogs)
                .errorLogs(errorLogs)
                .lastLogTime(lastLogTime)
                .build();
    }
    
    private DashboardData.AlertSummary convertToAlertSummary(Alert alert) {
        return DashboardData.AlertSummary.builder()
                .id(alert.getId())
                .level(alert.getSeverity() != null ? alert.getSeverity().toString() : "INFO")
                .message(alert.getDescription())
                .source(alert.getAlertType() != null ? alert.getAlertType().toString() : "unknown")
                .timestamp(alert.getCreatedAt())
                .resolved(alert.getStatus() != null && alert.getStatus().toString().equals("RESOLVED"))
                .build();
    }
    
    private LogEntryResponse convertAlertToLogEntry(Alert alert) {
        return LogEntryResponse.builder()
                .id(alert.getId())
                .level(alert.getSeverity() != null ? alert.getSeverity().toString() : "INFO")
                .type("alert")
                .source(alert.getAlertType() != null ? alert.getAlertType().toString() : "unknown")
                .message(alert.getDescription())
                .service("monitoring")
                .component("alert")
                .timestamp(alert.getCreatedAt())
                .metadata(Map.of("alertName", alert.getAlertName()))
                .tenantId(alert.getTenantId())
                .build();
    }
    
    private LogEntryResponse convertMetricToLogEntry(Metric metric) {
        String level = "SUCCESS".equals(metric.getStatus().toString()) ? "INFO" : "ERROR";
        String type = "SUCCESS".equals(metric.getStatus().toString()) ? "success" : "failure";
        
        return LogEntryResponse.builder()
                .id(metric.getId())
                .level(level)
                .type(type)
                .source("monitoring")
                .message(String.format("메트릭 수집 %s: %s", 
                        "SUCCESS".equals(metric.getStatus().toString()) ? "성공" : "실패", 
                        metric.getMetricName()))
                .service("monitoring")
                .component("metric")
                .timestamp(metric.getCollectedAt())
                .metadata(Map.of(
                        "metricName", metric.getMetricName(),
                        "value", metric.getMetricValue() != null ? metric.getMetricValue() : 0,
                        "responseTime", 0
                ))
                .tenantId(metric.getTenantId())
                .build();
    }
    
    
    private String determineOverallStatus(int healthy, int warning, int critical, int maintenance) {
        if (maintenance > 0) {
            return "MAINTENANCE";
        } else if (critical > 0) {
            return "CRITICAL";
        } else if (warning > 0) {
            return "WARNING";
        } else if (healthy > 0) {
            return "HEALTHY";
        } else {
            return "UNKNOWN";
        }
    }
}