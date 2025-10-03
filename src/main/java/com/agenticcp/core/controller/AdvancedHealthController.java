package com.agenticcp.core.controller;

import com.agenticcp.core.common.dto.ApiResponse;
import com.agenticcp.core.domain.monitoring.enums.MonitoringErrorCode;
import com.agenticcp.core.domain.monitoring.health.dto.*;
import com.agenticcp.core.domain.monitoring.health.service.AdvancedHealthCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 고급 헬스체크 컨트롤러
 * 
 * 시스템의 전반적인 상태와 개별 컴포넌트의 상태를 확인하는 API를 제공합니다.
 * 캐싱을 통해 성능을 최적화하고, 상세한 헬스체크 정보를 제공합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Slf4j
public class AdvancedHealthController {
    
    private final AdvancedHealthCheckService advancedHealthCheckService;
    
    /**
     * 전체 헬스체크 API
     * 
     * 시스템의 모든 컴포넌트 상태를 확인하고 전체 상태를 반환합니다.
     * 캐싱을 통해 성능이 최적화되어 있습니다.
     * 
     * @return 전체 헬스체크 결과
     */
    @GetMapping("/advanced")
    public ResponseEntity<ApiResponse<HealthStatusResponse>> getOverallHealth() {
        log.info("Advanced health check requested");
        
        try {
            HealthStatusResponse response = advancedHealthCheckService.getOverallHealth();
            log.info("Advanced health check completed with status: {}", response.getOverallStatus());
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Error performing advanced health check", e);
            return ResponseEntity.status(MonitoringErrorCode.HEALTH_CHECK_FAILED.getHttpStatus())
                    .body(ApiResponse.error(MonitoringErrorCode.HEALTH_CHECK_FAILED, e.getMessage()));
        }
    }
    
    /**
     * 특정 컴포넌트 헬스체크 API
     * 
     * 지정된 컴포넌트의 상태만 확인합니다.
     * 
     * @param name 컴포넌트 이름 (database, system, application 등)
     * @return 컴포넌트 헬스체크 결과
     */
    @GetMapping("/component/{name}")
    public ResponseEntity<ApiResponse<ComponentHealthStatus>> getComponentHealth(@PathVariable String name) {
        log.info("Component health check requested for: {}", name);
        
        try {
            ComponentHealthStatus response = advancedHealthCheckService.getComponentHealth(name);
            log.info("Component health check completed for {} with status: {}", 
                    name, response.getStatus());
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Error performing component health check for: {}", name, e);
            return ResponseEntity.status(MonitoringErrorCode.COMPONENT_HEALTH_CHECK_FAILED.getHttpStatus())
                    .body(ApiResponse.error(MonitoringErrorCode.COMPONENT_HEALTH_CHECK_FAILED, e.getMessage()));
        }
    }
    
    /**
     * 헬스체크 요약 API
     * 
     * 전체 서비스의 상태 통계를 제공합니다.
     * 
     * @return 헬스체크 요약 정보
     */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<HealthCheckSummary>> getHealthSummary() {
        log.info("Health check summary requested");
        
        try {
            HealthCheckSummary summary = advancedHealthCheckService.getHealthSummary();
            log.info("Health check summary generated: {} total services", summary.getTotalServices());
            
            return ResponseEntity.ok(ApiResponse.success(summary));
        } catch (Exception e) {
            log.error("Error generating health check summary", e);
            return ResponseEntity.status(MonitoringErrorCode.HEALTH_SUMMARY_FAILED.getHttpStatus())
                    .body(ApiResponse.error(MonitoringErrorCode.HEALTH_SUMMARY_FAILED, e.getMessage()));
        }
    }
    
    /**
     * 사용 가능한 컴포넌트 목록 API
     * 
     * 헬스체크 가능한 컴포넌트 목록을 반환합니다.
     * 
     * @return 컴포넌트 목록
     */
    @GetMapping("/components")
    public ResponseEntity<ApiResponse<Map<String, String>>> getAvailableComponents() {
        log.info("Available components requested");
        
        try {
            Map<String, String> components = Map.of(
                "database", "Database connection health check",
                "system", "System resource health check", 
                "application", "Application memory and thread health check"
            );
            
            return ResponseEntity.ok(ApiResponse.success(components));
        } catch (Exception e) {
            log.error("Error getting available components", e);
            return ResponseEntity.status(MonitoringErrorCode.HEALTH_CHECK_FAILED.getHttpStatus())
                    .body(ApiResponse.error(MonitoringErrorCode.HEALTH_CHECK_FAILED, e.getMessage()));
        }
    }
}
