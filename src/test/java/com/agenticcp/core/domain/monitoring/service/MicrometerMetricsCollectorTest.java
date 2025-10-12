package com.agenticcp.core.domain.monitoring.service;

import com.agenticcp.core.domain.monitoring.dto.SystemMetrics;
import com.agenticcp.core.domain.monitoring.entity.Metric;
import com.agenticcp.core.domain.monitoring.enums.CollectorType;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * MicrometerMetricsCollector 단위 테스트
 * 
 * @author AgenticCP
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MicrometerMetricsCollector 테스트")
class MicrometerMetricsCollectorTest {

    private MicrometerMetricsCollector micrometerMetricsCollector;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        micrometerMetricsCollector = new MicrometerMetricsCollector(meterRegistry);
        ReflectionTestUtils.setField(micrometerMetricsCollector, "enabled", true);
    }

    @Nested
    @DisplayName("기본 기능 테스트")
    class BasicFunctionalityTest {

        @Test
        @DisplayName("수집기 타입이 APPLICATION을 반환함")
        void getCollectorType_ReturnsApplication() {
            // When
            CollectorType type = micrometerMetricsCollector.getCollectorType();

            // Then
            assertThat(type).isEqualTo(CollectorType.APPLICATION);
        }

        @Test
        @DisplayName("수집기가 활성화되어 있음")
        void isEnabled_ReturnsTrue() {
            // When
            boolean enabled = micrometerMetricsCollector.isEnabled();

            // Then
            assertThat(enabled).isTrue();
        }

        @Test
        @DisplayName("수집기가 비활성화되어 있음")
        void isEnabled_WhenDisabled_ReturnsFalse() {
            // Given
            ReflectionTestUtils.setField(micrometerMetricsCollector, "enabled", false);

            // When
            boolean enabled = micrometerMetricsCollector.isEnabled();

            // Then
            assertThat(enabled).isFalse();
        }
    }

    @Nested
    @DisplayName("시스템 메트릭 수집 테스트")
    class SystemMetricsCollectionTest {

        @Test
        @DisplayName("시스템 메트릭 수집 성공")
        void collectSystemMetrics_Success() {
            // When
            SystemMetrics metrics = micrometerMetricsCollector.collectSystemMetrics();

            // Then
            assertThat(metrics).isNotNull();
            assertThat(metrics.getMemoryUsage()).isNotNull();
            assertThat(metrics.getMemoryUsedMB()).isNotNull();
            assertThat(metrics.getMemoryTotalMB()).isNotNull();
            assertThat(metrics.getCollectedAt()).isNotNull();
            assertThat(metrics.getSystemInfo().getJavaVersion()).isNotNull();
            assertThat(metrics.getSystemInfo().getHostname()).isNotNull();
            assertThat(metrics.getMetadata()).containsKey("micrometer.version");
            assertThat(metrics.getMetadata()).containsKey("registry.type");
        }

        @Test
        @DisplayName("수집기 비활성화 시 null 반환")
        void collectSystemMetrics_WhenDisabled_ReturnsNull() {
            // Given
            ReflectionTestUtils.setField(micrometerMetricsCollector, "enabled", false);

            // When
            SystemMetrics metrics = micrometerMetricsCollector.collectSystemMetrics();

            // Then
            assertThat(metrics).isNull();
        }
    }

    @Nested
    @DisplayName("애플리케이션 메트릭 수집 테스트")
    class ApplicationMetricsCollectionTest {

        @Test
        @DisplayName("애플리케이션 메트릭 수집 성공")
        void collectApplicationMetrics_Success() {
            // When
            List<Metric> metrics = micrometerMetricsCollector.collectApplicationMetrics();

            // Then
            assertThat(metrics).isNotNull();
            assertThat(metrics).isNotEmpty();
            
            // JVM 메모리 메트릭 확인
            assertThat(metrics.stream().anyMatch(m -> m.getMetricName().contains("jvm.memory.heap"))).isTrue();
            assertThat(metrics.stream().anyMatch(m -> m.getMetricName().contains("jvm.memory.nonheap"))).isTrue();
            
            // JVM 스레드 메트릭 확인
            assertThat(metrics.stream().anyMatch(m -> m.getMetricName().contains("jvm.threads"))).isTrue();
            
            // GC 메트릭 확인
            assertThat(metrics.stream().anyMatch(m -> m.getMetricName().contains("jvm.gc"))).isTrue();
            
            // 커스텀 메트릭 확인
            assertThat(metrics.stream().anyMatch(m -> m.getMetricName().contains("application.uptime"))).isTrue();
            assertThat(metrics.stream().anyMatch(m -> m.getMetricName().contains("jvm.classes"))).isTrue();
        }

        @Test
        @DisplayName("수집기 비활성화 시 빈 목록 반환")
        void collectApplicationMetrics_WhenDisabled_ReturnsEmptyList() {
            // Given
            ReflectionTestUtils.setField(micrometerMetricsCollector, "enabled", false);

            // When
            List<Metric> metrics = micrometerMetricsCollector.collectApplicationMetrics();

            // Then
            assertThat(metrics).isNotNull();
            assertThat(metrics).isEmpty();
        }

        @Test
        @DisplayName("메트릭 값이 유효함")
        void collectApplicationMetrics_ValuesAreValid() {
            // When
            List<Metric> metrics = micrometerMetricsCollector.collectApplicationMetrics();

            // Then
            for (Metric metric : metrics) {
                assertThat(metric.getMetricName()).isNotBlank();
                assertThat(metric.getMetricValue()).isNotNull();
                assertThat(metric.getUnit()).isNotBlank();
                assertThat(metric.getCollectedAt()).isNotNull();
                assertThat(metric.getMetricType()).isEqualTo(Metric.MetricType.APPLICATION);
            }
        }
    }

    @Nested
    @DisplayName("MeterRegistry 연동 테스트")
    class MeterRegistryIntegrationTest {

        @Test
        @DisplayName("MeterRegistry에 메트릭이 등록됨")
        void collectApplicationMetrics_RegistersMetricsToMeterRegistry() {
            // Given
            int initialMeterCount = meterRegistry.getMeters().size();

            // When
            micrometerMetricsCollector.collectApplicationMetrics();

            // Then
            int finalMeterCount = meterRegistry.getMeters().size();
            assertThat(finalMeterCount).isGreaterThan(initialMeterCount);
            
            // 특정 메트릭이 등록되었는지 확인
            assertThat(meterRegistry.find("jvm.memory.heap.used").gauge()).isNotNull();
            assertThat(meterRegistry.find("jvm.threads.count").gauge()).isNotNull();
            assertThat(meterRegistry.find("application.uptime").gauge()).isNotNull();
        }

        @Test
        @DisplayName("메트릭 값이 MeterRegistry에서 조회 가능함")
        void collectApplicationMetrics_MetricsAreAccessibleFromMeterRegistry() {
            // When
            micrometerMetricsCollector.collectApplicationMetrics();

            // Then
            var heapUsedGauge = meterRegistry.find("jvm.memory.heap.used").gauge();
            assertThat(heapUsedGauge).isNotNull();
            assertThat(heapUsedGauge.value()).isGreaterThanOrEqualTo(0);

            var threadCountGauge = meterRegistry.find("jvm.threads.count").gauge();
            assertThat(threadCountGauge).isNotNull();
            assertThat(threadCountGauge.value()).isGreaterThan(0);

            var uptimeGauge = meterRegistry.find("application.uptime").gauge();
            assertThat(uptimeGauge).isNotNull();
            assertThat(uptimeGauge.value()).isGreaterThanOrEqualTo(0);
        }
    }

    @Nested
    @DisplayName("예외 처리 테스트")
    class ExceptionHandlingTest {

        @Test
        @DisplayName("시스템 메트릭 수집 실패 시 BusinessException 발생")
        void collectSystemMetrics_WhenException_ThrowsBusinessException() {
            // Given - 비활성화된 수집기로 예외 유발
            MicrometerMetricsCollector faultyCollector = new MicrometerMetricsCollector(meterRegistry);
            faultyCollector.setEnabled(false);

            // When & Then
            SystemMetrics result = faultyCollector.collectSystemMetrics();
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("애플리케이션 메트릭 수집 실패 시 BusinessException 발생")
        void collectApplicationMetrics_WhenException_ThrowsBusinessException() {
            // Given - 비활성화된 수집기로 예외 유발
            MicrometerMetricsCollector faultyCollector = new MicrometerMetricsCollector(meterRegistry);
            faultyCollector.setEnabled(false);

            // When & Then
            List<Metric> result = faultyCollector.collectApplicationMetrics();
            assertThat(result).isEmpty();
        }
    }
}




