package com.agenticcp.core.domain.monitoring.storage;

import com.agenticcp.core.domain.monitoring.entity.Metric;
import com.agenticcp.core.domain.monitoring.entity.Metric.MetricType;
import com.agenticcp.core.domain.monitoring.enums.StorageType;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 테스트 데이터 빌더
 * 
 * <p>메트릭 저장소 테스트를 위한 테스트 데이터를 생성하는 빌더 클래스입니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public class TestDataBuilder {
    
    /**
     * 메트릭 빌더 생성
     */
    public static Metric.MetricBuilder metricBuilder() {
        return Metric.builder()
                .metricName("test.metric")
                .metricValue(100.0)
                .unit("count")
                .metricType(MetricType.SYSTEM)
                .collectedAt(LocalDateTime.now())
                .source("test");
    }
    
    /**
     * CPU 메트릭 생성
     */
    public static Metric cpuMetric() {
        return metricBuilder()
                .metricName("cpu.usage")
                .metricValue(75.5)
                .unit("%")
                .build();
    }
    
    /**
     * 메모리 메트릭 생성
     */
    public static Metric memoryMetric() {
        return metricBuilder()
                .metricName("memory.usage")
                .metricValue(1024.0)
                .unit("MB")
                .build();
    }
    
    /**
     * 디스크 메트릭 생성
     */
    public static Metric diskMetric() {
        return metricBuilder()
                .metricName("disk.usage")
                .metricValue(50.0)
                .unit("%")
                .build();
    }
    
    /**
     * 테스트용 메트릭 목록 생성
     */
    public static List<Metric> testMetrics() {
        return Arrays.asList(cpuMetric(), memoryMetric(), diskMetric());
    }
    
    /**
     * InfluxDB 저장소 설정 생성
     */
    public static MetricsStorageFactory.StorageConfig influxDBConfig() {
        return MetricsStorageFactory.StorageConfig.builder()
                .enabled(true)
                .url("http://localhost:8086")
                .username("admin")
                .password("admin")
                .database("test_metrics")
                .timeout(30000)
                .retryCount(3)
                .build();
    }
    
    /**
     * TimescaleDB 저장소 설정 생성
     */
    public static MetricsStorageFactory.StorageConfig timescaleDBConfig() {
        return MetricsStorageFactory.StorageConfig.builder()
                .enabled(false)
                .url("jdbc:postgresql://localhost:5432/test_metrics")
                .username("postgres")
                .password("postgres")
                .database("test_metrics")
                .timeout(30000)
                .retryCount(3)
                .build();
    }
    
    /**
     * Prometheus 저장소 설정 생성
     */
    public static MetricsStorageFactory.StorageConfig prometheusConfig() {
        return MetricsStorageFactory.StorageConfig.builder()
                .enabled(false)
                .url("http://localhost:9090")
                .username("")
                .password("")
                .database("test_metrics")
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
}
