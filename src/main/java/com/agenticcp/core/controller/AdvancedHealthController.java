package com.agenticcp.core.controller;

import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.domain.monitoring.health.dto.*;
import com.agenticcp.core.domain.monitoring.health.service.AdvancedHealthCheckService;
import com.agenticcp.core.domain.platform.service.MaintenanceModeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * @since 2025-11-13
 */
@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Advanced Health", description = "고급 헬스체크 API")
public class AdvancedHealthController {
    
    private final AdvancedHealthCheckService advancedHealthCheckService;
    private final MaintenanceModeService maintenanceModeService;
    
    /**
     * 전체 헬스체크 API
     * 
     * 시스템의 모든 컴포넌트 상태를 확인하고 전체 상태를 반환합니다.
     * 캐싱을 통해 성능이 최적화되어 있습니다.
     * 
     * @return 전체 헬스체크 결과
     */
    @Operation(summary = "전체 헬스체크", description = "모든 컴포넌트의 상태를 종합하여 반환합니다")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/advanced")
    public ResponseEntity<ApiResponse<HealthStatusResponse>> getOverallHealth() {
        log.info("[AdvancedHealthController] getOverallHealth - requested");
        
        HealthStatusResponse response = advancedHealthCheckService.getOverallHealth();
        log.info("[AdvancedHealthController] getOverallHealth - completed with status: {}", response.getOverallStatus());
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 특정 컴포넌트 헬스체크 API
     * 
     * 지정된 컴포넌트의 상태만 확인합니다.
     * 
     * @param name 컴포넌트 이름 (database, system, application 등)
     * @return 컴포넌트 헬스체크 결과
     */
    @Operation(summary = "컴포넌트 헬스체크", description = "지정한 컴포넌트의 상태를 확인합니다")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/component/{name}")
    public ResponseEntity<ApiResponse<ComponentHealthStatus>> getComponentHealth(
            @Parameter(description = "컴포넌트 이름", required = true)
            @PathVariable String name) {
        log.info("[AdvancedHealthController] getComponentHealth - requested for: {}", name);
        
        ComponentHealthStatus response = advancedHealthCheckService.getComponentHealth(name);
        log.info("[AdvancedHealthController] getComponentHealth - completed for {} with status: {}", 
                name, response.getStatus());
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 헬스체크 요약 API
     * 
     * 전체 서비스의 상태 통계를 제공합니다.
     * 
     * @return 헬스체크 요약 정보
     */
    @Operation(summary = "헬스체크 요약", description = "전체 서비스 상태에 대한 요약 통계를 제공합니다")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<HealthCheckSummary>> getHealthSummary() {
        log.info("[AdvancedHealthController] getHealthSummary - requested");
        
        HealthCheckSummary summary = advancedHealthCheckService.getHealthSummary();
        log.info("[AdvancedHealthController] getHealthSummary - generated: {} total services", summary.getTotalServices());
        
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
    
    /**
     * 사용 가능한 컴포넌트 목록 API
     * 
     * 헬스체크 가능한 컴포넌트 목록을 반환합니다.
     * 
     * @return 컴포넌트 목록
     */
    @Operation(summary = "헬스체크 가능 컴포넌트 목록", description = "헬스체크 대상 컴포넌트 리스트를 반환합니다")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/components")
    public ResponseEntity<ApiResponse<Map<String, String>>> getAvailableComponents() {
        log.info("[AdvancedHealthController] getAvailableComponents - requested");
        
        Map<String, String> components = Map.of(
            "database", "Database connection health check",
            "system", "System resource health check", 
            "application", "Application memory and thread health check"
        );
        
        return ResponseEntity.ok(ApiResponse.success(components));
    }
    
    /**
     * 유지보수 모드 상태 조회 API
     * 
     * 현재 유지보수 모드 상태를 반환합니다.
     * 
     * @return 유지보수 모드 상태
     */
    @Operation(summary = "유지보수 모드 상태 조회", description = "현재 유지보수 모드 활성화 여부를 반환합니다")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/maintenance-mode")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMaintenanceModeStatus() {
        log.info("[AdvancedHealthController] getMaintenanceModeStatus - requested");
        
        boolean isEnabled = maintenanceModeService.isMaintenanceModeEnabled();
        
        Map<String, Object> status = Map.of(
            "enabled", isEnabled,
            "status", isEnabled ? "WARNING" : "HEALTHY",
            "timestamp", System.currentTimeMillis()
        );
        
        return ResponseEntity.ok(ApiResponse.success(status));
    }
    
    /**
     * 유지보수 모드 활성화 API
     * 
     * 유지보수 모드를 활성화합니다.
     * 
     * @param reason 유지보수 모드 활성화 사유
     * @return 활성화 결과
     */
    @Operation(summary = "유지보수 모드 활성화", description = "유지보수 모드를 활성화합니다")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "활성화 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/maintenance-mode/enable")
    public ResponseEntity<ApiResponse<Map<String, Object>>> enableMaintenanceMode(
            @Parameter(description = "유지보수 모드 활성화 사유")
            @RequestParam(defaultValue = "Manual activation") String reason) {
        log.info("[AdvancedHealthController] enableMaintenanceMode - requested - reason: {}", reason);
        
        maintenanceModeService.enable(reason);
        
        Map<String, Object> result = Map.of(
            "enabled", true,
            "reason", reason,
            "timestamp", System.currentTimeMillis()
        );
        
        return ResponseEntity.ok(ApiResponse.success(result));
    }
    
    /**
     * 유지보수 모드 비활성화 API
     * 
     * 유지보수 모드를 비활성화합니다.
     * 
     * @return 비활성화 결과
     */
    @Operation(summary = "유지보수 모드 비활성화", description = "유지보수 모드를 비활성화합니다")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "비활성화 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/maintenance-mode/disable")
    public ResponseEntity<ApiResponse<Map<String, Object>>> disableMaintenanceMode() {
        log.info("[AdvancedHealthController] disableMaintenanceMode - requested");
        
        maintenanceModeService.disable();
        
        Map<String, Object> result = Map.of(
            "enabled", false,
            "reason", "Maintenance completed",
            "timestamp", System.currentTimeMillis()
        );
        
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
