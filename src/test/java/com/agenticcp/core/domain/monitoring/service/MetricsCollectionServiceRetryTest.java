package com.agenticcp.core.domain.monitoring.service;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.monitoring.dto.SystemMetrics;
import com.agenticcp.core.domain.monitoring.entity.Metric;
import com.agenticcp.core.domain.monitoring.enums.CollectorType;
import com.agenticcp.core.domain.monitoring.enums.MonitoringErrorCode;
import com.agenticcp.core.domain.monitoring.repository.MetricRepository;
import com.agenticcp.core.domain.monitoring.repository.MetricThresholdRepository;
import com.agenticcp.core.domain.monitoring.storage.MetricsStorageFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * MetricsCollectionService 재시도 로직 단위 테스트
 * 
 * <p>Issue #39: Task 8 - 재시도 로직 및 오류 처리 구현
 * <p>@Retryable과 @Recover 로직은 통합 테스트에서 검증하며,
 * 여기서는 캐시와 부분 실패 처리 등 검증 가능한 로직만 테스트합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MetricsCollectionService 재시도 로직 테스트")
class MetricsCollectionServiceRetryTest {
    
    @Mock
    private MetricRepository metricRepository;
    
    @Mock
    private MetricThresholdRepository metricThresholdRepository;
    
    @Mock
    private SystemMetricsCollector systemMetricsCollector;
    
    @Mock
    private MetricsCollectorFactory metricsCollectorFactory;
    
    @Mock
    private MetricsStorageFactory metricsStorageFactory;
    
    @Mock
    private MetricsCache metricsCache;
    
    @InjectMocks
    private MetricsCollectionService metricsCollectionService;
    
    private SystemMetrics testSystemMetrics;
    
    @BeforeEach
    void setUp() {
        testSystemMetrics = SystemMetrics.builder()
                .cpuUsage(50.0)
                .memoryUsage(60.0)
                .memoryUsedMB(1024L)
                .memoryTotalMB(2048L)
                .diskUsage(70.0)
                .diskUsedGB(100L)
                .diskTotalGB(500L)
                .collectedAt(LocalDateTime.now())
                .build();
    }
    
    @Nested
    @DisplayName("캐시 저장 검증")
    class CachingBehavior {
        
        @Test
        @DisplayName("시스템 메트릭 수집 성공 시 캐시에 저장된다")
        void collectSystemMetrics_OnSuccess_ShouldCacheMetrics() {
            // Given
            when(systemMetricsCollector.collectSystemMetrics()).thenReturn(testSystemMetrics);
            when(metricRepository.save(any(Metric.class))).thenReturn(null);
            
            // When
            metricsCollectionService.collectSystemMetrics();
            
            // Then
            verify(metricsCache).cacheSystemMetrics(testSystemMetrics);
        }
        
        @Test
        @DisplayName("애플리케이션 메트릭 수집 성공 시 캐시에 저장된다")
        void collectApplicationMetrics_OnSuccess_ShouldCacheMetrics() {
            // Given
            List<Metric> metrics = new ArrayList<>();
            metrics.add(Metric.builder()
                    .metricName("jvm.memory.used")
                    .metricValue(1024.0)
                    .unit("MB")
                    .metricType(Metric.MetricType.APPLICATION)
                    .collectedAt(LocalDateTime.now())
                    .build());
            
            MetricsCollector applicationCollector = mock(MetricsCollector.class);
            when(metricsCollectorFactory.createCollector(CollectorType.APPLICATION))
                    .thenReturn(applicationCollector);
            when(applicationCollector.isEnabled()).thenReturn(true);
            when(applicationCollector.collectApplicationMetrics()).thenReturn(metrics);
            when(metricRepository.save(any(Metric.class))).thenReturn(null);
            
            // When
            metricsCollectionService.collectApplicationMetrics();
            
            // Then
            verify(metricsCache).cacheApplicationMetrics(metrics);
        }
    }
    
    @Nested
    @DisplayName("폴백 처리 검증")
    class FallbackBehavior {
        
        @Test
        @DisplayName("시스템 메트릭 폴백 시 캐시가 있으면 예외를 발생시키지 않는다")
        void recoverFromSystemMetricsFailure_WhenCacheExists_ShouldNotThrowException() {
            // Given
            when(metricsCache.getLastSuccessfulSystemMetrics()).thenReturn(testSystemMetrics);
            Exception testException = new RuntimeException("Test");
            
            // When
            metricsCollectionService.recoverFromSystemMetricsFailure(testException);
            
            // Then
            verify(metricsCache).getLastSuccessfulSystemMetrics();
            // 예외 발생하지 않음
        }
        
        @Test
        @DisplayName("시스템 메트릭 폴백 시 캐시가 없으면 예외를 발생시킨다")
        void recoverFromSystemMetricsFailure_WhenNoCache_ShouldThrowException() {
            // Given
            when(metricsCache.getLastSuccessfulSystemMetrics()).thenReturn(null);
            Exception testException = new RuntimeException("Test");
            
            // When & Then
            assertThatThrownBy(() -> 
                    metricsCollectionService.recoverFromSystemMetricsFailure(testException))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MonitoringErrorCode.RETRY_EXHAUSTED);
            
            verify(metricsCache).getLastSuccessfulSystemMetrics();
        }
        
        @Test
        @DisplayName("애플리케이션 메트릭 폴백 시 캐시가 있으면 사용한다")
        void recoverFromApplicationMetricsFailure_WhenCacheExists_ShouldUseCache() {
            // Given
            List<Metric> cachedMetrics = new ArrayList<>();
            cachedMetrics.add(Metric.builder()
                    .metricName("jvm.memory.used")
                    .metricValue(1024.0)
                    .metricType(Metric.MetricType.APPLICATION)
                    .collectedAt(LocalDateTime.now())
                    .build());
            
            when(metricsCache.getLastSuccessfulApplicationMetrics()).thenReturn(cachedMetrics);
            Exception testException = new RuntimeException("Test");
            
            // When
            metricsCollectionService.recoverFromApplicationMetricsFailure(testException);
            
            // Then
            verify(metricsCache).getLastSuccessfulApplicationMetrics();
            // 예외 발생하지 않음 (애플리케이션 메트릭은 선택적)
        }
        
        @Test
        @DisplayName("애플리케이션 메트릭 폴백 시 캐시가 없어도 예외를 발생시키지 않는다")
        void recoverFromApplicationMetricsFailure_WhenNoCache_ShouldNotThrowException() {
            // Given
            when(metricsCache.getLastSuccessfulApplicationMetrics()).thenReturn(new ArrayList<>());
            Exception testException = new RuntimeException("Test");
            
            // When
            metricsCollectionService.recoverFromApplicationMetricsFailure(testException);
            
            // Then
            verify(metricsCache).getLastSuccessfulApplicationMetrics();
            // 예외 발생하지 않음 (경고 로그만)
        }
    }
    
    @Nested
    @DisplayName("부분 실패 처리 검증")
    class PartialFailureHandling {
        
        @Test
        @DisplayName("애플리케이션 메트릭 일부 실패 시 나머지는 저장된다")
        void collectApplicationMetrics_WhenPartialFailure_ShouldSaveSuccessfulMetrics() {
            // Given
            List<Metric> metrics = new ArrayList<>();
            metrics.add(Metric.builder()
                    .metricName("metric1")
                    .metricValue(1.0)
                    .unit("count")
                    .metricType(Metric.MetricType.APPLICATION)
                    .collectedAt(LocalDateTime.now())
                    .build());
            metrics.add(Metric.builder()
                    .metricName("metric2")
                    .metricValue(2.0)
                    .unit("count")
                    .metricType(Metric.MetricType.APPLICATION)
                    .collectedAt(LocalDateTime.now())
                    .build());
            metrics.add(Metric.builder()
                    .metricName("metric3")
                    .metricValue(3.0)
                    .unit("count")
                    .metricType(Metric.MetricType.APPLICATION)
                    .collectedAt(LocalDateTime.now())
                    .build());
            
            MetricsCollector applicationCollector = mock(MetricsCollector.class);
            when(metricsCollectorFactory.createCollector(CollectorType.APPLICATION))
                    .thenReturn(applicationCollector);
            when(applicationCollector.isEnabled()).thenReturn(true);
            when(applicationCollector.collectApplicationMetrics()).thenReturn(metrics);
            
            // metric2 저장 실패 시뮬레이션
            when(metricRepository.save(any(Metric.class)))
                    .thenReturn(null)
                    .thenThrow(new RuntimeException("저장 실패"))
                    .thenReturn(null);
            
            // When
            metricsCollectionService.collectApplicationMetrics();
            
            // Then
            verify(metricRepository, times(3)).save(any(Metric.class));
            verify(metricsCache).cacheApplicationMetrics(metrics);
            // 부분 실패여도 예외 발생하지 않음 (2개는 성공)
        }
        
        @Test
        @DisplayName("모든 메트릭 저장 실패 시 예외를 발생시킨다")
        void collectApplicationMetrics_WhenAllFailed_ShouldThrowException() {
            // Given
            List<Metric> metrics = new ArrayList<>();
            metrics.add(Metric.builder()
                    .metricName("metric1")
                    .metricValue(1.0)
                    .unit("count")
                    .metricType(Metric.MetricType.APPLICATION)
                    .collectedAt(LocalDateTime.now())
                    .build());
            
            MetricsCollector applicationCollector = mock(MetricsCollector.class);
            when(metricsCollectorFactory.createCollector(CollectorType.APPLICATION))
                    .thenReturn(applicationCollector);
            when(applicationCollector.isEnabled()).thenReturn(true);
            when(applicationCollector.collectApplicationMetrics()).thenReturn(metrics);
            when(metricRepository.save(any(Metric.class)))
                    .thenThrow(new RuntimeException("저장 실패"));
            
            // When & Then
            assertThatThrownBy(() -> metricsCollectionService.collectApplicationMetrics())
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MonitoringErrorCode.METRIC_SAVE_FAILED);
        }
    }
}

