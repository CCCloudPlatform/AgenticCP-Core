package com.agenticcp.core.domain.monitoring.service;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.monitoring.dto.SystemMetrics;
import com.agenticcp.core.domain.monitoring.entity.Metric;
import com.agenticcp.core.domain.monitoring.enums.CollectorType;
import com.agenticcp.core.domain.monitoring.enums.MonitoringErrorCode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Micrometer 기반 메트릭 수집기
 * 
 * @author AgenticCP
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MicrometerMetricsCollector implements MetricsCollector {

    private final MeterRegistry meterRegistry;
    
    @Value("${monitoring.micrometer.enabled:true}")
    private boolean enabled;
    
    /**
     * 수집기 활성화 상태 설정
     * 
     * @param enabled 활성화 여부
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 시스템 메트릭 수집 (Micrometer 방식)
     * 
     * @return 수집된 시스템 메트릭
     * @throws BusinessException 메트릭 수집 중 오류 발생 시
     */
    @Override
    public SystemMetrics collectSystemMetrics() {
        log.debug("Micrometer 시스템 메트릭 수집 시작");
        
        try {
            if (!enabled) {
                log.debug("Micrometer 메트릭 수집기가 비활성화됨");
                return null;
            }

            // JVM 메모리 정보 수집
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryBean.getHeapMemoryUsage().getMax();
            
            // 메모리 사용률 계산
            double memoryUsage = heapMax > 0 ? (double) heapUsed / heapMax * 100.0 : 0.0;
            long memoryUsedMB = heapUsed / (1024 * 1024);
            long memoryTotalMB = heapMax / (1024 * 1024);
            
            // 시스템 정보 구성
            SystemMetrics.SystemInfo systemInfo = SystemMetrics.SystemInfo.builder()
                    .hostname(System.getProperty("user.name"))
                    .osName(System.getProperty("os.name"))
                    .osVersion(System.getProperty("os.version"))
                    .javaVersion(System.getProperty("java.version"))
                    .availableProcessors(Runtime.getRuntime().availableProcessors())
                    .build();
            
            // 메타데이터 구성
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("micrometer.version", "1.12.0");
            metadata.put("registry.type", meterRegistry.getClass().getSimpleName());
            metadata.put("collection.timestamp", LocalDateTime.now().toString());
            
            SystemMetrics metrics = SystemMetrics.builder()
                    .cpuUsage(null) // Micrometer에서는 CPU 사용률을 별도로 수집하지 않음
                    .memoryUsage(memoryUsage)
                    .memoryUsedMB(memoryUsedMB)
                    .memoryTotalMB(memoryTotalMB)
                    .diskUsage(null) // Micrometer에서는 디스크 사용률을 별도로 수집하지 않음
                    .diskUsedGB(null)
                    .diskTotalGB(null)
                    .collectedAt(LocalDateTime.now())
                    .metadata(metadata)
                    .systemInfo(systemInfo)
                    .build();
                    
            log.debug("Micrometer 시스템 메트릭 수집 완료: memoryUsage={}%, memoryUsedMB={}", 
                    memoryUsage, memoryUsedMB);
            
            return metrics;
            
        } catch (Exception e) {
            log.error("Micrometer 시스템 메트릭 수집 실패", e);
            throw new BusinessException(MonitoringErrorCode.METRICS_COLLECTION_FAILED, 
                "Micrometer 시스템 메트릭 수집 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 애플리케이션 메트릭 수집 (Micrometer 방식)
     * 
     * @return 수집된 애플리케이션 메트릭 목록
     * @throws BusinessException 메트릭 수집 중 오류 발생 시
     */
    @Override
    public List<Metric> collectApplicationMetrics() {
        log.debug("Micrometer 애플리케이션 메트릭 수집 시작");
        
        List<Metric> metrics = new ArrayList<>();
        
        try {
            if (!enabled) {
                log.debug("Micrometer 메트릭 수집기가 비활성화됨");
                return metrics;
            }
            
            LocalDateTime collectedAt = LocalDateTime.now();
            
            // JVM 메모리 메트릭 수집
            collectJvmMemoryMetrics(metrics, collectedAt);
            
            // JVM 스레드 메트릭 수집
            collectJvmThreadMetrics(metrics, collectedAt);
            
            // GC 메트릭 수집
            collectGcMetrics(metrics, collectedAt);
            
            // 커스텀 메트릭 수집
            collectCustomMetrics(metrics, collectedAt);
            
            log.debug("Micrometer 애플리케이션 메트릭 수집 완료: 총 {}개 메트릭", metrics.size());
            return metrics;
            
        } catch (Exception e) {
            log.error("Micrometer 애플리케이션 메트릭 수집 실패", e);
            throw new BusinessException(MonitoringErrorCode.METRICS_COLLECTION_FAILED, 
                "Micrometer 애플리케이션 메트릭 수집 중 오류가 발생했습니다: " + e.getMessage());
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
     * JVM 메모리 메트릭 수집 (Micrometer 방식)
     */
    private void collectJvmMemoryMetrics(List<Metric> metrics, LocalDateTime collectedAt) {
        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            
            // Heap 메모리 정보
            long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryBean.getHeapMemoryUsage().getMax();
            long heapCommitted = memoryBean.getHeapMemoryUsage().getCommitted();
            
            // Micrometer Gauge 등록
            Gauge.builder("jvm.memory.heap.used", () -> heapUsed / (1024.0 * 1024.0))
                .description("JVM Heap 메모리 사용량")
                .register(meterRegistry);
                
            Gauge.builder("jvm.memory.heap.max", () -> heapMax / (1024.0 * 1024.0))
                .description("JVM Heap 메모리 최대 크기")
                .register(meterRegistry);
                
            Gauge.builder("jvm.memory.heap.committed", () -> heapCommitted / (1024.0 * 1024.0))
                .description("JVM Heap 메모리 커밋된 크기")
                .register(meterRegistry);
            
            // Heap 사용률
            if (heapMax > 0) {
                double heapUsage = (double) heapUsed / heapMax * 100.0;
                Gauge.builder("jvm.memory.heap.usage", () -> heapUsage)
                    .description("JVM Heap 메모리 사용률")
                    .register(meterRegistry);
            }
            
            // Non-Heap 메모리 정보
            long nonHeapUsed = memoryBean.getNonHeapMemoryUsage().getUsed();
            long nonHeapMax = memoryBean.getNonHeapMemoryUsage().getMax();
            long nonHeapCommitted = memoryBean.getNonHeapMemoryUsage().getCommitted();
            
            Gauge.builder("jvm.memory.nonheap.used", () -> nonHeapUsed / (1024.0 * 1024.0))
                .description("JVM Non-Heap 메모리 사용량")
                .register(meterRegistry);
                
            Gauge.builder("jvm.memory.nonheap.max", () -> nonHeapMax / (1024.0 * 1024.0))
                .description("JVM Non-Heap 메모리 최대 크기")
                .register(meterRegistry);
                
            Gauge.builder("jvm.memory.nonheap.committed", () -> nonHeapCommitted / (1024.0 * 1024.0))
                .description("JVM Non-Heap 메모리 커밋된 크기")
                .register(meterRegistry);
            
            // Metric 엔티티로도 저장
            metrics.add(createMetric("jvm.memory.heap.used", heapUsed / (1024.0 * 1024.0), "MB", collectedAt));
            metrics.add(createMetric("jvm.memory.heap.max", heapMax / (1024.0 * 1024.0), "MB", collectedAt));
            metrics.add(createMetric("jvm.memory.heap.committed", heapCommitted / (1024.0 * 1024.0), "MB", collectedAt));
            
            if (heapMax > 0) {
                double heapUsage = (double) heapUsed / heapMax * 100.0;
                metrics.add(createMetric("jvm.memory.heap.usage", heapUsage, "%", collectedAt));
            }
            
            metrics.add(createMetric("jvm.memory.nonheap.used", nonHeapUsed / (1024.0 * 1024.0), "MB", collectedAt));
            metrics.add(createMetric("jvm.memory.nonheap.max", nonHeapMax / (1024.0 * 1024.0), "MB", collectedAt));
            metrics.add(createMetric("jvm.memory.nonheap.committed", nonHeapCommitted / (1024.0 * 1024.0), "MB", collectedAt));
            
            log.debug("Micrometer JVM 메모리 메트릭 수집 완료: heapUsed={}MB, nonHeapUsed={}MB", 
                    heapUsed / (1024 * 1024), nonHeapUsed / (1024 * 1024));
                    
        } catch (Exception e) {
            log.warn("Micrometer JVM 메모리 메트릭 수집 실패", e);
        }
    }

    /**
     * JVM 스레드 메트릭 수집 (Micrometer 방식)
     */
    private void collectJvmThreadMetrics(List<Metric> metrics, LocalDateTime collectedAt) {
        try {
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            
            int threadCount = threadBean.getThreadCount();
            int peakThreadCount = threadBean.getPeakThreadCount();
            long totalStartedThreadCount = threadBean.getTotalStartedThreadCount();
            int daemonThreadCount = threadBean.getDaemonThreadCount();
            
            // Micrometer Gauge 등록
            Gauge.builder("jvm.threads.count", () -> threadCount)
                .description("JVM 스레드 수")
                .register(meterRegistry);
                
            Gauge.builder("jvm.threads.peak", () -> peakThreadCount)
                .description("JVM 최대 스레드 수")
                .register(meterRegistry);
                
            Gauge.builder("jvm.threads.total_started", () -> totalStartedThreadCount)
                .description("JVM 총 시작된 스레드 수")
                .register(meterRegistry);
                
            Gauge.builder("jvm.threads.daemon", () -> daemonThreadCount)
                .description("JVM 데몬 스레드 수")
                .register(meterRegistry);
            
            // Metric 엔티티로도 저장
            metrics.add(createMetric("jvm.threads.count", (double) threadCount, "count", collectedAt));
            metrics.add(createMetric("jvm.threads.peak", (double) peakThreadCount, "count", collectedAt));
            metrics.add(createMetric("jvm.threads.total_started", (double) totalStartedThreadCount, "count", collectedAt));
            metrics.add(createMetric("jvm.threads.daemon", (double) daemonThreadCount, "count", collectedAt));
            
            log.debug("Micrometer JVM 스레드 메트릭 수집 완료: threadCount={}, daemonCount={}", 
                    threadCount, daemonThreadCount);
                    
        } catch (Exception e) {
            log.warn("Micrometer JVM 스레드 메트릭 수집 실패", e);
        }
    }

    /**
     * GC 메트릭 수집 (Micrometer 방식)
     */
    private void collectGcMetrics(List<Metric> metrics, LocalDateTime collectedAt) {
        try {
            var gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
            
            for (var gcBean : gcBeans) {
                String gcName = gcBean.getName();
                long collectionCount = gcBean.getCollectionCount();
                long collectionTime = gcBean.getCollectionTime();
                
                // Micrometer Counter 등록
                Counter.builder("jvm.gc.collections")
                    .description("GC 컬렉션 횟수")
                    .tag("gc.name", gcName)
                    .register(meterRegistry)
                    .increment(collectionCount);
                
                // Micrometer Timer 등록
                Timer.builder("jvm.gc.collection.time")
                    .description("GC 컬렉션 시간")
                    .tag("gc.name", gcName)
                    .register(meterRegistry)
                    .record(collectionTime, TimeUnit.MILLISECONDS);
                
                // Metric 엔티티로도 저장
                metrics.add(createMetric("jvm.gc.collections." + gcName, (double) collectionCount, "count", collectedAt));
                metrics.add(createMetric("jvm.gc.collection.time." + gcName, (double) collectionTime, "ms", collectedAt));
            }
            
            log.debug("Micrometer GC 메트릭 수집 완료: {}개 GC 빈", gcBeans.size());
            
        } catch (Exception e) {
            log.warn("Micrometer GC 메트릭 수집 실패", e);
        }
    }

    /**
     * 커스텀 메트릭 수집 (Micrometer 방식)
     */
    private void collectCustomMetrics(List<Metric> metrics, LocalDateTime collectedAt) {
        try {
            // 애플리케이션 시작 시간
            long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
            Gauge.builder("application.uptime", () -> uptime / 1000.0)
                .description("애플리케이션 가동 시간")
                .register(meterRegistry); // 초 단위
            
            // 클래스 로딩 정보
            var classLoadingBean = ManagementFactory.getClassLoadingMXBean();
            int loadedClassCount = classLoadingBean.getLoadedClassCount();
            long totalLoadedClassCount = classLoadingBean.getTotalLoadedClassCount();
            long unloadedClassCount = classLoadingBean.getUnloadedClassCount();
            
            Gauge.builder("jvm.classes.loaded", () -> loadedClassCount)
                .description("로드된 클래스 수")
                .register(meterRegistry);
                
            Gauge.builder("jvm.classes.total_loaded", () -> totalLoadedClassCount)
                .description("총 로드된 클래스 수")
                .register(meterRegistry);
                
            Gauge.builder("jvm.classes.unloaded", () -> unloadedClassCount)
                .description("언로드된 클래스 수")
                .register(meterRegistry);
            
            // Metric 엔티티로도 저장
            metrics.add(createMetric("application.uptime", uptime / 1000.0, "seconds", collectedAt));
            metrics.add(createMetric("jvm.classes.loaded", (double) loadedClassCount, "count", collectedAt));
            metrics.add(createMetric("jvm.classes.total_loaded", (double) totalLoadedClassCount, "count", collectedAt));
            metrics.add(createMetric("jvm.classes.unloaded", (double) unloadedClassCount, "count", collectedAt));
            
            log.debug("Micrometer 커스텀 메트릭 수집 완료");
            
        } catch (Exception e) {
            log.warn("Micrometer 커스텀 메트릭 수집 실패", e);
        }
    }

    /**
     * Metric 엔티티 생성
     */
    private Metric createMetric(String name, Double value, String unit, LocalDateTime collectedAt) {
        return Metric.builder()
                .metricName(name)
                .metricValue(value)
                .unit(unit)
                .collectedAt(collectedAt)
                .metricType(Metric.MetricType.APPLICATION)
                .source("micrometer")
                .build();
    }
}