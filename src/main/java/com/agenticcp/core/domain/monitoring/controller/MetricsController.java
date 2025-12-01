package com.agenticcp.core.domain.monitoring.controller;

import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.domain.monitoring.entity.Metric;
import com.agenticcp.core.domain.monitoring.enums.MonitoringErrorCode;
import com.agenticcp.core.domain.monitoring.repository.MetricRepository;
import com.agenticcp.core.domain.monitoring.service.MetricsCollectionService;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 메트릭 관련 API 컨트롤러
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/monitoring/metrics")
@RequiredArgsConstructor
@Tag(name = "Metric Monitoring", description = "메트릭 조회/수집 API")
public class MetricsController {

    private final MetricRepository metricRepository;
    private final MetricsCollectionService metricsCollectionService;

    /**
     * 메트릭 목록 조회
     * 
     * @param metricName 메트릭 이름 (선택)
     * @param metricType 메트릭 타입 (선택)
     * @param pageable 페이징 정보
     * @return 메트릭 목록 (페이징)
     */
    @Operation(summary = "메트릭 목록 조회", description = "필터(이름/타입)와 페이징으로 메트릭 목록을 조회합니다")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<Metric>>> getMetrics(
            @RequestParam(required = false) String metricName,
            @RequestParam(required = false) Metric.MetricType metricType,
            Pageable pageable) {
        
        log.info("Retrieving metrics: metricName={}, metricType={}, page={}, size={}", 
                metricName, metricType, pageable.getPageNumber(), pageable.getPageSize());
        
        String tenantId = TenantContextHolder.getCurrentTenantKeyOrThrow();
        Page<Metric> metrics;
        if (metricName != null) {
            List<Metric> metricList = metricRepository.findLatestByMetricName(metricName, tenantId, pageable);
            metrics = new org.springframework.data.domain.PageImpl<>(metricList, pageable, metricList.size());
        } else if (metricType != null) {
            metrics = metricRepository.findByMetricType(metricType, tenantId, pageable);
        } else {
            metrics = metricRepository.findByTenantId(tenantId, pageable);
        }
        
        // 목록 조회: 빈 결과도 정상 응답
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }

    /**
     * 특정 메트릭 조회
     * 
     * @param metricName 메트릭 이름
     * @param startTime 시작 시간 (선택)
     * @param endTime 종료 시간 (선택)
     * @return 메트릭 목록
     * @throws ResourceNotFoundException 메트릭을 찾을 수 없는 경우
     * @throws BusinessException 시간 범위가 유효하지 않은 경우
     */
    @Operation(summary = "특정 메트릭 조회", description = "메트릭 이름과 시간 범위로 데이터 조회")
    @GetMapping("/{metricName}")
    public ResponseEntity<ApiResponse<List<Metric>>> getMetricByName(
            @PathVariable @NotBlank String metricName,
            @RequestParam(required = false) LocalDateTime startTime,
            @RequestParam(required = false) LocalDateTime endTime) {
        
        log.info("Retrieving metric: metricName={}, startTime={}, endTime={}", 
                metricName, startTime, endTime);
        
        // 시간 범위 유효성 검증
        if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
            throw new BusinessException(MonitoringErrorCode.INVALID_TIME_RANGE, 
                "시작 시간이 종료 시간보다 늦을 수 없습니다.");
        }
        
        String tenantId = TenantContextHolder.getCurrentTenantKeyOrThrow();
        List<Metric> metrics;
        if (startTime != null && endTime != null) {
            metrics = metricRepository.findByMetricNameAndTimeRange(metricName, tenantId, startTime, endTime);
        } else {
            metrics = metricRepository.findLatestByMetricName(metricName, tenantId, Pageable.ofSize(100));
        }
        
        if (metrics.isEmpty()) {
            throw new ResourceNotFoundException(MonitoringErrorCode.METRIC_NOT_FOUND);
        }
        
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }

    /**
     * 메트릭 트렌드 조회
     * 
     * @param since 기준 시점 (선택, 기본값: 1시간 전)
     * @return 메트릭 트렌드 목록
     */
    @Operation(summary = "메트릭 트렌드 조회", description = "기준 시점부터 최신까지 트렌드 조회")
    @GetMapping("/trend")
    public ResponseEntity<ApiResponse<List<Metric>>> getMetricsTrend(
            @RequestParam(required = false) LocalDateTime since) {
        
        log.info("Retrieving metrics trend: since={}", since);
        
        String tenantId = TenantContextHolder.getCurrentTenantKeyOrThrow();
        LocalDateTime sinceTime = since != null ? since : LocalDateTime.now().minusHours(1);
        List<Metric> metrics = metricRepository.findSince(tenantId, sinceTime);
        
        // 목록 조회: 빈 결과도 정상 응답
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }

    /**
     * 수동 메트릭 수집
     * 
     * @return 성공 메시지
     * @throws BusinessException 메트릭 수집 중 오류가 발생한 경우
     */
    @Operation(summary = "수동 메트릭 수집", description = "즉시 메트릭 수집 작업을 실행합니다")
    @PostMapping("/collect")
    public ResponseEntity<ApiResponse<String>> collectMetrics() {
        log.info("Manual metrics collection requested");
        
        metricsCollectionService.collectMetricsManually();
        return ResponseEntity.ok(ApiResponse.success("메트릭 수집이 완료되었습니다."));
    }

    /**
     * 메트릭 이름 목록 조회
     * 
     * @return 메트릭 이름 목록
     */
    @Operation(summary = "메트릭 이름 목록 조회", description = "현재 테넌트에서 관측된 메트릭 이름 목록을 조회합니다")
    @GetMapping("/names")
    public ResponseEntity<ApiResponse<List<String>>> getMetricNames() {
        log.info("Retrieving metric names");
        
        String tenantId = TenantContextHolder.getCurrentTenantKeyOrThrow();
        List<String> metricNames = metricRepository.findDistinctMetricNames(tenantId);
        
        // 목록 조회: 빈 결과도 정상 응답
        return ResponseEntity.ok(ApiResponse.success(metricNames));
    }
}
