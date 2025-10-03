package com.agenticcp.core.domain.monitoring.service;

import com.agenticcp.core.common.exception.BusinessException;
// TODO: 테넌트 도메인 구현 후 활성화 예정
// import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.domain.monitoring.config.RetryConfig;
import com.agenticcp.core.domain.monitoring.dto.SystemMetrics;
import com.agenticcp.core.domain.monitoring.entity.Metric;
import com.agenticcp.core.domain.monitoring.entity.MetricThreshold;
import com.agenticcp.core.common.enums.CommonErrorCode;
import com.agenticcp.core.domain.monitoring.enums.MonitoringErrorCode;
import com.agenticcp.core.domain.monitoring.repository.MetricRepository;
import com.agenticcp.core.domain.monitoring.repository.MetricThresholdRepository;
import com.agenticcp.core.domain.monitoring.enums.CollectorType;
import com.agenticcp.core.domain.monitoring.storage.MetricsStorageFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 메트릭 수집을 담당하는 서비스
 * 
 * <p>시스템 리소스 메트릭과 애플리케이션 메트릭을 수집하여 저장합니다.
 * 
 * <p>Issue #39: Task 8 - 재시도 로직 및 오류 처리 구현
 * <ul>
 *   <li>메트릭 수집 실패 시 자동 재시도 (최대 3회)</li>
 *   <li>재시도 실패 시 캐시된 데이터로 폴백</li>
 *   <li>재시도 통계 수집</li>
 * </ul>
 * 
 * @author AgenticCP Team
 * @version 1.1.0
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MetricsCollectionService {
    
    /**
     * 메트릭 수집 주기 (밀리초)
     * 30초마다 자동 수집 (Issue #39 요구사항: 1분 이하)
     */
    private static final int DEFAULT_TIMEOUT = 30000;
    
    /**
     * 기본 재시도 횟수
     * @deprecated RetryConfig.MAX_RETRY_ATTEMPTS 사용 권장
     */
    @Deprecated
    private static final int DEFAULT_RETRY_COUNT = 3;

    private final MetricRepository metricRepository;
    private final MetricThresholdRepository metricThresholdRepository;
    private final SystemMetricsCollector systemMetricsCollector;
    private final MetricsCollectorFactory metricsCollectorFactory;
    private final MetricsStorageFactory metricsStorageFactory;
    private final MetricsCache metricsCache;

    /**
     * 1분마다 자동으로 메트릭 수집 실행
     */
    @Scheduled(fixedRate = DEFAULT_TIMEOUT)
    @Transactional
    public void collectMetricsScheduled() {
        try {
            log.info("Starting scheduled metrics collection...");
            collectSystemMetrics();
            collectApplicationMetrics();
            log.info("Scheduled metrics collection completed successfully");
        } catch (BusinessException e) {
            log.error("Business error during scheduled metrics collection: {}", e.getMessage(), e);
            // 스케줄된 작업은 예외를 다시 던지지 않음
        } catch (Exception e) {
            log.error("Unexpected error during scheduled metrics collection", e);
            // 스케줄된 작업은 예외를 다시 던지지 않음
        }
    }

    /**
     * 시스템 리소스 메트릭 수집
     * 
     * <p>Issue #39 Task 8: 재시도 로직 적용
     * <ul>
     *   <li>최대 3회 재시도</li>
     *   <li>지수 백오프: 1초 → 2초 → 4초</li>
     *   <li>실패 시 recoverFromSystemMetricsFailure 호출</li>
     * </ul>
     * 
     * @throws BusinessException 메트릭 수집 실패 시
     */
    @Retryable(
        value = {BusinessException.class, RuntimeException.class},
        maxAttempts = RetryConfig.MAX_RETRY_ATTEMPTS,
        backoff = @Backoff(delay = RetryConfig.INITIAL_BACKOFF_DELAY, multiplier = RetryConfig.BACKOFF_MULTIPLIER)
    )
    @Transactional
    public void collectSystemMetrics() {
        try {
            log.debug("시스템 메트릭 수집 시작...");
            
            SystemMetrics systemMetrics = systemMetricsCollector.collectSystemMetrics();
            saveSystemMetrics(systemMetrics);
            
            // 캐시에 저장 (폴백용)
            metricsCache.cacheSystemMetrics(systemMetrics);
            
            log.debug("시스템 메트릭 수집 완료");
        } catch (BusinessException e) {
            log.error("시스템 메트릭 수집 중 비즈니스 오류 발생: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("시스템 메트릭 수집 중 예상치 못한 오류 발생", e);
            throw new BusinessException(MonitoringErrorCode.SYSTEM_METRICS_UNAVAILABLE, 
                "시스템 메트릭 수집 중 예상치 못한 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 시스템 메트릭 수집 실패 시 폴백 처리
     * 
     * <p>모든 재시도가 실패했을 때 호출됩니다.
     * <ul>
     *   <li>재시도 실패 기록</li>
     *   <li>캐시된 데이터 사용</li>
     *   <li>캐시 없으면 예외 발생</li>
     * </ul>
     * 
     * @param e 발생한 예외
     * @throws BusinessException 폴백도 실패한 경우
     */
    @Recover
    public void recoverFromSystemMetricsFailure(Exception e) {
        log.error("시스템 메트릭 수집의 모든 재시도가 실패했습니다.", e);
        
        // 캐시된 데이터 사용
        SystemMetrics cachedMetrics = metricsCache.getLastSuccessfulSystemMetrics();
        
        if (cachedMetrics != null) {
            log.warn("캐시된 시스템 메트릭을 사용합니다. collectedAt={}", cachedMetrics.getCollectedAt());
            // 캐시된 데이터는 이미 저장되어 있으므로 별도 저장 불필요
            return;
        }
        
        // 캐시도 없으면 예외 발생
        log.error("캐시된 시스템 메트릭도 없습니다. 폴백 실패.");
        throw new BusinessException(MonitoringErrorCode.RETRY_EXHAUSTED, 
            "시스템 메트릭 수집의 모든 재시도가 실패했으며, 캐시된 데이터도 없습니다.");
    }

    /**
     * 애플리케이션 메트릭 수집
     * 
     * <p>Issue #39 Task 8: 재시도 로직 적용 및 부분 실패 처리
     * <ul>
     *   <li>최대 3회 재시도</li>
     *   <li>지수 백오프: 1초 → 2초 → 4초</li>
     *   <li>부분 실패 허용: 일부 메트릭 실패해도 나머지는 저장</li>
     *   <li>실패 시 recoverFromApplicationMetricsFailure 호출</li>
     * </ul>
     */
    @Retryable(
        value = {BusinessException.class, RuntimeException.class},
        maxAttempts = RetryConfig.MAX_RETRY_ATTEMPTS,
        backoff = @Backoff(delay = RetryConfig.INITIAL_BACKOFF_DELAY, multiplier = RetryConfig.BACKOFF_MULTIPLIER)
    )
    @Transactional
    public void collectApplicationMetrics() {
        try {
            log.debug("애플리케이션 메트릭 수집 시작...");
            
            // 애플리케이션 메트릭 수집기 생성
            MetricsCollector applicationCollector = metricsCollectorFactory.createCollector(CollectorType.APPLICATION);
            
            if (applicationCollector != null && applicationCollector.isEnabled()) {
                // 애플리케이션 메트릭 수집
                List<Metric> applicationMetrics = applicationCollector.collectApplicationMetrics();
                
                // Issue #39 Task 8: 부분 실패 처리
                saveMetricsWithPartialFailureHandling(applicationMetrics);
                
                // 캐시에 저장 (폴백용)
                metricsCache.cacheApplicationMetrics(applicationMetrics);
                
                log.debug("애플리케이션 메트릭 수집 완료: {} 메트릭", applicationMetrics.size());
            } else {
                log.debug("애플리케이션 메트릭 수집기가 비활성화되어 있거나 사용 불가능합니다.");
            }
            
        } catch (BusinessException e) {
            log.error("애플리케이션 메트릭 수집 중 비즈니스 오류 발생: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("애플리케이션 메트릭 수집 중 예상치 못한 오류 발생", e);
            throw new BusinessException(MonitoringErrorCode.METRICS_COLLECTION_FAILED, 
                "애플리케이션 메트릭 수집 중 예상치 못한 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 애플리케이션 메트릭 수집 실패 시 폴백 처리
     * 
     * <p>모든 재시도가 실패했을 때 호출됩니다.
     * 
     * @param e 발생한 예외
     */
    @Recover
    public void recoverFromApplicationMetricsFailure(Exception e) {
        log.error("애플리케이션 메트릭 수집의 모든 재시도가 실패했습니다.", e);
        
        // 캐시된 데이터 사용
        List<Metric> cachedMetrics = metricsCache.getLastSuccessfulApplicationMetrics();
        
        if (!cachedMetrics.isEmpty()) {
            log.warn("캐시된 애플리케이션 메트릭 {}개를 사용합니다.", cachedMetrics.size());
            // 캐시된 데이터는 이미 저장되어 있으므로 별도 저장 불필요
            return;
        }
        
        // 캐시도 없으면 경고만 로그 (애플리케이션 메트릭은 선택적이므로 예외 발생하지 않음)
        log.warn("캐시된 애플리케이션 메트릭도 없습니다. 이번 수집 주기는 건너뜁니다.");
    }
    
    /**
     * 부분 실패 처리를 지원하는 메트릭 저장
     * 
     * <p>Issue #39 Task 8: 부분 실패 처리
     * <ul>
     *   <li>일부 메트릭 저장 실패 시 나머지는 계속 저장</li>
     *   <li>실패한 메트릭은 로그로 기록</li>
     *   <li>모든 메트릭 실패 시에만 예외 발생</li>
     * </ul>
     * 
     * @param metrics 저장할 메트릭 목록
     * @throws BusinessException 모든 메트릭 저장에 실패한 경우
     */
    private void saveMetricsWithPartialFailureHandling(List<Metric> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            log.debug("저장할 메트릭이 없습니다.");
            return;
        }
        
        int successCount = 0;
        int failureCount = 0;
        List<String> failedMetricNames = new ArrayList<>();
        
        for (Metric metric : metrics) {
            try {
                // 직접 저장 (saveMetric 내부 예외 처리 우회)
                metricRepository.save(metric);
                checkThresholdViolations(metric);
                successCount++;
                log.debug("메트릭 저장 성공: {} = {} {}", metric.getMetricName(), metric.getMetricValue(), metric.getUnit());
            } catch (Exception e) {
                failureCount++;
                failedMetricNames.add(metric.getMetricName());
                log.warn("메트릭 저장 실패: name={}, error={}", metric.getMetricName(), e.getMessage());
            }
        }
        
        log.info("메트릭 저장 완료: 성공={}, 실패={}", successCount, failureCount);
        
        if (failureCount > 0) {
            log.warn("실패한 메트릭 목록: {}", failedMetricNames);
        }
        
        // 모든 메트릭 저장 실패 시에만 예외 발생
        if (successCount == 0 && failureCount > 0) {
            throw new BusinessException(MonitoringErrorCode.METRIC_SAVE_FAILED, 
                String.format("모든 메트릭 저장에 실패했습니다. 실패 수: %d", failureCount));
        }
    }

    /**
     * 수동 메트릭 수집 (API 호출용)
     */
    @Transactional
    public void collectMetricsManually() {
        log.info("Manual metrics collection requested");
        try {
            collectSystemMetrics();
            collectApplicationMetrics();
        } catch (BusinessException e) {
            log.error("Business error during manual metrics collection: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during manual metrics collection", e);
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, 
                "수동 메트릭 수집 중 예상치 못한 오류가 발생했습니다.");
        }
    }

    /**
     * 시스템 메트릭을 데이터베이스에 저장
     */
    private void saveSystemMetrics(SystemMetrics systemMetrics) {
        try {
            LocalDateTime collectedAt = systemMetrics.getCollectedAt();
            Map<String, Object> metadata = systemMetrics.getMetadata();

            // CPU 사용률 저장
            if (systemMetrics.getCpuUsage() != null) {
                saveMetric("cpu.usage", systemMetrics.getCpuUsage(), "%", 
                          Metric.MetricType.SYSTEM, collectedAt, metadata);
            }

            // 메모리 사용률 저장
            if (systemMetrics.getMemoryUsage() != null) {
                saveMetric("memory.usage", systemMetrics.getMemoryUsage(), "%", 
                          Metric.MetricType.SYSTEM, collectedAt, metadata);
            }

            // 메모리 사용량 저장
            if (systemMetrics.getMemoryUsedMB() != null) {
                saveMetric("memory.used", systemMetrics.getMemoryUsedMB().doubleValue(), "MB", 
                          Metric.MetricType.SYSTEM, collectedAt, metadata);
            }

            // 메모리 총량 저장
            if (systemMetrics.getMemoryTotalMB() != null) {
                saveMetric("memory.total", systemMetrics.getMemoryTotalMB().doubleValue(), "MB", 
                          Metric.MetricType.SYSTEM, collectedAt, metadata);
            }

            // 디스크 사용률 저장
            if (systemMetrics.getDiskUsage() != null) {
                saveMetric("disk.usage", systemMetrics.getDiskUsage(), "%", 
                          Metric.MetricType.SYSTEM, collectedAt, metadata);
            }

            // 디스크 사용량 저장
            if (systemMetrics.getDiskUsedGB() != null) {
                saveMetric("disk.used", systemMetrics.getDiskUsedGB().doubleValue(), "GB", 
                          Metric.MetricType.SYSTEM, collectedAt, metadata);
            }

            // 디스크 총량 저장
            if (systemMetrics.getDiskTotalGB() != null) {
                saveMetric("disk.total", systemMetrics.getDiskTotalGB().doubleValue(), "GB", 
                          Metric.MetricType.SYSTEM, collectedAt, metadata);
            }
        } catch (Exception e) {
            log.error("Error saving system metrics to database", e);
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, 
                "메트릭 데이터 저장 중 오류가 발생했습니다.");
        }
    }

    /**
     * 메트릭 엔티티 저장
     */
    private void saveMetric(Metric metric) {
        try {
            metricRepository.save(metric);
            
            // ✅ 임계값 위반 확인
            checkThresholdViolations(metric);
            
            log.debug("Saved metric: {} = {} {}", metric.getMetricName(), metric.getMetricValue(), metric.getUnit());
        } catch (Exception e) {
            log.error("Error saving metric: {} = {} {}", metric.getMetricName(), metric.getMetricValue(), metric.getUnit(), e);
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, 
                "메트릭 저장 중 오류가 발생했습니다.");
        }
    }

    /**
     * 개별 메트릭 저장
     */
    private void saveMetric(String metricName, Double metricValue, String unit, 
                           Metric.MetricType metricType, LocalDateTime collectedAt, 
                           Map<String, Object> metadata) {
        try {
            Metric metric = Metric.builder()
                    .metricName(metricName)
                    .metricValue(metricValue)
                    .unit(unit)
                    .metricType(metricType)
                    .collectedAt(collectedAt)
                    .metadata(convertMetadataToString(metadata))
                    // TODO: 테넌트 도메인 구현 후 활성화 예정
                    // .tenantId(TenantContextHolder.getCurrentTenantKeyOrThrow())
                    .build();

            metricRepository.save(metric);
            
            // ✅ 임계값 위반 확인
            checkThresholdViolations(metric);
            
            log.debug("Saved metric: {} = {} {}", metricName, metricValue, unit);
        } catch (Exception e) {
            log.error("Error saving metric: {} = {} {}", metricName, metricValue, unit, e);
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, 
                "메트릭 저장 중 오류가 발생했습니다.");
        }
    }

    /**
     * 메타데이터를 JSON 문자열로 변환
     */
    private String convertMetadataToString(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        
        try {
            // TODO: JSON 변환 로직 구현 (Jackson 또는 Gson 사용)
            return metadata.toString();
        } catch (Exception e) {
            log.warn("Failed to convert metadata to string", e);
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, 
                "메타데이터 변환에 실패했습니다.");
        }
    }

    /**
     * 임계값 위반 확인
     */
    private void checkThresholdViolations(Metric metric) {
        try {
            List<MetricThreshold> thresholds = metricThresholdRepository.findByMetricName(metric.getMetricName());
            
            for (MetricThreshold threshold : thresholds) {
                if (threshold.isThresholdViolated(metric.getMetricValue())) {
                    log.warn("🚨 Threshold violated for metric {}: {} {} {} {}", 
                        metric.getMetricName(), 
                        metric.getMetricValue(), 
                        threshold.getOperator(), 
                        threshold.getThresholdValue(),
                        threshold.getThresholdType());
                    
                    // TODO: 알림 발송 로직 구현
                    // sendAlert(threshold, metric);
                }
            }
        } catch (Exception e) {
            log.error("Error checking threshold violations for metric: {}", metric.getMetricName(), e);
            // 임계값 확인 실패는 메트릭 저장을 중단시키지 않음
        }
    }
}
