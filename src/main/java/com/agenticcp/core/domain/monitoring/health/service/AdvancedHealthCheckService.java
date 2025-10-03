package com.agenticcp.core.domain.monitoring.health.service;

import com.agenticcp.core.domain.monitoring.health.dto.*;
import com.agenticcp.core.domain.monitoring.health.indicator.HealthIndicator;
import com.agenticcp.core.domain.platform.entity.PlatformHealth;
import com.agenticcp.core.domain.platform.repository.PlatformHealthRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

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
 * @since 2024-01-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdvancedHealthCheckService {
    
    private final List<HealthIndicator> healthIndicators;
    private final PlatformHealthRepository platformHealthRepository;
    
    @Cacheable(value = "healthCheck", key = "'overall'")
    public HealthStatusResponse getOverallHealth() {
        log.info("Performing overall health check");
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
                log.error("Error checking health for indicator: {}", indicator.getName(), e);
                HealthIndicatorResult errorResult = HealthIndicatorResult.critical(
                    "Health check failed: " + e.getMessage()
                );
                components.put(indicator.getName(), errorResult);
                overallStatus = PlatformHealth.HealthStatus.CRITICAL;
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
        
        log.info("Overall health check completed in {}ms with status: {}", responseTime, overallStatus);
        return response;
    }
    
    @Cacheable(value = "healthCheck", key = "#componentName")
    public ComponentHealthStatus getComponentHealth(String componentName) {
        log.info("Performing health check for component: {}", componentName);
        long startTime = System.currentTimeMillis();
        
        HealthIndicator indicator = healthIndicators.stream()
                .filter(i -> i.getName().equals(componentName))
                .findFirst()
                .orElse(null);
        
        if (indicator == null) {
            log.warn("Component not found: {}", componentName);
            return ComponentHealthStatus.builder()
                    .component(componentName)
                    .status(PlatformHealth.HealthStatus.UNKNOWN)
                    .message("Component not found")
                    .timestamp(LocalDateTime.now())
                    .responseTime(System.currentTimeMillis() - startTime)
                    .build();
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
            
            log.info("Component health check completed for {} in {}ms with status: {}", 
                    componentName, responseTime, result.getStatus());
            return response;
            
        } catch (Exception e) {
            log.error("Error checking health for component: {}", componentName, e);
            return ComponentHealthStatus.builder()
                    .component(componentName)
                    .status(PlatformHealth.HealthStatus.CRITICAL)
                    .message("Health check failed: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .responseTime(System.currentTimeMillis() - startTime)
                    .build();
        }
    }
    
    public HealthCheckSummary getHealthSummary() {
        log.info("Generating health check summary");
        
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
        
        log.info("Health check summary generated: {} total, {} healthy, {} warning, {} critical, {} unknown",
                totalServices, healthyServices, warningServices, criticalServices, unknownServices);
        
        return summary;
    }
    
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
            log.debug("Saved health status for service: {} with status: {}", serviceName, status);
            
        } catch (Exception e) {
            log.error("Error saving health status for service: {}", serviceName, e);
        }
    }
}
