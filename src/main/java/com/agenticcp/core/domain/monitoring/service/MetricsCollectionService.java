package com.agenticcp.core.domain.monitoring.service;

import com.agenticcp.core.common.exception.BusinessException;
// TODO: 테넌트 도메인 구현 후 활성화 예정
// import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.domain.monitoring.dto.SystemMetrics;
import com.agenticcp.core.domain.monitoring.entity.Metric;
import com.agenticcp.core.domain.monitoring.entity.MetricThreshold;
import com.agenticcp.core.common.enums.CommonErrorCode;
import com.agenticcp.core.domain.monitoring.repository.MetricRepository;
import com.agenticcp.core.domain.monitoring.repository.MetricThresholdRepository;
import com.agenticcp.core.domain.monitoring.enums.CollectorType;
import com.agenticcp.core.domain.monitoring.storage.MetricsStorageFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 메트릭 수집을 담당하는 서비스
 * 시스템 리소스 메트릭과 애플리케이션 메트릭을 수집하여 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MetricsCollectionService {

    private final MetricRepository metricRepository;
    private final MetricThresholdRepository metricThresholdRepository;
    private final SystemMetricsCollector systemMetricsCollector;
    private final MetricsCollectorFactory metricsCollectorFactory;
    private final MetricsStorageFactory metricsStorageFactory;

    /**
     * 1분마다 자동으로 메트릭 수집 실행
     */
    @Scheduled(fixedRate = 60000)
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
     */
    @Transactional
    public void collectSystemMetrics() {
        try {
            log.debug("Collecting system metrics...");
            SystemMetrics systemMetrics = systemMetricsCollector.collectSystemMetrics();
            saveSystemMetrics(systemMetrics);
            log.debug("System metrics collected successfully");
        } catch (BusinessException e) {
            log.error("Business error collecting system metrics: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error collecting system metrics", e);
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, 
                "시스템 메트릭 수집 중 예상치 못한 오류가 발생했습니다.");
        }
    }

    /**
     * 애플리케이션 메트릭 수집
     */
    @Transactional
    public void collectApplicationMetrics() {
        try {
            log.debug("Collecting application metrics...");
            
            // 애플리케이션 메트릭 수집기 생성
            MetricsCollector applicationCollector = metricsCollectorFactory.createCollector(CollectorType.APPLICATION);
            
            if (applicationCollector != null && applicationCollector.isEnabled()) {
                // 애플리케이션 메트릭 수집
                List<Metric> applicationMetrics = applicationCollector.collectApplicationMetrics();
                
                // 수집된 메트릭 저장
                for (Metric metric : applicationMetrics) {
                    saveMetric(metric);
                }
                
                log.debug("Application metrics collected successfully: {} metrics", applicationMetrics.size());
            } else {
                log.debug("Application metrics collector is disabled or not available");
            }
            
        } catch (BusinessException e) {
            log.error("Business error collecting application metrics: {}", e.getMessage(), e);
            // 애플리케이션 메트릭 수집 실패는 시스템 메트릭에 영향주지 않음
        } catch (Exception e) {
            log.error("Unexpected error collecting application metrics", e);
            // 애플리케이션 메트릭 수집 실패는 시스템 메트릭에 영향주지 않음
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
