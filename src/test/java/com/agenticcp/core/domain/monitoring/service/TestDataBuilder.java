package com.agenticcp.core.domain.monitoring.service;

import com.agenticcp.core.domain.monitoring.dto.SystemMetrics;
import com.agenticcp.core.domain.monitoring.entity.Metric;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 모니터링 도메인 테스트 데이터 빌더
 * 테스트 가이드라인에 따라 재사용 가능한 테스트 데이터를 제공
 */
public class TestDataBuilder {

    /**
     * 기본 SystemMetrics 빌더
     */
    public static SystemMetrics.SystemMetricsBuilder systemMetricsBuilder() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("hostname", "test-host");
        metadata.put("os_name", "Windows 10");
        metadata.put("java_version", "17.0.1");

        SystemMetrics.SystemInfo systemInfo = SystemMetrics.SystemInfo.builder()
                .hostname("test-host")
                .osName("Windows 10")
                .osVersion("10.0")
                .javaVersion("17.0.1")
                .availableProcessors(8)
                .build();

        return SystemMetrics.builder()
                .cpuUsage(45.2)
                .memoryUsage(67.8)
                .memoryUsedMB(2048L)
                .memoryTotalMB(4096L)
                .diskUsage(23.1)
                .diskUsedGB(100L)
                .diskTotalGB(500L)
                .collectedAt(LocalDateTime.now())
                .metadata(metadata)
                .systemInfo(systemInfo);
    }

    /**
     * null 값이 포함된 SystemMetrics 빌더
     */
    public static SystemMetrics.SystemMetricsBuilder systemMetricsWithNullsBuilder() {
        return SystemMetrics.builder()
                .cpuUsage(null)  // null 값
                .memoryUsage(50.0)
                .memoryUsedMB(null)  // null 값
                .memoryTotalMB(4096L)
                .diskUsage(20.0)
                .diskUsedGB(100L)
                .diskTotalGB(null)  // null 값
                .collectedAt(LocalDateTime.now())
                .metadata(new HashMap<>())
                .systemInfo(SystemMetrics.SystemInfo.builder().build());
    }

    /**
     * 높은 리소스 사용률 SystemMetrics 빌더
     */
    public static SystemMetrics.SystemMetricsBuilder highResourceUsageSystemMetricsBuilder() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("hostname", "high-load-host");
        metadata.put("os_name", "Linux");
        metadata.put("java_version", "17.0.1");

        SystemMetrics.SystemInfo systemInfo = SystemMetrics.SystemInfo.builder()
                .hostname("high-load-host")
                .osName("Linux")
                .osVersion("20.04")
                .javaVersion("17.0.1")
                .availableProcessors(16)
                .build();

        return SystemMetrics.builder()
                .cpuUsage(95.5)
                .memoryUsage(89.2)
                .memoryUsedMB(8192L)
                .memoryTotalMB(9216L)
                .diskUsage(78.3)
                .diskUsedGB(400L)
                .diskTotalGB(512L)
                .collectedAt(LocalDateTime.now())
                .metadata(metadata)
                .systemInfo(systemInfo);
    }

    /**
     * 기본 SystemInfo 빌더
     */
    public static SystemMetrics.SystemInfo.SystemInfoBuilder systemInfoBuilder() {
        return SystemMetrics.SystemInfo.builder()
                .hostname("test-host")
                .osName("Windows 10")
                .osVersion("10.0")
                .javaVersion("17.0.1")
                .availableProcessors(8);
    }

    /**
     * 기본 메타데이터 생성
     */
    public static Map<String, Object> defaultMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("hostname", "test-host");
        metadata.put("os_name", "Windows 10");
        metadata.put("java_version", "17.0.1");
        metadata.put("jvm_vendor", "Eclipse Adoptium");
        metadata.put("jvm_version", "17.0.1+12");
        return metadata;
    }

    /**
     * 커스텀 메타데이터 생성
     */
    public static Map<String, Object> customMetadata(String hostname, String osName, String javaVersion) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("hostname", hostname);
        metadata.put("os_name", osName);
        metadata.put("java_version", javaVersion);
        return metadata;
    }

    /**
     * Metric 엔티티 Mock 생성 헬퍼
     */
    public static Metric createMockMetric(String metricName, Double value, String unit, Metric.MetricType type) {
        Metric mockMetric = org.mockito.Mockito.mock(Metric.class);
        org.mockito.Mockito.when(mockMetric.getMetricName()).thenReturn(metricName);
        org.mockito.Mockito.when(mockMetric.getMetricValue()).thenReturn(value);
        org.mockito.Mockito.when(mockMetric.getUnit()).thenReturn(unit);
        org.mockito.Mockito.when(mockMetric.getMetricType()).thenReturn(type);
        return mockMetric;
    }

    /**
     * CPU 메트릭 Mock 생성
     */
    public static Metric createCpuUsageMetric() {
        return createMockMetric("cpu.usage", 45.2, "%", Metric.MetricType.SYSTEM);
    }

    /**
     * 메모리 사용률 메트릭 Mock 생성
     */
    public static Metric createMemoryUsageMetric() {
        return createMockMetric("memory.usage", 67.8, "%", Metric.MetricType.SYSTEM);
    }

    /**
     * 메모리 사용량 메트릭 Mock 생성
     */
    public static Metric createMemoryUsedMetric() {
        return createMockMetric("memory.used", 2048.0, "MB", Metric.MetricType.SYSTEM);
    }

    /**
     * 메모리 총량 메트릭 Mock 생성
     */
    public static Metric createMemoryTotalMetric() {
        return createMockMetric("memory.total", 4096.0, "MB", Metric.MetricType.SYSTEM);
    }

    /**
     * 디스크 사용률 메트릭 Mock 생성
     */
    public static Metric createDiskUsageMetric() {
        return createMockMetric("disk.usage", 23.1, "%", Metric.MetricType.SYSTEM);
    }

    /**
     * 디스크 사용량 메트릭 Mock 생성
     */
    public static Metric createDiskUsedMetric() {
        return createMockMetric("disk.used", 100.0, "GB", Metric.MetricType.SYSTEM);
    }

    /**
     * 디스크 총량 메트릭 Mock 생성
     */
    public static Metric createDiskTotalMetric() {
        return createMockMetric("disk.total", 500.0, "GB", Metric.MetricType.SYSTEM);
    }

    /**
     * 애플리케이션 메트릭 생성 (ApplicationMetricsCollectorTest용)
     */
    public static Metric createMetric(String name, Double value, String unit) {
        return Metric.builder()
                .metricName(name)
                .metricValue(value)
                .unit(unit)
                .collectedAt(LocalDateTime.now())
                .metricType(Metric.MetricType.APPLICATION)
                .build();
    }
    
    /**
     * 애플리케이션용 SystemMetrics 생성
     */
    public static SystemMetrics createSystemMetrics() {
        return SystemMetrics.builder()
                .collectedAt(LocalDateTime.now())
                .systemInfo(SystemMetrics.SystemInfo.builder()
                        .hostname("test-host")
                        .osName("Test OS")
                        .javaVersion("11.0.0")
                        .availableProcessors(4)
                        .build())
                .metadata(Map.of("collector_type", "APPLICATION"))
                .build();
    }
}
