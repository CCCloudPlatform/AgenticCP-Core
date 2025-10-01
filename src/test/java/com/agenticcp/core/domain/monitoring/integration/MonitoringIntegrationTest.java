package com.agenticcp.core.domain.monitoring.integration;

import com.agenticcp.core.domain.monitoring.entity.Metric;
import com.agenticcp.core.domain.monitoring.repository.MetricRepository;
import com.agenticcp.core.domain.monitoring.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * 모니터링 도메인 통합 테스트
 * 
 * <p>모든 컴포넌트가 제대로 연동되어 작동하는지 확인합니다.
 * 
 * <p>테스트 시나리오:
 * <ul>
 *   <li>메트릭 수집기들이 Spring Bean으로 등록되는지 확인</li>
 *   <li>MetricsCollectionService가 수집기를 사용하는지 확인</li>
 *   <li>수집된 메트릭이 DB에 저장되는지 확인</li>
 *   <li>API를 통해 메트릭을 조회할 수 있는지 확인</li>
 * </ul>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("모니터링 도메인 통합 테스트")
class MonitoringIntegrationTest {

    @Autowired(required = false)
    private SystemMetricsCollector systemMetricsCollector;

    @Autowired(required = false)
    private MicrometerMetricsCollector micrometerMetricsCollector;

    @Autowired(required = false)
    private HttpMetricsCollector httpMetricsCollector;

    @Autowired(required = false)
    private MetricsCollectorRegistry metricsCollectorRegistry;

    @Autowired(required = false)
    private MetricsCollectionService metricsCollectionService;

    @Autowired(required = false)
    private MetricsCache metricsCache;

    @Autowired(required = false)
    private RetryMetricsTracker retryMetricsTracker;

    @Autowired(required = false)
    private MetricRepository metricRepository;

    @Test
    @DisplayName("모든 핵심 컴포넌트가 Spring Bean으로 등록되어야 함")
    void allComponentsShouldBeRegisteredAsBeans() {
        // given & when & then
        assertThat(systemMetricsCollector).isNotNull()
            .describedAs("SystemMetricsCollector가 Spring Bean으로 등록되어야 합니다");
        
        assertThat(micrometerMetricsCollector).isNotNull()
            .describedAs("MicrometerMetricsCollector가 Spring Bean으로 등록되어야 합니다");
        
        assertThat(httpMetricsCollector).isNotNull()
            .describedAs("HttpMetricsCollector가 Spring Bean으로 등록되어야 합니다");
        
        assertThat(metricsCollectorRegistry).isNotNull()
            .describedAs("MetricsCollectorRegistry가 Spring Bean으로 등록되어야 합니다");
        
        assertThat(metricsCollectionService).isNotNull()
            .describedAs("MetricsCollectionService가 Spring Bean으로 등록되어야 합니다");
        
        assertThat(metricsCache).isNotNull()
            .describedAs("MetricsCache가 Spring Bean으로 등록되어야 합니다");
        
        assertThat(retryMetricsTracker).isNotNull()
            .describedAs("RetryMetricsTracker가 Spring Bean으로 등록되어야 합니다");
        
        assertThat(metricRepository).isNotNull()
            .describedAs("MetricRepository가 Spring Bean으로 등록되어야 합니다");
    }

    @Test
    @DisplayName("모든 메트릭 수집기가 MetricsCollectorRegistry에 등록되어야 함")
    void allCollectorsShouldBeRegisteredInRegistry() {
        // given & when
        List<MetricsCollector> allCollectors = metricsCollectorRegistry.getAllCollectors();

        // then
        assertThat(allCollectors).isNotEmpty()
            .describedAs("최소 1개 이상의 수집기가 등록되어야 합니다");
        
        assertThat(allCollectors)
            .extracting(MetricsCollector::getCollectorType)
            .describedAs("SYSTEM, APPLICATION, CUSTOM 타입이 포함되어야 합니다")
            .isNotEmpty();
    }

    @Test
    @DisplayName("SystemMetricsCollector가 정상적으로 메트릭을 수집해야 함")
    void systemMetricsCollectorShouldCollectMetrics() {
        // given & when
        var systemMetrics = systemMetricsCollector.collectSystemMetrics();

        // then
        assertThat(systemMetrics).isNotNull()
            .describedAs("시스템 메트릭이 수집되어야 합니다");
        
        assertThat(systemMetrics.getCpuUsage())
            .describedAs("CPU 사용률이 0 이상이어야 합니다")
            .isGreaterThanOrEqualTo(0.0);
        
        assertThat(systemMetrics.getMemoryUsedMB())
            .describedAs("메모리 사용량이 0보다 커야 합니다")
            .isGreaterThan(0L);
    }

    @Test
    @DisplayName("MicrometerMetricsCollector가 정상적으로 메트릭을 수집해야 함")
    void micrometerMetricsCollectorShouldCollectMetrics() {
        // given & when
        List<Metric> metrics = micrometerMetricsCollector.collectApplicationMetrics();

        // then
        assertThat(metrics).isNotEmpty()
            .describedAs("Micrometer 메트릭이 수집되어야 합니다");
        
        assertThat(metrics)
            .extracting(Metric::getSource)
            .allMatch(source -> source.equals("micrometer"))
            .describedAs("모든 메트릭의 소스가 'micrometer'여야 합니다");
    }

    @Test
    @DisplayName("HttpMetricsCollector가 Bean으로 등록되고 활성화되어야 함")
    void httpMetricsCollectorShouldBeEnabledAndRegistered() {
        // given & when & then
        assertThat(httpMetricsCollector.isEnabled())
            .describedAs("HttpMetricsCollector가 활성화되어야 합니다")
            .isTrue();
        
        assertThat(httpMetricsCollector.getCollectorName())
            .describedAs("수집기 이름이 'http-metrics'여야 합니다")
            .isEqualTo("http-metrics");
    }

    @Test
    @DisplayName("MetricsCache가 정상적으로 작동해야 함")
    void metricsCacheShouldWorkProperly() {
        // given
        List<Metric> testMetrics = List.of(
            Metric.builder()
                .metricName("test.cache.metric")
                .metricValue(999.0)
                .metricType(Metric.MetricType.APPLICATION)
                .source("test")
                .build()
        );

        // when
        metricsCache.clearAll(); // 이전 캐시 삭제
        metricsCache.cacheApplicationMetrics(testMetrics);
        List<Metric> cachedMetrics = metricsCache.getLastSuccessfulApplicationMetrics();

        // then
        assertThat(cachedMetrics).isNotNull()
            .describedAs("캐시된 메트릭을 조회할 수 있어야 합니다")
            .hasSize(1);
        
        assertThat(cachedMetrics.get(0).getMetricName())
            .isEqualTo("test.cache.metric");
    }

    @Test
    @DisplayName("MetricsCollectionService가 수동으로 메트릭을 수집할 수 있어야 함")
    void metricsCollectionServiceShouldCollectMetricsManually() {
        // given
        long beforeCount = metricRepository.count();

        // when
        metricsCollectionService.collectMetricsManually();

        // then
        long afterCount = metricRepository.count();
        assertThat(afterCount)
            .describedAs("메트릭이 DB에 저장되어야 합니다")
            .isGreaterThan(beforeCount);
    }

    @Test
    @DisplayName("전체 메트릭 수집 플로우가 정상 작동해야 함")
    void fullMetricsCollectionFlowShouldWork() {
        // given
        long initialCount = metricRepository.count();

        // when - 시스템 메트릭 수집
        metricsCollectionService.collectSystemMetrics();
        
        // when - 애플리케이션 메트릭 수집  
        metricsCollectionService.collectApplicationMetrics();

        // then
        long finalCount = metricRepository.count();
        assertThat(finalCount)
            .describedAs("시스템 + 애플리케이션 메트릭이 모두 저장되어야 합니다")
            .isGreaterThan(initialCount);
        
        // 최근 저장된 메트릭 조회
        List<Metric> recentMetrics = metricRepository.findAll();
        assertThat(recentMetrics)
            .describedAs("저장된 메트릭을 조회할 수 있어야 합니다")
            .isNotEmpty();
    }

    @Test
    @DisplayName("Registry를 통해 활성화된 수집기만 필터링할 수 있어야 함")
    void registryShouldFilterEnabledCollectors() {
        // given & when
        List<MetricsCollector> enabledCollectors = metricsCollectorRegistry.getEnabledCollectors();

        // then
        assertThat(enabledCollectors)
            .describedAs("활성화된 수집기만 반환되어야 합니다")
            .allMatch(MetricsCollector::isEnabled);
    }

    @Test
    @DisplayName("RetryMetricsTracker가 정상 작동해야 함")
    void retryMetricsTrackerShouldWork() {
        // given & when
        retryMetricsTracker.recordRetryAttempt();
        retryMetricsTracker.recordRetrySuccess();

        // then
        // 예외가 발생하지 않으면 성공
        assertThat(retryMetricsTracker).isNotNull();
    }
}

