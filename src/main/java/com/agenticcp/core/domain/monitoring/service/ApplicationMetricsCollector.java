package com.agenticcp.core.domain.monitoring.service;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.monitoring.dto.SystemMetrics;
import com.agenticcp.core.domain.monitoring.entity.Metric;
import com.agenticcp.core.domain.monitoring.enums.CollectorType;
import com.agenticcp.core.domain.monitoring.enums.MonitoringErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 애플리케이션 메트릭 수집기
 * 
 * <p>JVM 메트릭, 애플리케이션 성능 메트릭, 커스텀 메트릭을 수집합니다.
 * Spring Boot Actuator의 MetricsEndpoint를 활용하여 
 * 애플리케이션 관련 메트릭을 수집합니다.</p>
 * 
 * <p>수집하는 메트릭:</p>
 * <ul>
 *   <li>JVM 메모리 사용량 (heap, non-heap)</li>
 *   <li>JVM 스레드 정보</li>
 *   <li>GC 정보</li>
 *   <li>애플리케이션 커스텀 메트릭</li>
 * </ul>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@Component
public class ApplicationMetricsCollector implements MetricsCollector {

    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    private final MetricsEndpoint metricsEndpoint;
    
    /**
     * 수집기 활성화 상태
     */
    private volatile boolean enabled = true;
    
    /**
     * 생성자
     * 
     * @param metricsEndpoint Spring Boot Actuator 메트릭 엔드포인트
     */
    public ApplicationMetricsCollector(MetricsEndpoint metricsEndpoint) {
        this.metricsEndpoint = metricsEndpoint;
        log.info("애플리케이션 메트릭 수집기 초기화 완료");
    }

    /**
     * 수집기 활성화/비활성화 설정
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * 시스템 메트릭 수집 (애플리케이션 관점)
     * 
     * @return 수집된 시스템 메트릭 정보
     * @throws BusinessException 메트릭 수집 중 오류 발생 시
     */
    @Override
    public SystemMetrics collectSystemMetrics() {
        log.debug("애플리케이션 시스템 메트릭 수집 시작");
        
        try {
            if (!enabled) {
                log.debug("애플리케이션 메트릭 수집기가 비활성화됨");
                return createEmptySystemMetrics();
            }
            
            // JVM 메모리 정보 수집
            long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryBean.getHeapMemoryUsage().getMax();
            long nonHeapUsed = memoryBean.getNonHeapMemoryUsage().getUsed();
            long nonHeapMax = memoryBean.getNonHeapMemoryUsage().getMax();
            
            // 메모리 사용률 계산
            double memoryUsage = calculateMemoryUsage(heapUsed + nonHeapUsed, heapMax + nonHeapMax);
            long memoryUsedMB = (heapUsed + nonHeapUsed) / (1024 * 1024);
            long memoryTotalMB = (heapMax + nonHeapMax) / (1024 * 1024);
            
            // 시스템 정보 수집
            SystemMetrics.SystemInfo systemInfo = getApplicationSystemInfo();
            
            // 메타데이터 구성
            Map<String, Object> metadata = buildApplicationMetadata();
            
            SystemMetrics metrics = SystemMetrics.builder()
                    .cpuUsage(null) // 애플리케이션 수집기에서는 CPU 사용률을 수집하지 않음
                    .memoryUsage(memoryUsage)
                    .memoryUsedMB(memoryUsedMB)
                    .memoryTotalMB(memoryTotalMB)
                    .diskUsage(null) // 애플리케이션 수집기에서는 디스크 사용률을 수집하지 않음
                    .diskUsedGB(null)
                    .diskTotalGB(null)
                    .collectedAt(LocalDateTime.now())
                    .metadata(metadata)
                    .systemInfo(systemInfo)
                    .build();
                    
            log.debug("애플리케이션 시스템 메트릭 수집 완료: memoryUsage={}%, memoryUsedMB={}", 
                    memoryUsage, memoryUsedMB);
            
            return metrics;
            
        } catch (Exception e) {
            log.error("애플리케이션 시스템 메트릭 수집 실패", e);
            throw new BusinessException(MonitoringErrorCode.METRICS_COLLECTION_FAILED, 
                "애플리케이션 시스템 메트릭 수집 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 애플리케이션 메트릭 수집
     * 
     * @return 수집된 애플리케이션 메트릭 목록
     * @throws BusinessException 메트릭 수집 중 오류 발생 시
     */
    @Override
    public List<Metric> collectApplicationMetrics() {
        log.debug("애플리케이션 메트릭 수집 시작");
        
        List<Metric> metrics = new ArrayList<>();
        
        try {
            if (!enabled) {
                log.debug("애플리케이션 메트릭 수집기가 비활성화됨");
                return metrics;
            }
            
            LocalDateTime collectedAt = LocalDateTime.now();
            
            // JVM 메모리 메트릭 수집
            collectJvmMemoryMetrics(metrics, collectedAt);
            
            // JVM 스레드 메트릭 수집
            collectJvmThreadMetrics(metrics, collectedAt);
            
            // GC 메트릭 수집
            collectGcMetrics(metrics, collectedAt);
            
            // Spring Boot Actuator 메트릭 수집
            collectActuatorMetrics(metrics, collectedAt);
            
            log.debug("애플리케이션 메트릭 수집 완료: 총 {}개 메트릭", metrics.size());
            return metrics;
            
        } catch (Exception e) {
            log.error("애플리케이션 메트릭 수집 실패", e);
            throw new BusinessException(MonitoringErrorCode.METRICS_COLLECTION_FAILED, 
                "애플리케이션 메트릭 수집 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 수집기 타입 반환
     * 
     * @return 수집기 타입
     */
    @Override
    public CollectorType getCollectorType() {
        return CollectorType.APPLICATION;
    }
    
    /**
     * 수집기 활성화 여부 확인
     * 
     * @return 활성화 여부
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    
    /**
     * JVM 메모리 메트릭 수집
     */
    private void collectJvmMemoryMetrics(List<Metric> metrics, LocalDateTime collectedAt) {
        try {
            // Heap 메모리 사용량
            long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryBean.getHeapMemoryUsage().getMax();
            long heapCommitted = memoryBean.getHeapMemoryUsage().getCommitted();
            
            metrics.add(createMetric("jvm.memory.heap.used", heapUsed / (1024.0 * 1024.0), "MB", collectedAt));
            metrics.add(createMetric("jvm.memory.heap.max", heapMax / (1024.0 * 1024.0), "MB", collectedAt));
            metrics.add(createMetric("jvm.memory.heap.committed", heapCommitted / (1024.0 * 1024.0), "MB", collectedAt));
            
            // Heap 메모리 사용률
            if (heapMax > 0) {
                double heapUsage = (double) heapUsed / heapMax * 100.0;
                metrics.add(createMetric("jvm.memory.heap.usage", heapUsage, "%", collectedAt));
            }
            
            // Non-Heap 메모리 사용량
            long nonHeapUsed = memoryBean.getNonHeapMemoryUsage().getUsed();
            long nonHeapMax = memoryBean.getNonHeapMemoryUsage().getMax();
            long nonHeapCommitted = memoryBean.getNonHeapMemoryUsage().getCommitted();
            
            metrics.add(createMetric("jvm.memory.nonheap.used", nonHeapUsed / (1024.0 * 1024.0), "MB", collectedAt));
            metrics.add(createMetric("jvm.memory.nonheap.max", nonHeapMax / (1024.0 * 1024.0), "MB", collectedAt));
            metrics.add(createMetric("jvm.memory.nonheap.committed", nonHeapCommitted / (1024.0 * 1024.0), "MB", collectedAt));
            
            log.debug("JVM 메모리 메트릭 수집 완료: heapUsed={}MB, nonHeapUsed={}MB", 
                    heapUsed / (1024 * 1024), nonHeapUsed / (1024 * 1024));
                    
        } catch (Exception e) {
            log.warn("JVM 메모리 메트릭 수집 실패", e);
        }
    }
    
    /**
     * JVM 스레드 메트릭 수집
     */
    private void collectJvmThreadMetrics(List<Metric> metrics, LocalDateTime collectedAt) {
        try {
            int threadCount = threadBean.getThreadCount();
            int peakThreadCount = threadBean.getPeakThreadCount();
            long totalStartedThreadCount = threadBean.getTotalStartedThreadCount();
            int daemonThreadCount = threadBean.getDaemonThreadCount();
            
            metrics.add(createMetric("jvm.threads.count", (double) threadCount, "count", collectedAt));
            metrics.add(createMetric("jvm.threads.peak", (double) peakThreadCount, "count", collectedAt));
            metrics.add(createMetric("jvm.threads.total_started", (double) totalStartedThreadCount, "count", collectedAt));
            metrics.add(createMetric("jvm.threads.daemon", (double) daemonThreadCount, "count", collectedAt));
            
            log.debug("JVM 스레드 메트릭 수집 완료: threadCount={}, daemonCount={}", 
                    threadCount, daemonThreadCount);
                    
        } catch (Exception e) {
            log.warn("JVM 스레드 메트릭 수집 실패", e);
        }
    }
    
    /**
     * GC 메트릭 수집
     */
    private void collectGcMetrics(List<Metric> metrics, LocalDateTime collectedAt) {
        try {
            var gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
            
            for (var gcBean : gcBeans) {
                String gcName = gcBean.getName();
                long collectionCount = gcBean.getCollectionCount();
                long collectionTime = gcBean.getCollectionTime();
                
                metrics.add(createMetric("jvm.gc." + sanitizeMetricName(gcName) + ".count", 
                        (double) collectionCount, "count", collectedAt));
                metrics.add(createMetric("jvm.gc." + sanitizeMetricName(gcName) + ".time", 
                        (double) collectionTime, "ms", collectedAt));
            }
            
            log.debug("GC 메트릭 수집 완료: {}개 GC 빈", gcBeans.size());
            
        } catch (Exception e) {
            log.warn("GC 메트릭 수집 실패", e);
        }
    }
    
    /**
     * Spring Boot Actuator 메트릭 수집
     */
    private void collectActuatorMetrics(List<Metric> metrics, LocalDateTime collectedAt) {
        try {
            if (metricsEndpoint == null) {
                log.debug("MetricsEndpoint가 null이므로 Actuator 메트릭 수집 건너뜀");
                return;
            }
            
            // HTTP 요청 메트릭
            collectHttpMetrics(metrics, collectedAt);
            
            // 데이터베이스 연결 메트릭
            collectDatabaseMetrics(metrics, collectedAt);
            
            log.debug("Actuator 메트릭 수집 완료");
            
        } catch (Exception e) {
            log.warn("Actuator 메트릭 수집 실패", e);
        }
    }
    
    /**
     * HTTP 메트릭 수집
     */
    private void collectHttpMetrics(List<Metric> metrics, LocalDateTime collectedAt) {
        try {
            // HTTP 요청 수
            var httpRequests = metricsEndpoint.metric("http.server.requests", null);
            if (httpRequests != null && httpRequests.getMeasurements() != null) {
                for (var measurement : httpRequests.getMeasurements()) {
                    if ("COUNT".equals(measurement.getStatistic().name())) {
                        metrics.add(createMetric("http.server.requests.count", 
                                measurement.getValue(), "count", collectedAt));
                    }
                }
            }
            
        } catch (Exception e) {
            log.debug("HTTP 메트릭 수집 실패 (정상적인 경우일 수 있음)", e);
        }
    }
    
    /**
     * 데이터베이스 메트릭 수집
     */
    private void collectDatabaseMetrics(List<Metric> metrics, LocalDateTime collectedAt) {
        try {
            // 데이터베이스 연결 수
            var dbConnections = metricsEndpoint.metric("hikaricp.connections", null);
            if (dbConnections != null && dbConnections.getMeasurements() != null) {
                for (var measurement : dbConnections.getMeasurements()) {
                    String statistic = measurement.getStatistic().name();
                    if ("VALUE".equals(statistic)) {
                        metrics.add(createMetric("hikaricp.connections.active", 
                                measurement.getValue(), "count", collectedAt));
                    }
                }
            }
            
        } catch (Exception e) {
            log.debug("데이터베이스 메트릭 수집 실패 (정상적인 경우일 수 있음)", e);
        }
    }
    
    /**
     * 메트릭 생성 헬퍼 메서드
     */
    private Metric createMetric(String metricName, Double value, String unit, LocalDateTime collectedAt) {
        return Metric.builder()
                .metricName(metricName)
                .metricValue(value)
                .unit(unit)
                .metricType(Metric.MetricType.APPLICATION)
                .collectedAt(collectedAt)
                .source("application")
                .status(Metric.Status.ACTIVE)
                .build();
    }
    
    /**
     * 메트릭 이름 정리 (특수문자 제거)
     */
    private String sanitizeMetricName(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
    
    /**
     * 메모리 사용률 계산
     */
    private double calculateMemoryUsage(long used, long max) {
        if (max <= 0) {
            return 0.0;
        }
        return (double) used / max * 100.0;
    }
    
    /**
     * 애플리케이션 시스템 정보 수집
     */
    private SystemMetrics.SystemInfo getApplicationSystemInfo() {
        try {
            return SystemMetrics.SystemInfo.builder()
                    .hostname(getHostname())
                    .osName(System.getProperty("os.name"))
                    .osVersion(System.getProperty("os.version"))
                    .javaVersion(System.getProperty("java.version"))
                    .availableProcessors(Runtime.getRuntime().availableProcessors())
                    .build();
        } catch (Exception e) {
            log.warn("애플리케이션 시스템 정보 수집 실패", e);
            return SystemMetrics.SystemInfo.builder().build();
        }
    }
    
    /**
     * 호스트명 수집
     */
    private String getHostname() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    /**
     * 애플리케이션 메타데이터 구성
     */
    private Map<String, Object> buildApplicationMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("collector_type", "APPLICATION");
        metadata.put("jvm_version", System.getProperty("java.version"));
        metadata.put("jvm_vendor", System.getProperty("java.vendor"));
        metadata.put("available_processors", Runtime.getRuntime().availableProcessors());
        metadata.put("max_memory", Runtime.getRuntime().maxMemory());
        metadata.put("free_memory", Runtime.getRuntime().freeMemory());
        metadata.put("total_memory", Runtime.getRuntime().totalMemory());
        return metadata;
    }
    
    /**
     * 빈 시스템 메트릭 생성
     */
    private SystemMetrics createEmptySystemMetrics() {
        return SystemMetrics.builder()
                .cpuUsage(null)
                .memoryUsage(null)
                .memoryUsedMB(null)
                .memoryTotalMB(null)
                .diskUsage(null)
                .diskUsedGB(null)
                .diskTotalGB(null)
                .collectedAt(LocalDateTime.now())
                .metadata(Map.of("collector_type", "APPLICATION", "enabled", false))
                .systemInfo(SystemMetrics.SystemInfo.builder().build())
                .build();
    }
}
