package com.agenticcp.core.domain.monitoring.health.service;

import com.agenticcp.core.domain.monitoring.health.dto.*;
import com.agenticcp.core.domain.monitoring.health.exception.ComponentNotFoundException;
import com.agenticcp.core.domain.monitoring.health.exception.HealthCheckException;
import com.agenticcp.core.domain.monitoring.health.indicator.HealthIndicator;
import com.agenticcp.core.domain.monitoring.enums.MonitoringErrorCode;
import com.agenticcp.core.domain.platform.entity.PlatformHealth;
import com.agenticcp.core.domain.platform.repository.PlatformHealthRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 고급 헬스체크 서비스
 * 
 * 시스템의 전반적인 상태와 개별 컴포넌트의 상태를 확인하는 서비스입니다.
 * 캐싱을 통해 성능을 최적화하고, 기존 PlatformHealth 엔티티와 연동됩니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdvancedHealthCheckService {
    
    private final List<HealthIndicator> healthIndicators;
    private final PlatformHealthRepository platformHealthRepository;
    
    /**
     * 전체 헬스체크 수행
     * 
     * <p>모든 헬스 인디케이터를 순회하며 각 컴포넌트의 상태를 확인하고,
     * 전체 시스템의 상태를 종합하여 반환합니다.</p>
     * 
     * @return 헬스 상태 응답
     * @throws HealthCheckException 헬스체크 수행 중 오류 발생 시
     */
    public HealthStatusResponse getOverallHealth() {
        log.info("[AdvancedHealthCheckService] getOverallHealth - performing overall health check");
        long startTime = System.currentTimeMillis();
        
        Map<String, HealthIndicatorResult> components = new HashMap<>();
        PlatformHealth.HealthStatus overallStatus = PlatformHealth.HealthStatus.HEALTHY;
        
        for (HealthIndicator indicator : healthIndicators) {
            try {
                HealthIndicatorResult result = indicator.check();
                components.put(indicator.getName(), result);
                
                // 전체 상태 결정 (CRITICAL > WARNING > HEALTHY)
                if (result.getStatus() == PlatformHealth.HealthStatus.CRITICAL) {
                    overallStatus = PlatformHealth.HealthStatus.CRITICAL;
                } else if (result.getStatus() == PlatformHealth.HealthStatus.WARNING && 
                          overallStatus != PlatformHealth.HealthStatus.CRITICAL) {
                    overallStatus = PlatformHealth.HealthStatus.WARNING;
                }
                
                // PlatformHealth에 저장
                saveHealthStatus(indicator.getName(), result);
                
            } catch (Exception e) {
                log.error("[AdvancedHealthCheckService] getOverallHealth - error checking health for indicator: {}", indicator.getName(), e);
                throw new HealthCheckException(MonitoringErrorCode.HEALTH_INDICATOR_ERROR, 
                    "Health indicator error for " + indicator.getName() + ": " + e.getMessage());
            }
        }
        
        long responseTime = System.currentTimeMillis() - startTime;
        
        HealthStatusResponse response = HealthStatusResponse.builder()
                .overallStatus(overallStatus)
                .timestamp(LocalDateTime.now())
                .components(components)
                .responseTime(responseTime)
                .message("Health check completed")
                .build();
        
        log.info("[AdvancedHealthCheckService] getOverallHealth - completed in {}ms with status: {}", responseTime, overallStatus);
        return response;
    }
    
    /**
     * 특정 컴포넌트 헬스체크 수행
     * 
     * <p>지정된 컴포넌트의 헬스 상태를 확인하고 반환합니다.</p>
     * 
     * @param componentName 컴포넌트 이름
     * @return 컴포넌트 헬스 상태
     * @throws ComponentNotFoundException 컴포넌트를 찾을 수 없을 때
     * @throws HealthCheckException 헬스체크 수행 중 오류 발생 시
     */
    public ComponentHealthStatus getComponentHealth(String componentName) {
        log.info("[AdvancedHealthCheckService] getComponentHealth - performing health check for component: {}", componentName);
        long startTime = System.currentTimeMillis();
        
        HealthIndicator indicator = healthIndicators.stream()
                .filter(i -> i.getName().equals(componentName))
                .findFirst()
                .orElse(null);
        
        if (indicator == null) {
            log.warn("[AdvancedHealthCheckService] getComponentHealth - component not found: {}", componentName);
            throw new ComponentNotFoundException(componentName);
        }
        
        try {
            HealthIndicatorResult result = indicator.check();
            long responseTime = System.currentTimeMillis() - startTime;
            
            // PlatformHealth에 저장
            saveHealthStatus(componentName, result);
            
            ComponentHealthStatus response = ComponentHealthStatus.builder()
                    .component(componentName)
                    .status(result.getStatus())
                    .message(result.getMessage())
                    .details(result.getDetails())
                    .timestamp(LocalDateTime.now())
                    .responseTime(responseTime)
                    .build();
            
            log.info("[AdvancedHealthCheckService] getComponentHealth - completed for {} in {}ms with status: {}", 
                    componentName, responseTime, result.getStatus());
            return response;
            
        } catch (Exception e) {
            log.error("[AdvancedHealthCheckService] getComponentHealth - error checking health for component: {}", componentName, e);
            throw new HealthCheckException(MonitoringErrorCode.HEALTH_INDICATOR_ERROR, 
                "Health indicator error for " + componentName + ": " + e.getMessage());
        }
    }
    
    /**
     * 헬스체크 요약 조회
     * 
     * <p>데이터베이스에 저장된 최신 헬스 상태를 기반으로
     * 전체 서비스의 상태 통계를 계산하여 반환합니다.</p>
     * 
     * @return 헬스체크 요약 정보
     */
    public HealthCheckSummary getHealthSummary() {
        log.info("[AdvancedHealthCheckService] getHealthSummary - generating health check summary");
        
        List<PlatformHealth> latestHealthStatus = platformHealthRepository.findLatestHealthStatus();
        
        long totalServices = latestHealthStatus.size();
        long healthyServices = latestHealthStatus.stream()
                .mapToLong(ph -> ph.getStatus() == PlatformHealth.HealthStatus.HEALTHY ? 1 : 0)
                .sum();
        long warningServices = latestHealthStatus.stream()
                .mapToLong(ph -> ph.getStatus() == PlatformHealth.HealthStatus.WARNING ? 1 : 0)
                .sum();
        long criticalServices = latestHealthStatus.stream()
                .mapToLong(ph -> ph.getStatus() == PlatformHealth.HealthStatus.CRITICAL ? 1 : 0)
                .sum();
        long unknownServices = latestHealthStatus.stream()
                .mapToLong(ph -> ph.getStatus() == PlatformHealth.HealthStatus.UNKNOWN ? 1 : 0)
                .sum();
        
        HealthCheckSummary summary = HealthCheckSummary.builder()
                .totalServices(totalServices)
                .healthyServices(healthyServices)
                .warningServices(warningServices)
                .criticalServices(criticalServices)
                .unknownServices(unknownServices)
                .lastUpdated(LocalDateTime.now())
                .build();
        
        log.info("[AdvancedHealthCheckService] getHealthSummary - generated: {} total, {} healthy, {} warning, {} critical, {} unknown",
                totalServices, healthyServices, warningServices, criticalServices, unknownServices);
        
        return summary;
    }
    
    /**
     * 헬스 상태 저장
     * 
     * <p>헬스 인디케이터 결과를 PlatformHealth 엔티티에 저장합니다.</p>
     * 
     * @param serviceName 서비스 이름
     * @param result 헬스 인디케이터 결과
     */
    @Transactional
    private void saveHealthStatus(String serviceName, HealthIndicatorResult result) {
        try {
            PlatformHealth existingHealth = platformHealthRepository.findByServiceName(serviceName)
                    .orElse(null);
            
            PlatformHealth.HealthStatus status = result.getStatus();
            Long responseTime = result.getDetails() != null ? 
                    (Long) result.getDetails().get("responseTime") : null;
            
            Double cpuUsage = result.getDetails() != null ? 
                    (Double) result.getDetails().get("cpuUsage") : null;
            Double memoryUsage = result.getDetails() != null ? 
                    (Double) result.getDetails().get("memoryUsage") : null;
            Double diskUsage = result.getDetails() != null ? 
                    (Double) result.getDetails().get("diskUsage") : null;
            
            PlatformHealth platformHealth;
            if (existingHealth != null) {
                platformHealth = PlatformHealth.builder()
                        .serviceName(serviceName)
                        .status(status)
                        .responseTimeMs(responseTime)
                        .cpuUsagePercent(cpuUsage)
                        .memoryUsagePercent(memoryUsage)
                        .diskUsagePercent(diskUsage)
                        .errorCount(status == PlatformHealth.HealthStatus.CRITICAL ? 
                                (existingHealth.getErrorCount() != null ? existingHealth.getErrorCount() + 1L : 1L) : 0L)
                        .lastCheckTime(LocalDateTime.now())
                        .errorMessage(status == PlatformHealth.HealthStatus.CRITICAL ? result.getMessage() : null)
                        .metadata(result.getDetails() != null ? result.getDetails().toString() : null)
                        .build();
                platformHealth.setId(existingHealth.getId());
            } else {
                platformHealth = PlatformHealth.builder()
                        .serviceName(serviceName)
                        .status(status)
                        .responseTimeMs(responseTime)
                        .cpuUsagePercent(cpuUsage)
                        .memoryUsagePercent(memoryUsage)
                        .diskUsagePercent(diskUsage)
                        .errorCount(status == PlatformHealth.HealthStatus.CRITICAL ? 1L : 0L)
                        .lastCheckTime(LocalDateTime.now())
                        .errorMessage(status == PlatformHealth.HealthStatus.CRITICAL ? result.getMessage() : null)
                        .metadata(result.getDetails() != null ? result.getDetails().toString() : null)
                        .build();
            }
            
            platformHealthRepository.save(platformHealth);
            log.debug("[AdvancedHealthCheckService] saveHealthStatus - saved health status for service: {} with status: {}", serviceName, status);
            
        } catch (Exception e) {
            log.error("[AdvancedHealthCheckService] saveHealthStatus - error saving health status for service: {}", serviceName, e);
        }
    }
}
