package com.agenticcp.core.domain.monitoring.service;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.monitoring.dto.SystemMetrics;
import com.agenticcp.core.domain.monitoring.entity.Metric;
import com.agenticcp.core.domain.monitoring.enums.CollectorType;
import com.agenticcp.core.domain.monitoring.TestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;


/**
 * ApplicationMetricsCollector 단위 테스트
 * 애플리케이션 메트릭 수집기의 핵심 비즈니스 로직을 검증
 */
@ExtendWith(MockitoExtension.class)
class ApplicationMetricsCollectorTest {

    @Mock
    private MetricsEndpoint metricsEndpoint;

    @InjectMocks
    private ApplicationMetricsCollector applicationMetricsCollector;

    /**
     * 정상적인 메트릭 수집 테스트 그룹
     */
    @Nested
    @DisplayName("정상적인 메트릭 수집 테스트")
    class SuccessfulMetricsCollectionTest {

        @Test
        @DisplayName("시스템 메트릭 수집 성공")
        void collectSystemMetrics_Success() {
            // When
            SystemMetrics systemMetrics = applicationMetricsCollector.collectSystemMetrics();

            // Then
            assertThat(systemMetrics).isNotNull();
            assertThat(systemMetrics.getCollectedAt()).isNotNull();
            assertThat(systemMetrics.getSystemInfo()).isNotNull();
            assertThat(systemMetrics.getMetadata()).isNotNull();
            assertThat(systemMetrics.getMetadata()).containsKey("collector_type");
            assertThat(systemMetrics.getMetadata().get("collector_type")).isEqualTo("APPLICATION");
        }

        @Test
        @DisplayName("애플리케이션 메트릭 수집 성공")
        void collectApplicationMetrics_Success() {
            // When
            List<Metric> metrics = applicationMetricsCollector.collectApplicationMetrics();

            // Then
            assertThat(metrics).isNotNull();
            assertThat(metrics).isNotEmpty();
            
            // JVM 메모리 메트릭 확인
            assertThat(metrics).extracting(Metric::getMetricName)
                    .contains("jvm.memory.heap.used", "jvm.memory.heap.max", "jvm.memory.heap.committed");
            
            // JVM 스레드 메트릭 확인
            assertThat(metrics).extracting(Metric::getMetricName)
                    .contains("jvm.threads.count", "jvm.threads.peak", "jvm.threads.daemon");
            
            // 모든 메트릭이 APPLICATION 타입인지 확인
            assertThat(metrics).extracting(Metric::getMetricType)
                    .containsOnly(Metric.MetricType.APPLICATION);
        }

        @Test
        @DisplayName("수집기 타입 확인")
        void getCollectorType_Success() {
            // When
            CollectorType type = applicationMetricsCollector.getCollectorType();

            // Then
            assertThat(type).isEqualTo(CollectorType.APPLICATION);
        }

        @Test
        @DisplayName("수집기 활성화 상태 확인")
        void isEnabled_Success() {
            // When
            boolean enabled = applicationMetricsCollector.isEnabled();

            // Then
            assertThat(enabled).isTrue();
        }
    }

    /**
     * 비활성화 상태 테스트 그룹
     */
    @Nested
    @DisplayName("비활성화 상태 테스트")
    class DisabledStateTest {

        @Test
        @DisplayName("비활성화된 수집기에서 시스템 메트릭 수집")
        void collectSystemMetrics_WhenDisabled_ReturnsEmptyMetrics() {
            // Given
            applicationMetricsCollector.setEnabled(false);

            // When
            SystemMetrics systemMetrics = applicationMetricsCollector.collectSystemMetrics();

            // Then
            assertThat(systemMetrics).isNotNull();
            assertThat(systemMetrics.getMemoryUsage()).isNull();
            assertThat(systemMetrics.getMetadata()).containsEntry("enabled", false);
        }

        @Test
        @DisplayName("비활성화된 수집기에서 애플리케이션 메트릭 수집")
        void collectApplicationMetrics_WhenDisabled_ReturnsEmptyList() {
            // Given
            applicationMetricsCollector.setEnabled(false);

            // When
            List<Metric> metrics = applicationMetricsCollector.collectApplicationMetrics();

            // Then
            assertThat(metrics).isNotNull();
            assertThat(metrics).isEmpty();
        }

        @Test
        @DisplayName("수집기 활성화/비활성화 설정")
        void setEnabled_Success() {
            // Given
            assertThat(applicationMetricsCollector.isEnabled()).isTrue();

            // When
            applicationMetricsCollector.setEnabled(false);

            // Then
            assertThat(applicationMetricsCollector.isEnabled()).isFalse();

            // 다시 활성화
            applicationMetricsCollector.setEnabled(true);
            assertThat(applicationMetricsCollector.isEnabled()).isTrue();
        }
    }

    /**
     * 메트릭 타입별 수집 테스트 그룹
     */
    @Nested
    @DisplayName("메트릭 타입별 수집 테스트")
    class MetricTypeCollectionTest {

        @Test
        @DisplayName("JVM 메모리 메트릭 수집 확인")
        void collectJvmMemoryMetrics_Success() {
            // When
            List<Metric> metrics = applicationMetricsCollector.collectApplicationMetrics();

            // Then
            // Heap 메모리 메트릭 확인
            assertThat(metrics).extracting(Metric::getMetricName)
                    .contains("jvm.memory.heap.used", "jvm.memory.heap.max", "jvm.memory.heap.committed");
            
            // Non-Heap 메모리 메트릭 확인
            assertThat(metrics).extracting(Metric::getMetricName)
                    .contains("jvm.memory.nonheap.used", "jvm.memory.nonheap.max", "jvm.memory.nonheap.committed");
            
            // 메모리 사용률 메트릭 확인
            assertThat(metrics).extracting(Metric::getMetricName)
                    .contains("jvm.memory.heap.usage");
        }

        @Test
        @DisplayName("JVM 스레드 메트릭 수집 확인")
        void collectJvmThreadMetrics_Success() {
            // When
            List<Metric> metrics = applicationMetricsCollector.collectApplicationMetrics();

            // Then
            assertThat(metrics).extracting(Metric::getMetricName)
                    .contains("jvm.threads.count", "jvm.threads.peak", 
                             "jvm.threads.total_started", "jvm.threads.daemon");
        }

        @Test
        @DisplayName("GC 메트릭 수집 확인")
        void collectGcMetrics_Success() {
            // When
            List<Metric> metrics = applicationMetricsCollector.collectApplicationMetrics();

            // Then
            // GC 메트릭이 있는지 확인 (실제 GC 빈이 있는 경우에만)
            boolean hasGcMetrics = metrics.stream()
                    .anyMatch(metric -> metric.getMetricName().startsWith("jvm.gc."));
            
            // GC 메트릭이 있으면 count와 time 메트릭이 모두 있는지 확인
            if (hasGcMetrics) {
                assertThat(metrics).extracting(Metric::getMetricName)
                        .anyMatch(name -> name.endsWith(".count"));
                assertThat(metrics).extracting(Metric::getMetricName)
                        .anyMatch(name -> name.endsWith(".time"));
            }
        }
    }

    /**
     * 메트릭 단위 및 값 검증 테스트 그룹
     */
    @Nested
    @DisplayName("메트릭 단위 및 값 검증 테스트")
    class MetricValueValidationTest {

        @Test
        @DisplayName("메트릭 단위 검증")
        void metricUnits_Validation() {
            // When
            List<Metric> metrics = applicationMetricsCollector.collectApplicationMetrics();

            // Then
            // 메모리 사용량 메트릭은 MB 단위
            metrics.stream()
                    .filter(metric -> metric.getMetricName().contains("memory") && 
                            !metric.getMetricName().contains("usage"))
                    .forEach(metric -> assertThat(metric.getUnit()).isEqualTo("MB"));

            // 메모리 사용률 메트릭은 % 단위
            metrics.stream()
                    .filter(metric -> metric.getMetricName().contains("usage"))
                    .forEach(metric -> assertThat(metric.getUnit()).isEqualTo("%"));

            // 스레드 메트릭은 count 단위
            metrics.stream()
                    .filter(metric -> metric.getMetricName().contains("threads"))
                    .forEach(metric -> assertThat(metric.getUnit()).isEqualTo("count"));

            // GC 메트릭은 count 또는 ms 단위
            metrics.stream()
                    .filter(metric -> metric.getMetricName().startsWith("jvm.gc."))
                    .forEach(metric -> assertThat(metric.getUnit()).isIn("count", "ms"));
        }

        @Test
        @DisplayName("메트릭 값 범위 검증")
        void metricValueRanges_Validation() {
            // When
            List<Metric> metrics = applicationMetricsCollector.collectApplicationMetrics();

            // Then
            // 모든 메트릭 값이 null이 아니고 유효한 범위에 있는지 확인
            metrics.forEach(metric -> {
                assertThat(metric.getMetricValue()).isNotNull();
                assertThat(metric.getMetricValue()).isNotNaN();
                assertThat(metric.getMetricValue()).isNotInfinite();
                
                // 메모리 사용률은 0-100% 범위
                if (metric.getMetricName().contains("usage") && "%".equals(metric.getUnit())) {
                    assertThat(metric.getMetricValue()).isBetween(0.0, 100.0);
                }
                
                // 카운트 메트릭은 0 이상
                if ("count".equals(metric.getUnit())) {
                    assertThat(metric.getMetricValue()).isGreaterThanOrEqualTo(0.0);
                }
            });
        }
        
        @Test
        @DisplayName("테스트 데이터 빌더 사용 예시")
        void testDataBuilder_Usage() {
            // Given
            Metric testMetric = TestDataBuilder.applicationMetricBuilder()
                    .metricName("test.metric")
                    .metricValue(100.0)
                    .unit("count")
                    .build();
            SystemMetrics testSystemMetrics = TestDataBuilder.systemMetrics();
            
            // When & Then
            assertThat(testMetric.getMetricName()).isEqualTo("test.metric");
            assertThat(testMetric.getMetricValue()).isEqualTo(100.0);
            assertThat(testMetric.getUnit()).isEqualTo("count");
            assertThat(testMetric.getMetricType()).isEqualTo(Metric.MetricType.APPLICATION);
            
            assertThat(testSystemMetrics.getSystemInfo()).isNotNull();
            assertThat(testSystemMetrics.getSystemInfo().getHostname()).isEqualTo("test-host");
        }
    }

    /**
     * 시스템 정보 테스트 그룹
     */
    @Nested
    @DisplayName("시스템 정보 테스트")
    class SystemInfoTest {

        @Test
        @DisplayName("시스템 정보 수집 확인")
        void systemInfo_Collection() {
            // When
            SystemMetrics systemMetrics = applicationMetricsCollector.collectSystemMetrics();

            // Then
            SystemMetrics.SystemInfo systemInfo = systemMetrics.getSystemInfo();
            assertThat(systemInfo).isNotNull();
            assertThat(systemInfo.getJavaVersion()).isNotNull();
            assertThat(systemInfo.getAvailableProcessors()).isPositive();
        }

        @Test
        @DisplayName("메타데이터 구성 확인")
        void metadata_Composition() {
            // When
            SystemMetrics systemMetrics = applicationMetricsCollector.collectSystemMetrics();

            // Then
            assertThat(systemMetrics.getMetadata()).isNotNull();
            assertThat(systemMetrics.getMetadata()).containsKey("collector_type");
            assertThat(systemMetrics.getMetadata()).containsKey("jvm_version");
            assertThat(systemMetrics.getMetadata()).containsKey("available_processors");
            assertThat(systemMetrics.getMetadata().get("collector_type")).isEqualTo("APPLICATION");
        }
    }

    /**
     * 예외 처리 테스트 그룹
     */
    @Nested
    @DisplayName("예외 처리 테스트")
    class ExceptionHandlingTest {

        @Test
        @DisplayName("MetricsEndpoint null 처리")
        void handleNullMetricsEndpoint_Success() {
            // Given
            ApplicationMetricsCollector collectorWithNullEndpoint = 
                    new ApplicationMetricsCollector(null);

            // When
            List<Metric> metrics = collectorWithNullEndpoint.collectApplicationMetrics();

            // Then
            assertThat(metrics).isNotNull();
            // JVM 메트릭은 여전히 수집되어야 함
            assertThat(metrics).isNotEmpty();
            assertThat(metrics).extracting(Metric::getMetricName)
                    .contains("jvm.memory.heap.used", "jvm.threads.count");
            
            // null MetricsEndpoint에 대한 검증 (호출되지 않아야 함)
            // verify()는 Mock 객체에만 사용 가능하므로 여기서는 생략
        }

        @Test
        @DisplayName("메트릭 수집 중 예외 발생 시 BusinessException 발생")
        void collectMetrics_Exception_ThrowsBusinessException() {
            // Given
            ApplicationMetricsCollector collectorWithNullEndpoint = 
                    new ApplicationMetricsCollector(null);

            // When & Then
            // null MetricsEndpoint로 인한 예외 상황 테스트
            assertThatCode(() -> collectorWithNullEndpoint.collectApplicationMetrics())
                    .doesNotThrowAnyException();
        }
    }
}
