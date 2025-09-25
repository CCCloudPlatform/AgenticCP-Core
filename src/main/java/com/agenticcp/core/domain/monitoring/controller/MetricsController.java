package com.agenticcp.core.domain.monitoring.controller;

import com.agenticcp.core.common.dto.ApiResponse;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.monitoring.entity.Metric;
import com.agenticcp.core.domain.monitoring.enums.MonitoringErrorCode;
import com.agenticcp.core.common.enums.CommonErrorCode;
import com.agenticcp.core.domain.monitoring.repository.MetricRepository;
import com.agenticcp.core.domain.monitoring.service.MetricsCollectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 메트릭 관련 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/monitoring/metrics")
@RequiredArgsConstructor
public class MetricsController {

    private final MetricRepository metricRepository;
    private final MetricsCollectionService metricsCollectionService;

    /**
     * 메트릭 목록 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<Metric>>> getMetrics(
            @RequestParam(required = false) String metricName,
            @RequestParam(required = false) Metric.MetricType metricType,
            Pageable pageable) {
        
        try {
            log.info("Retrieving metrics: metricName={}, metricType={}, page={}, size={}", 
                    metricName, metricType, pageable.getPageNumber(), pageable.getPageSize());
            
            Page<Metric> metrics;
            if (metricName != null) {
                List<Metric> metricList = metricRepository.findLatestByMetricName(metricName, pageable);
                metrics = new org.springframework.data.domain.PageImpl<>(metricList, pageable, metricList.size());
            } else if (metricType != null) {
                metrics = metricRepository.findByMetricType(metricType, pageable);
            } else {
                metrics = metricRepository.findAll(pageable);
            }
            
            // 목록 조회: 빈 결과도 정상 응답
            return ResponseEntity.ok(ApiResponse.success(metrics));
        } catch (Exception e) {
            log.error("Error retrieving metrics", e);
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, 
                "메트릭 목록 조회 중 오류가 발생했습니다.");
        }
    }

    /**
     * 특정 메트릭 조회
     */
    @GetMapping("/{metricName}")
    public ResponseEntity<ApiResponse<List<Metric>>> getMetricByName(
            @PathVariable String metricName,
            @RequestParam(required = false) LocalDateTime startTime,
            @RequestParam(required = false) LocalDateTime endTime) {
        
        try {
            log.info("Retrieving metric: metricName={}, startTime={}, endTime={}", 
                    metricName, startTime, endTime);
            
            // 메트릭 이름 유효성 검증
            if (metricName == null || metricName.trim().isEmpty()) {
                throw new BusinessException(MonitoringErrorCode.INVALID_METRIC_NAME, 
                    "메트릭 이름이 유효하지 않습니다.");
            }
            
            // 시간 범위 유효성 검증
            if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
                throw new BusinessException(MonitoringErrorCode.INVALID_TIME_RANGE, 
                    "시작 시간이 종료 시간보다 늦을 수 없습니다.");
            }
            
            List<Metric> metrics;
            if (startTime != null && endTime != null) {
                metrics = metricRepository.findByMetricNameAndTimeRange(metricName, startTime, endTime);
            } else {
                metrics = metricRepository.findLatestByMetricName(metricName, Pageable.ofSize(100));
            }
            
            if (metrics.isEmpty()) {
                throw new ResourceNotFoundException(MonitoringErrorCode.METRIC_NOT_FOUND);
            }
            
            return ResponseEntity.ok(ApiResponse.success(metrics));
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving metric: {}", metricName, e);
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, 
                "메트릭 조회 중 오류가 발생했습니다.");
        }
    }

    /**
     * 메트릭 트렌드 조회
     */
    @GetMapping("/trend")
    public ResponseEntity<ApiResponse<List<Metric>>> getMetricsTrend(
            @RequestParam(required = false) LocalDateTime since) {
        
        try {
            log.info("Retrieving metrics trend: since={}", since);
            
            LocalDateTime sinceTime = since != null ? since : LocalDateTime.now().minusHours(1);
            List<Metric> metrics = metricRepository.findSince(sinceTime);
            
            // 목록 조회: 빈 결과도 정상 응답
            return ResponseEntity.ok(ApiResponse.success(metrics));
        } catch (Exception e) {
            log.error("Error retrieving metrics trend", e);
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, 
                "메트릭 트렌드 조회 중 오류가 발생했습니다.");
        }
    }

    /**
     * 수동 메트릭 수집
     */
    @PostMapping("/collect")
    public ResponseEntity<ApiResponse<String>> collectMetrics() {
        try {
            log.info("Manual metrics collection requested");
            
            metricsCollectionService.collectMetricsManually();
            return ResponseEntity.ok(ApiResponse.success("메트릭 수집이 완료되었습니다."));
        } catch (BusinessException e) {
            log.error("Business error during manual metrics collection: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during manual metrics collection", e);
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, 
                "메트릭 수집 중 예상치 못한 오류가 발생했습니다.");
        }
    }

    /**
     * 메트릭 이름 목록 조회
     */
    @GetMapping("/names")
    public ResponseEntity<ApiResponse<List<String>>> getMetricNames() {
        try {
            log.info("Retrieving metric names");
            
            List<String> metricNames = metricRepository.findDistinctMetricNames();
            
            // 목록 조회: 빈 결과도 정상 응답
            return ResponseEntity.ok(ApiResponse.success(metricNames));
        } catch (Exception e) {
            log.error("Error retrieving metric names", e);
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, 
                "메트릭 이름 목록 조회 중 오류가 발생했습니다.");
        }
    }
}
