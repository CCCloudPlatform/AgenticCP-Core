package com.agenticcp.core.domain.monitoring.service;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.monitoring.dto.SystemMetrics;
import com.agenticcp.core.domain.monitoring.entity.Metric;
import com.agenticcp.core.domain.monitoring.repository.MetricRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * MetricsCollectionService 단위 테스트
 * 메트릭 수집 서비스의 핵심 비즈니스 로직을 검증
 * 테스트 가이드라인에 따라 @Nested 클래스로 그룹화
 */
@ExtendWith(MockitoExtension.class)
class MetricsCollectionServiceTest {

    @Mock
    private MetricRepository metricRepository;

    @Mock
    private SystemMetricsCollector systemMetricsCollector;

    @InjectMocks
    private MetricsCollectionService metricsCollectionService;

    /**
     * 정상적인 메트릭 수집 테스트 그룹
     * 시나리오 1: SystemMetricsCollector가 유효한 데이터를 반환하고 7개의 개별 메트릭이 저장되는지 검증
     * 시나리오 2: 수동 메트릭 수집 시 시스템과 애플리케이션 메트릭이 모두 수집되는지 검증
     */
    @Nested
    @DisplayName("정상적인 메트릭 수집 테스트")
    class SuccessfulMetricsCollectionTest {

        /**
         * 시나리오 1: 정상적인 시스템 메트릭 수집 및 저장
         * Given: SystemMetricsCollector가 유효한 SystemMetrics 반환
         * When: collectSystemMetrics() 호출
         * Then: 7개의 개별 메트릭이 데이터베이스에 저장됨
         */
        @Test
        @DisplayName("시스템 메트릭 수집 성공 시 개별 메트릭들이 저장됨")
        void collectSystemMetrics_WhenValidMetrics_ShouldSaveIndividualMetrics() {
            // Given
            SystemMetrics testMetrics = TestDataBuilder.systemMetricsBuilder().build();
            when(systemMetricsCollector.collectSystemMetrics()).thenReturn(testMetrics);
            when(metricRepository.save(any(Metric.class))).thenReturn(mock(Metric.class));

            // When
            metricsCollectionService.collectSystemMetrics();

            // Then
            verify(systemMetricsCollector, times(1)).collectSystemMetrics();
            verify(metricRepository, times(7)).save(any(Metric.class));
        }

        /**
         * 시나리오 2: 수동 메트릭 수집
         * Given: 수동 메트릭 수집 요청
         * When: collectMetricsManually() 호출
         * Then: 시스템 메트릭과 애플리케이션 메트릭이 모두 수집됨
         */
        @Test
        @DisplayName("수동 메트릭 수집 시 모든 메트릭이 수집됨")
        void collectMetricsManually_shouldCollectAllMetrics() {
            // Given
            SystemMetrics testMetrics = TestDataBuilder.systemMetricsBuilder().build();
            when(systemMetricsCollector.collectSystemMetrics()).thenReturn(testMetrics);
            when(metricRepository.save(any(Metric.class))).thenReturn(mock(Metric.class));

            // When
            metricsCollectionService.collectMetricsManually();

            // Then
            verify(systemMetricsCollector, times(1)).collectSystemMetrics();
            verify(metricRepository, times(7)).save(any(Metric.class));
        }
    }

    /**
     * 개별 메트릭 저장 검증 테스트 그룹
     * 시나리오 3: CPU 사용률 메트릭이 올바른 이름, 값, 단위로 저장되는지 검증
     * 시나리오 4: 메모리 관련 3개 메트릭(사용률, 사용량, 총량)이 올바르게 저장되는지 검증
     * 시나리오 5: 디스크 관련 3개 메트릭(사용률, 사용량, 총량)이 올바르게 저장되는지 검증
     */
    @Nested
    @DisplayName("개별 메트릭 저장 검증 테스트")
    class IndividualMetricsValidationTest {

        /**
         * 시나리오 3: CPU 사용률 메트릭 저장 검증
         * Given: SystemMetrics에 CPU 사용률 포함
         * When: collectSystemMetrics() 호출
         * Then: cpu.usage 메트릭이 올바르게 저장됨
         */
        @Test
        @DisplayName("CPU 사용률 메트릭이 올바르게 저장됨")
        void collectSystemMetrics_shouldSaveCpuUsageMetric() {
            // Given
            SystemMetrics testMetrics = TestDataBuilder.systemMetricsBuilder().build();
            when(systemMetricsCollector.collectSystemMetrics()).thenReturn(testMetrics);
            when(metricRepository.save(any(Metric.class))).thenReturn(mock(Metric.class));

            // When
            metricsCollectionService.collectSystemMetrics();

            // Then: cpu.usage 메트릭 저장 확인
            verify(metricRepository).save(argThat(metric -> 
                "cpu.usage".equals(metric.getMetricName()) &&
                Double.valueOf(45.2).equals(metric.getMetricValue()) &&
                "%".equals(metric.getUnit()) &&
                Metric.MetricType.SYSTEM.equals(metric.getMetricType())
            ));
        }

        /**
         * 시나리오 4: 메모리 관련 메트릭 저장 검증
         * Given: SystemMetrics에 메모리 정보 포함
         * When: collectSystemMetrics() 호출
         * Then: 메모리 관련 3개 메트릭이 저장됨
         */
        @Test
        @DisplayName("메모리 관련 메트릭들이 올바르게 저장됨")
        void collectSystemMetrics_shouldSaveMemoryMetrics() {
            // Given
            SystemMetrics testMetrics = TestDataBuilder.systemMetricsBuilder().build();
            when(systemMetricsCollector.collectSystemMetrics()).thenReturn(testMetrics);
            when(metricRepository.save(any(Metric.class))).thenReturn(mock(Metric.class));

            // When
            metricsCollectionService.collectSystemMetrics();

            // Then: 메모리 사용률 저장 확인
            verify(metricRepository).save(argThat(metric -> 
                "memory.usage".equals(metric.getMetricName()) &&
                Double.valueOf(67.8).equals(metric.getMetricValue()) &&
                "%".equals(metric.getUnit())
            ));

            // Then: 메모리 사용량 저장 확인
            verify(metricRepository).save(argThat(metric -> 
                "memory.used".equals(metric.getMetricName()) &&
                Double.valueOf(2048.0).equals(metric.getMetricValue()) &&
                "MB".equals(metric.getUnit())
            ));

            // Then: 메모리 총량 저장 확인
            verify(metricRepository).save(argThat(metric -> 
                "memory.total".equals(metric.getMetricName()) &&
                Double.valueOf(4096.0).equals(metric.getMetricValue()) &&
                "MB".equals(metric.getUnit())
            ));
        }

        /**
         * 시나리오 5: 디스크 관련 메트릭 저장 검증
         * Given: SystemMetrics에 디스크 정보 포함
         * When: collectSystemMetrics() 호출
         * Then: 디스크 관련 3개 메트릭이 저장됨
         */
        @Test
        @DisplayName("디스크 관련 메트릭들이 올바르게 저장됨")
        void collectSystemMetrics_shouldSaveDiskMetrics() {
            // Given
            SystemMetrics testMetrics = TestDataBuilder.systemMetricsBuilder().build();
            when(systemMetricsCollector.collectSystemMetrics()).thenReturn(testMetrics);
            when(metricRepository.save(any(Metric.class))).thenReturn(mock(Metric.class));

            // When
            metricsCollectionService.collectSystemMetrics();

            // Then: 디스크 사용률 저장 확인
            verify(metricRepository).save(argThat(metric -> 
                "disk.usage".equals(metric.getMetricName()) &&
                Double.valueOf(23.1).equals(metric.getMetricValue()) &&
                "%".equals(metric.getUnit())
            ));

            // Then: 디스크 사용량 저장 확인
            verify(metricRepository).save(argThat(metric -> 
                "disk.used".equals(metric.getMetricName()) &&
                Double.valueOf(100.0).equals(metric.getMetricValue()) &&
                "GB".equals(metric.getUnit())
            ));

            // Then: 디스크 총량 저장 확인
            verify(metricRepository).save(argThat(metric -> 
                "disk.total".equals(metric.getMetricName()) &&
                Double.valueOf(500.0).equals(metric.getMetricValue()) &&
                "GB".equals(metric.getUnit())
            ));
        }
    }

    /**
     * 특수 케이스 처리 테스트 그룹
     * 시나리오 6: null 값이 있는 메트릭은 건너뛰고 null이 아닌 값들만 저장되는지 검증
     * 시나리오 7: 메타데이터가 JSON 문자열로 올바르게 변환되어 저장되는지 검증
     * 시나리오 8: 애플리케이션 메트릭 수집이 현재 TODO 상태로 예외 없이 처리되는지 검증
     * 시나리오 9: 높은 리소스 사용률 상황에서도 정상적으로 처리되는지 검증
     */
    @Nested
    @DisplayName("특수 케이스 처리 테스트")
    class SpecialCaseHandlingTest {

        /**
         * 시나리오 6: null 값 처리 검증
         * Given: SystemMetrics에 일부 값이 null
         * When: collectSystemMetrics() 호출
         * Then: null이 아닌 값들만 저장됨
         */
        @Test
        @DisplayName("null 값이 있는 메트릭은 저장되지 않음")
        void collectSystemMetrics_shouldSkipNullMetrics() {
            // Given
            SystemMetrics metricsWithNulls = TestDataBuilder.systemMetricsWithNullsBuilder().build();
            when(systemMetricsCollector.collectSystemMetrics()).thenReturn(metricsWithNulls);
            when(metricRepository.save(any(Metric.class))).thenReturn(mock(Metric.class));

            // When
            metricsCollectionService.collectSystemMetrics();

            // Then: null이 아닌 값들만 저장됨 (4개 메트릭)
            verify(metricRepository, times(4)).save(any(Metric.class));
        }

        /**
         * 시나리오 7: 메타데이터 변환 검증
         * Given: SystemMetrics에 메타데이터 포함
         * When: collectSystemMetrics() 호출
         * Then: 메타데이터가 문자열로 변환되어 저장됨
         */
        @Test
        @DisplayName("메타데이터가 문자열로 변환되어 저장됨")
        void collectSystemMetrics_shouldConvertMetadataToString() {
            // Given
            SystemMetrics testMetrics = TestDataBuilder.systemMetricsBuilder().build();
            when(systemMetricsCollector.collectSystemMetrics()).thenReturn(testMetrics);
            when(metricRepository.save(any(Metric.class))).thenReturn(mock(Metric.class));

            // When
            metricsCollectionService.collectSystemMetrics();

            // Then: 메타데이터가 문자열로 변환되어 저장됨
            verify(metricRepository, atLeastOnce()).save(argThat(metric -> 
                metric.getMetadata() != null && 
                metric.getMetadata().contains("test-host")
            ));
        }

        /**
         * 시나리오 8: 애플리케이션 메트릭 수집 (현재 TODO 상태)
         * Given: collectApplicationMetrics() 호출
         * When: collectApplicationMetrics() 실행
         * Then: 현재는 로그만 출력하고 예외 발생하지 않음
         */
        @Test
        @DisplayName("애플리케이션 메트릭 수집은 현재 TODO 상태")
        void collectApplicationMetrics_shouldNotThrowException() {
            // When & Then: 예외 발생하지 않음
            assertThatCode(() -> metricsCollectionService.collectApplicationMetrics())
                    .doesNotThrowAnyException();
        }

        /**
         * 시나리오 9: 높은 리소스 사용률 상황 처리
         * Given: 높은 CPU/메모리/디스크 사용률을 가진 SystemMetrics
         * When: collectSystemMetrics() 호출
         * Then: 높은 사용률 상황에서도 정상적으로 처리됨
         */
        @Test
        @DisplayName("높은 리소스 사용률 상황에서도 정상 처리")
        void collectSystemMetrics_WithHighResourceUsage_ShouldProcessNormally() {
            // Given
            SystemMetrics highUsageMetrics = TestDataBuilder.highResourceUsageSystemMetricsBuilder().build();
            when(systemMetricsCollector.collectSystemMetrics()).thenReturn(highUsageMetrics);
            when(metricRepository.save(any(Metric.class))).thenReturn(mock(Metric.class));

            // When
            metricsCollectionService.collectSystemMetrics();

            // Then: 7개의 메트릭이 모두 저장됨
            verify(metricRepository, times(7)).save(any(Metric.class));
        }
    }

    /**
     * 예외 처리 테스트 그룹
     * 시나리오 10: SystemMetricsCollector에서 예외 발생 시 적절한 BusinessException으로 변환되는지 검증
     * 시나리오 11: Repository 저장 실패 시 적절한 BusinessException이 발생하는지 검증
     * 시나리오 12: 수동 메트릭 수집 중 예외 발생 시 적절한 예외 처리가 되는지 검증
     */
    @Nested
    @DisplayName("예외 처리 테스트")
    class ExceptionHandlingTest {

        /**
         * 시나리오 10: SystemMetricsCollector 예외 처리
         * Given: SystemMetricsCollector가 예외 발생
         * When: collectSystemMetrics() 호출
         * Then: BusinessException이 발생함
         */
        @Test
        @DisplayName("SystemMetricsCollector 예외 시 BusinessException 발생")
        void collectSystemMetrics_shouldThrowBusinessExceptionWhenCollectorFails() {
            // Given
            when(systemMetricsCollector.collectSystemMetrics())
                    .thenThrow(new RuntimeException("Collection failed"));

            // When & Then
            assertThatThrownBy(() -> metricsCollectionService.collectSystemMetrics())
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("시스템 메트릭 수집 중 예상치 못한 오류가 발생했습니다.");
        }

        /**
         * 시나리오 11: Repository 저장 실패 시 예외 처리
         * Given: MetricRepository가 예외 발생
         * When: collectSystemMetrics() 호출
         * Then: BusinessException이 발생함
         */
        @Test
        @DisplayName("Repository 저장 실패 시 BusinessException 발생")
        void collectSystemMetrics_shouldThrowBusinessExceptionWhenSaveFails() {
            // Given
            SystemMetrics testMetrics = TestDataBuilder.systemMetricsBuilder().build();
            when(systemMetricsCollector.collectSystemMetrics()).thenReturn(testMetrics);
            when(metricRepository.save(any(Metric.class)))
                    .thenThrow(new RuntimeException("Database error"));

            // When & Then
            assertThatThrownBy(() -> metricsCollectionService.collectSystemMetrics())
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("메트릭 데이터 저장 중 오류가 발생했습니다.");
        }

        /**
         * 시나리오 12: 수동 메트릭 수집 중 예외 처리
         * Given: 수동 메트릭 수집 중 SystemMetricsCollector에서 예외 발생
         * When: collectMetricsManually() 호출
         * Then: 적절한 BusinessException이 발생함
         */
        @Test
        @DisplayName("수동 메트릭 수집 중 예외 발생 시 적절한 예외 처리")
        void collectMetricsManually_WhenExceptionOccurs_ShouldHandleGracefully() {
            // Given
            when(systemMetricsCollector.collectSystemMetrics())
                    .thenThrow(new RuntimeException("Manual collection failed"));

            // When & Then
            assertThatThrownBy(() -> metricsCollectionService.collectMetricsManually())
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("메트릭 수집 중 예상치 못한 오류가 발생했습니다.");
        }
    }
}
