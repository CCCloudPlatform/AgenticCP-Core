package com.agenticcp.core.domain.monitoring.controller;

import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.domain.monitoring.dto.DashboardData;
import com.agenticcp.core.domain.monitoring.dto.LogEntryResponse;
import com.agenticcp.core.domain.monitoring.entity.Metric;
import com.agenticcp.core.domain.monitoring.service.MonitoringDashboardService;
import com.agenticcp.core.domain.monitoring.service.MetricTrendService;
import com.agenticcp.core.common.context.TenantContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 모니터링 대시보드 컨트롤러
 * 
 * @author AgenticCP Team
 * @since 2025-10-20
 */
@Slf4j
@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
@Tag(name = "Monitoring Dashboard", description = "모니터링 대시보드 API")
public class MonitoringDashboardController {
    
    private final MonitoringDashboardService dashboardService;
    private final MetricTrendService metricTrendService;
    
    /**
     * 통합 대시보드 데이터 조회
     */
    @GetMapping("/dashboard")
    @Operation(summary = "대시보드 데이터 조회", description = "시스템 상태, 알림, 메트릭, 로그 요약 정보를 통합 조회")
    public ResponseEntity<ApiResponse<DashboardData>> getDashboardData(
            HttpServletRequest request,
            Authentication authentication) {
        
        log.info("대시보드 데이터 조회 요청");
        
        try {
            String tenantId = getTenantIdFromRequest(request, authentication);
            DashboardData dashboardData = dashboardService.getDashboardData(tenantId);
            
            return ResponseEntity.ok(ApiResponse.success(dashboardData));
            
        } catch (Exception e) {
            log.error("대시보드 데이터 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<DashboardData>builder()
                            .success(false)
                            .errorCode("DASHBOARD_001")
                            .message("대시보드 데이터 조회 실패: " + e.getMessage())
                            .build());
        }
    }
    
    /**
     * 알림 목록 조회
     */
    @GetMapping("/alerts")
    @Operation(summary = "알림 목록 조회", description = "시스템 알림 목록을 페이지네이션으로 조회")
    public ResponseEntity<ApiResponse<List<DashboardData.AlertSummary>>> getAlerts(
            HttpServletRequest request,
            Authentication authentication,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "알림 레벨 필터") @RequestParam(required = false) String level,
            @Parameter(description = "알림 소스 필터") @RequestParam(required = false) String source,
            @Parameter(description = "시작 시간") @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "종료 시간") @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        log.info("알림 목록 조회 요청 - page: {}, size: {}, level: {}, source: {}", page, size, level, source);
        
        try {
            String tenantId = getTenantIdFromRequest(request, authentication);
            List<DashboardData.AlertSummary> alerts = dashboardService.getAlerts(
                    tenantId, page, size, level, source, startTime, endTime);
            
            return ResponseEntity.ok(ApiResponse.success(alerts));
            
        } catch (Exception e) {
            log.error("알림 목록 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<List<DashboardData.AlertSummary>>builder()
                            .success(false)
                            .errorCode("ALERT_001")
                            .message("알림 목록 조회 실패: " + e.getMessage())
                            .build());
        }
    }
    
    /**
     * 메트릭 데이터 조회
     */
    @GetMapping("/dashboard/metrics")
    @Operation(summary = "메트릭 데이터 조회", description = "시스템 성능 메트릭 데이터를 조회 (트렌드 분석 지원)")
    public ResponseEntity<ApiResponse<List<Metric>>> getMetrics(
            HttpServletRequest request,
            Authentication authentication,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "시작 시간") @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "종료 시간") @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @Parameter(description = "시간 간격 (1m, 5m, 1h, 1d)") @RequestParam(required = false) String interval,
            @Parameter(description = "메트릭 이름") @RequestParam(required = false) String metricName) {
        
        log.info("메트릭 데이터 조회 요청 - page: {}, size: {}, interval: {}, metricName: {}", 
                page, size, interval, metricName);
        
        try {
            String tenantId = getTenantIdFromRequest(request, authentication);
            List<Metric> metrics;
            
            if (metricName != null && interval != null) {
                // 트렌드 분석 모드 (자동 판단)
                metrics = metricTrendService.getMetricsWithTrend(tenantId, metricName, startTime, endTime, interval);
            } else {
                // 일반 조회 모드
                metrics = dashboardService.getMetrics(tenantId, page, size, startTime, endTime);
            }
            
            return ResponseEntity.ok(ApiResponse.success(metrics));
            
        } catch (Exception e) {
            log.error("메트릭 데이터 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<List<Metric>>builder()
                            .success(false)
                            .errorCode("METRIC_001")
                            .message("메트릭 데이터 조회 실패: " + e.getMessage())
                            .build());
        }
    }
    
    /**
     * 로그 엔트리 조회
     */
    @GetMapping("/logs")
    @Operation(summary = "로그 엔트리 조회", description = "시스템 로그를 조회 (알림 및 메트릭 기반)")
    public ResponseEntity<ApiResponse<List<LogEntryResponse>>> getLogs(
            HttpServletRequest request,
            Authentication authentication,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "로그 레벨 필터") @RequestParam(required = false) String level,
            @Parameter(description = "로그 타입 필터") @RequestParam(required = false) String type,
            @Parameter(description = "로그 소스 필터") @RequestParam(required = false) String source,
            @Parameter(description = "시작 시간") @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "종료 시간") @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        log.info("로그 엔트리 조회 요청 - page: {}, size: {}, level: {}, type: {}, source: {}", 
                page, size, level, type, source);
        
        try {
            String tenantId = getTenantIdFromRequest(request, authentication);
            List<LogEntryResponse> logs = dashboardService.getLogs(
                    tenantId, page, size, level, type, source, startTime, endTime);
            
            return ResponseEntity.ok(ApiResponse.success(logs));
            
        } catch (Exception e) {
            log.error("로그 엔트리 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<List<LogEntryResponse>>builder()
                            .success(false)
                            .errorCode("LOG_001")
                            .message("로그 엔트리 조회 실패: " + e.getMessage())
                            .build());
        }
    }
    
    /**
     * 요청에서 테넌트 ID 추출
     */
    private String getTenantIdFromRequest(HttpServletRequest request, Authentication authentication) {
        // TenantContextHolder에서 테넌트 키 가져오기
        String tenantKey = TenantContextHolder.getCurrentTenantKey();
        if (tenantKey != null) {
            log.debug("테넌트 ID 추출 성공: {}", tenantKey);
            return tenantKey;
        }
        
        // 테넌트 컨텍스트가 없는 경우 기본값 반환
        log.warn("테넌트 컨텍스트가 설정되지 않음. 기본 테넌트 사용");
        return "default-tenant";
    }
}
