package com.agenticcp.core.domain.monitoring;

import com.agenticcp.core.domain.monitoring.dto.SystemMetrics;
import com.agenticcp.core.domain.monitoring.entity.Metric;
import com.agenticcp.core.domain.monitoring.entity.Metric.MetricType;
import com.agenticcp.core.domain.monitoring.enums.StorageType;
import com.agenticcp.core.domain.monitoring.storage.MetricsStorageFactory;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 모니터링 도메인 통합 테스트 데이터 빌더
 * <p>
 * 모니터링 도메인의 모든 테스트에서 사용할 수 있는 통합 테스트 데이터 빌더입니다.
 * Service, Storage, Factory 등 모든 모니터링 관련 테스트에서 재사용 가능합니다.
 * </p>
 *
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public class TestDataBuilder {

    // ==================== SystemMetrics 관련 ====================

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
                .systemInfo(systemInfo)
                .metadata(metadata)
                .collectedAt(LocalDateTime.now())
                .cpuUsage(45.2)
                .memoryUsage(67.8)
                .memoryUsedMB(2048L)
                .memoryTotalMB(4096L)
                .diskUsage(23.1)
                .diskUsedGB(100L)
                .diskTotalGB(500L);
    }

    /**
     * 기본 SystemMetrics 생성
     */
    public static SystemMetrics systemMetrics() {
        return systemMetricsBuilder().build();
    }

    /**
     * CPU 사용률이 높은 SystemMetrics 생성
     */
    public static SystemMetrics highCpuSystemMetrics() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("hostname", "high-cpu-host");
        metadata.put("os_name", "Linux");
        metadata.put("java_version", "17.0.1");

        SystemMetrics.SystemInfo systemInfo = SystemMetrics.SystemInfo.builder()
                .hostname("high-cpu-host")
                .osName("Linux")
                .osVersion("5.4.0")
                .javaVersion("17.0.1")
                .availableProcessors(16)
                .build();

        return SystemMetrics.builder()
                .systemInfo(systemInfo)
                .metadata(metadata)
                .collectedAt(LocalDateTime.now())
                .cpuUsage(85.5)
                .memoryUsage(90.2)
                .memoryUsedMB(8192L)
                .memoryTotalMB(16384L)
                .diskUsage(75.8)
                .diskUsedGB(400L)
                .diskTotalGB(1000L)
                .build();
    }

    // ==================== Metric 관련 ====================

    /**
     * 메트릭 빌더 생성 (기본값: SYSTEM)
     */
    public static Metric.MetricBuilder metricBuilder() {
        return Metric.builder()
                .metricName("test.metric")
                .metricValue(100.0)
                .unit("count")
                .metricType(MetricType.SYSTEM)
                .collectedAt(LocalDateTime.now())
                .source("test-source");
    }

    /**
     * 애플리케이션 메트릭 빌더 생성
     */
    public static Metric.MetricBuilder applicationMetricBuilder() {
        return Metric.builder()
                .metricName("test.metric")
                .metricValue(100.0)
                .unit("count")
                .metricType(MetricType.APPLICATION)
                .collectedAt(LocalDateTime.now())
                .source("test-source");
    }

    /**
     * 기본 메트릭 생성
     */
    public static Metric metric() {
        return metricBuilder().build();
    }

    /**
     * 테스트용 메트릭 목록 생성
     */
    public static List<Metric> testMetrics() {
        return Arrays.asList(
                Metric.builder()
                        .metricName("cpu.usage")
                        .metricValue(50.5)
                        .unit("%")
                        .metricType(MetricType.SYSTEM)
                        .collectedAt(LocalDateTime.now())
                        .source("test-system")
                        .build(),
                Metric.builder()
                        .metricName("memory.used")
                        .metricValue(1024.0)
                        .unit("MB")
                        .metricType(MetricType.SYSTEM)
                        .collectedAt(LocalDateTime.now())
                        .source("test-system")
                        .build(),
                Metric.builder()
                        .metricName("app.response.time")
                        .metricValue(150.0)
                        .unit("ms")
                        .metricType(MetricType.APPLICATION)
                        .collectedAt(LocalDateTime.now())
                        .source("test-application")
                        .build()
        );
    }

    /**
     * CPU 메트릭 생성
     */
    public static Metric cpuMetric() {
        return metricBuilder()
                .metricName("cpu.usage")
                .metricValue(75.5)
                .unit("%")
                .metricType(MetricType.SYSTEM)
                .build();
    }

    /**
     * 메모리 메트릭 생성
     */
    public static Metric memoryMetric() {
        return metricBuilder()
                .metricName("memory.usage")
                .metricValue(2048.0)
                .unit("MB")
                .metricType(MetricType.SYSTEM)
                .build();
    }

    /**
     * 애플리케이션 메트릭 생성
     */
    public static Metric applicationMetric() {
        return metricBuilder()
                .metricName("app.requests")
                .metricValue(1000.0)
                .unit("count")
                .metricType(MetricType.APPLICATION)
                .build();
    }

    // ==================== StorageConfig 관련 ====================

    /**
     * InfluxDB용 StorageConfig 생성
     */
    public static MetricsStorageFactory.StorageConfig influxDBConfig() {
        return MetricsStorageFactory.StorageConfig.builder()
                .enabled(true)
                .url("http://localhost:8086")
                .database("test_metrics")
                .username("test_admin")
                .password("test_password")
                .timeout(30000)
                .retryCount(3)
                .build();
    }

    /**
     * TimescaleDB용 StorageConfig 생성
     */
    public static MetricsStorageFactory.StorageConfig timescaleDBConfig() {
        return MetricsStorageFactory.StorageConfig.builder()
                .enabled(true)
                .url("jdbc:postgresql://localhost:5432/test_metrics")
                .database("test_metrics")
                .username("test_postgres")
                .password("test_password")
                .timeout(30000)
                .retryCount(3)
                .build();
    }

    /**
     * Prometheus용 StorageConfig 생성
     */
    public static MetricsStorageFactory.StorageConfig prometheusConfig() {
        return MetricsStorageFactory.StorageConfig.builder()
                .enabled(true)
                .url("http://localhost:9090")
                .pushgateway("http://localhost:9091")
                .timeout(30000)
                .retryCount(3)
                .build();
    }

    /**
     * 특정 저장소 타입의 설정 생성
     */
    public static MetricsStorageFactory.StorageConfig configFor(StorageType type) {
        return switch (type) {
            case INFLUXDB -> influxDBConfig();
            case TIMESCALEDB -> timescaleDBConfig();
            case PROMETHEUS -> prometheusConfig();
        };
    }

    /**
     * 비활성화된 StorageConfig 생성
     */
    public static MetricsStorageFactory.StorageConfig disabledConfig() {
        return MetricsStorageFactory.StorageConfig.builder()
                .enabled(false)
                .url("http://localhost:9999")
                .database("disabled_db")
                .username("disabled_user")
                .password("disabled_pass")
                .timeout(1000)
                .retryCount(1)
                .build();
    }
}
