package com.agenticcp.core.domain.monitoring.service;

import com.agenticcp.core.domain.monitoring.entity.Metric;
import com.agenticcp.core.domain.monitoring.repository.MetricRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * MetricTrendService 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-22
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MetricTrendService 테스트")
class MetricTrendServiceTest {

    @Mock
    private MetricRepository metricRepository;

    @InjectMocks
    private MetricTrendService metricTrendService;

    private String testTenantId;
    private String testMetricName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<Metric> sampleMetrics;

    @BeforeEach
    void setUp() {
        testTenantId = "tenant-001";
        testMetricName = "cpu.usage";
        startTime = LocalDateTime.now().minusHours(24);
        endTime = LocalDateTime.now();
        sampleMetrics = createSampleMetrics();
    }

    @Nested
    @DisplayName("메트릭 트렌드 조회 테스트")
    class GetMetricsWithTrendTest {

        @Test
        @DisplayName("메트릭 트렌드 조회 성공 - 1시간 간격")
        void getMetricsWithTrend_Success_1Hour() {
            // Given
            String interval = "1h";
            when(metricRepository.findByMetricNameAndTimeRange(testMetricName, testTenantId, startTime, endTime))
                    .thenReturn(sampleMetrics);

            // When
            List<Metric> result = metricTrendService.getMetricsWithTrend(
                    testTenantId, testMetricName, startTime, endTime, interval);

            // Then
            assertThat(result).isNotEmpty();
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getMetricName()).isEqualTo(testMetricName);
            assertThat(result.get(0).getMetricValue()).isEqualTo(75.5);

            verify(metricRepository).findByMetricNameAndTimeRange(testMetricName, testTenantId, startTime, endTime);
        }

        @Test
        @DisplayName("메트릭 트렌드 조회 성공 - 5분 간격")
        void getMetricsWithTrend_Success_5Minutes() {
            // Given
            String interval = "5m";
            when(metricRepository.findByMetricNameAndTimeRange(testMetricName, testTenantId, startTime, endTime))
                    .thenReturn(sampleMetrics);

            // When
            List<Metric> result = metricTrendService.getMetricsWithTrend(
                    testTenantId, testMetricName, startTime, endTime, interval);

            // Then
            assertThat(result).isNotEmpty();
            verify(metricRepository).findByMetricNameAndTimeRange(testMetricName, testTenantId, startTime, endTime);
        }

        @Test
        @DisplayName("메트릭 트렌드 조회 성공 - 1일 간격")
        void getMetricsWithTrend_Success_1Day() {
            // Given
            String interval = "1d";
            when(metricRepository.findByMetricNameAndTimeRange(testMetricName, testTenantId, startTime, endTime))
                    .thenReturn(sampleMetrics);

            // When
            List<Metric> result = metricTrendService.getMetricsWithTrend(
                    testTenantId, testMetricName, startTime, endTime, interval);

            // Then
            assertThat(result).isNotEmpty();
            verify(metricRepository).findByMetricNameAndTimeRange(testMetricName, testTenantId, startTime, endTime);
        }

        @Test
        @DisplayName("메트릭 트렌드 조회 - 빈 결과")
        void getMetricsWithTrend_EmptyResult() {
            // Given
            String interval = "1h";
            when(metricRepository.findByMetricNameAndTimeRange(testMetricName, testTenantId, startTime, endTime))
                    .thenReturn(Arrays.asList());

            // When
            List<Metric> result = metricTrendService.getMetricsWithTrend(
                    testTenantId, testMetricName, startTime, endTime, interval);

            // Then
            assertThat(result).isEmpty();
            verify(metricRepository).findByMetricNameAndTimeRange(testMetricName, testTenantId, startTime, endTime);
        }

        @Test
        @DisplayName("메트릭 트렌드 조회 - 잘못된 간격")
        void getMetricsWithTrend_InvalidInterval() {
            // Given
            String interval = "invalid-interval";
            when(metricRepository.findByMetricNameAndTimeRange(testMetricName, testTenantId, startTime, endTime))
                    .thenReturn(sampleMetrics);

            // When
            List<Metric> result = metricTrendService.getMetricsWithTrend(
                    testTenantId, testMetricName, startTime, endTime, interval);

            // Then
            assertThat(result).isNotEmpty();
            // 잘못된 간격의 경우 기본값(1시간)으로 처리됨
            verify(metricRepository).findByMetricNameAndTimeRange(testMetricName, testTenantId, startTime, endTime);
        }
    }

    @Nested
    @DisplayName("시간 간격별 집계 테스트")
    class TimeIntervalAggregationTest {

        @Test
        @DisplayName("1분 간격 집계")
        void aggregateMetrics_1Minute() {
            // Given
            List<Metric> metrics = createMetricsForAggregation();
            when(metricRepository.findByMetricNameAndTimeRange(anyString(), anyString(), any(), any()))
                    .thenReturn(metrics);

            // When
            List<Metric> result = metricTrendService.getMetricsWithTrend(
                    testTenantId, testMetricName, startTime, endTime, "1m");

            // Then
            assertThat(result).isNotEmpty();
            // 시간순으로 정렬되어야 함
            assertThat(result.get(0).getCollectedAt()).isBeforeOrEqualTo(result.get(1).getCollectedAt());
        }

        @Test
        @DisplayName("5분 간격 집계")
        void aggregateMetrics_5Minutes() {
            // Given
            List<Metric> metrics = createMetricsForAggregation();
            when(metricRepository.findByMetricNameAndTimeRange(anyString(), anyString(), any(), any()))
                    .thenReturn(metrics);

            // When
            List<Metric> result = metricTrendService.getMetricsWithTrend(
                    testTenantId, testMetricName, startTime, endTime, "5m");

            // Then
            assertThat(result).isNotEmpty();
        }

        @Test
        @DisplayName("1시간 간격 집계")
        void aggregateMetrics_1Hour() {
            // Given
            List<Metric> metrics = createMetricsForAggregation();
            when(metricRepository.findByMetricNameAndTimeRange(anyString(), anyString(), any(), any()))
                    .thenReturn(metrics);

            // When
            List<Metric> result = metricTrendService.getMetricsWithTrend(
                    testTenantId, testMetricName, startTime, endTime, "1h");

            // Then
            assertThat(result).isNotEmpty();
        }

        @Test
        @DisplayName("1일 간격 집계")
        void aggregateMetrics_1Day() {
            // Given
            List<Metric> metrics = createMetricsForAggregation();
            when(metricRepository.findByMetricNameAndTimeRange(anyString(), anyString(), any(), any()))
                    .thenReturn(metrics);

            // When
            List<Metric> result = metricTrendService.getMetricsWithTrend(
                    testTenantId, testMetricName, startTime, endTime, "1d");

            // Then
            assertThat(result).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("트렌드 분석 알고리즘 테스트")
    class TrendAnalysisTest {

        @Test
        @DisplayName("평균값 계산 테스트")
        void calculateAverageValue() {
            // Given
            List<Metric> metricsWithSameTime = Arrays.asList(
                    createMetric("cpu.usage", 70.0, LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.HOURS)),
                    createMetric("cpu.usage", 80.0, LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.HOURS)),
                    createMetric("cpu.usage", 90.0, LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.HOURS))
            );
            when(metricRepository.findByMetricNameAndTimeRange(anyString(), anyString(), any(), any()))
                    .thenReturn(metricsWithSameTime);

            // When
            List<Metric> result = metricTrendService.getMetricsWithTrend(
                    testTenantId, testMetricName, startTime, endTime, "1h");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getMetricValue()).isEqualTo(80.0); // (70 + 80 + 90) / 3
        }

        @Test
        @DisplayName("시간순 정렬 테스트")
        void sortByTime() {
            // Given
            List<Metric> unsortedMetrics = Arrays.asList(
                    createMetric("cpu.usage", 70.0, LocalDateTime.now().minusHours(2)),
                    createMetric("cpu.usage", 80.0, LocalDateTime.now().minusHours(1)),
                    createMetric("cpu.usage", 90.0, LocalDateTime.now())
            );
            when(metricRepository.findByMetricNameAndTimeRange(anyString(), anyString(), any(), any()))
                    .thenReturn(unsortedMetrics);

            // When
            List<Metric> result = metricTrendService.getMetricsWithTrend(
                    testTenantId, testMetricName, startTime, endTime, "1h");

            // Then
            assertThat(result).hasSize(3);
            // 시간순으로 정렬되어야 함
            for (int i = 1; i < result.size(); i++) {
                assertThat(result.get(i-1).getCollectedAt()).isBeforeOrEqualTo(result.get(i).getCollectedAt());
            }
        }

        @Test
        @DisplayName("메타데이터 보존 테스트")
        void preserveMetadata() {
            // Given
            List<Metric> metrics = createSampleMetrics();
            when(metricRepository.findByMetricNameAndTimeRange(anyString(), anyString(), any(), any()))
                    .thenReturn(metrics);

            // When
            List<Metric> result = metricTrendService.getMetricsWithTrend(
                    testTenantId, testMetricName, startTime, endTime, "1h");

            // Then
            assertThat(result).isNotEmpty();
            Metric firstResult = result.get(0);
            assertThat(firstResult.getMetricName()).isEqualTo(testMetricName);
            assertThat(firstResult.getUnit()).isEqualTo("percent");
            assertThat(firstResult.getMetricType()).isEqualTo(Metric.MetricType.SYSTEM);
            assertThat(firstResult.getTenantId()).isEqualTo(testTenantId);
        }
    }

    // === Helper Methods ===

    private List<Metric> createSampleMetrics() {
        Metric metric1 = Metric.builder()
                .metricName(testMetricName)
                .metricValue(75.5)
                .unit("percent")
                .metricType(Metric.MetricType.SYSTEM)
                .status(Metric.Status.ACTIVE)
                .collectedAt(LocalDateTime.now().minusHours(2))
                .source("system")
                .tenantId(testTenantId)
                .build();

        Metric metric2 = Metric.builder()
                .metricName(testMetricName)
                .metricValue(80.2)
                .unit("percent")
                .metricType(Metric.MetricType.SYSTEM)
                .status(Metric.Status.ACTIVE)
                .collectedAt(LocalDateTime.now().minusHours(1))
                .source("system")
                .tenantId(testTenantId)
                .build();

        return Arrays.asList(metric1, metric2);
    }

    private List<Metric> createMetricsForAggregation() {
        LocalDateTime baseTime = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.HOURS);
        
        Metric metric1 = createMetric("cpu.usage", 70.0, baseTime);
        Metric metric2 = createMetric("cpu.usage", 80.0, baseTime.plusMinutes(30));
        Metric metric3 = createMetric("cpu.usage", 90.0, baseTime.plusHours(1));
        Metric metric4 = createMetric("cpu.usage", 85.0, baseTime.plusHours(1).plusMinutes(30));

        return Arrays.asList(metric1, metric2, metric3, metric4);
    }

    private Metric createMetric(String metricName, double value, LocalDateTime timestamp) {
        return Metric.builder()
                .metricName(metricName)
                .metricValue(value)
                .unit("percent")
                .metricType(Metric.MetricType.SYSTEM)
                .status(Metric.Status.ACTIVE)
                .collectedAt(timestamp)
                .source("system")
                .tenantId(testTenantId)
                .build();
    }
}
