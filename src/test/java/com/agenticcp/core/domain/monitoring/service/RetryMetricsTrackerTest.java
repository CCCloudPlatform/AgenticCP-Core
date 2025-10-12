package com.agenticcp.core.domain.monitoring.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RetryMetricsTracker 단위 테스트
 * 
 * <p>Issue #39: Task 8 - 재시도 로직 및 오류 처리 구현
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@DisplayName("RetryMetricsTracker 단위 테스트")
class RetryMetricsTrackerTest {
    
    private MeterRegistry meterRegistry;
    private RetryMetricsTracker retryMetricsTracker;
    
    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        retryMetricsTracker = new RetryMetricsTracker(meterRegistry);
    }
    
    @Nested
    @DisplayName("재시도 시도 기록")
    class RecordRetryAttempt {
        
        @Test
        @DisplayName("재시도 시도를 기록하면 카운터가 증가한다")
        void recordRetryAttempt_ShouldIncrementCounter() {
            // When
            retryMetricsTracker.recordRetryAttempt();
            retryMetricsTracker.recordRetryAttempt();
            retryMetricsTracker.recordRetryAttempt();
            
            // Then
            Counter counter = meterRegistry.find("metrics.collection.retry.attempts").counter();
            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(3.0);
        }
    }
    
    @Nested
    @DisplayName("재시도 성공 기록")
    class RecordRetrySuccess {
        
        @Test
        @DisplayName("재시도 성공을 기록하면 성공 카운터가 증가한다")
        void recordRetrySuccess_ShouldIncrementSuccessCounter() {
            // When
            retryMetricsTracker.recordRetrySuccess();
            retryMetricsTracker.recordRetrySuccess();
            
            // Then
            Counter counter = meterRegistry.find("metrics.collection.retry.success").counter();
            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(2.0);
        }
    }
    
    @Nested
    @DisplayName("재시도 실패 기록")
    class RecordRetryFailure {
        
        @Test
        @DisplayName("재시도 실패를 기록하면 실패 카운터가 증가한다")
        void recordRetryFailure_ShouldIncrementFailureCounter() {
            // When
            retryMetricsTracker.recordRetryFailure();
            
            // Then
            Counter counter = meterRegistry.find("metrics.collection.retry.failure").counter();
            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(1.0);
        }
    }
    
    @Nested
    @DisplayName("재시도 통계 조회")
    class GetRetryStats {
        
        @Test
        @DisplayName("재시도 통계를 정확하게 조회한다")
        void getRetryStats_ShouldReturnCorrectStats() {
            // Given
            retryMetricsTracker.recordRetryAttempt();
            retryMetricsTracker.recordRetryAttempt();
            retryMetricsTracker.recordRetryAttempt();
            retryMetricsTracker.recordRetrySuccess();
            retryMetricsTracker.recordRetrySuccess();
            retryMetricsTracker.recordRetryFailure();
            
            // When
            RetryMetricsTracker.RetryStats stats = retryMetricsTracker.getRetryStats();
            
            // Then
            assertThat(stats.getTotalAttempts()).isEqualTo(3L);
            assertThat(stats.getTotalSuccesses()).isEqualTo(2L);
            assertThat(stats.getTotalFailures()).isEqualTo(1L);
        }
        
        @Test
        @DisplayName("성공률을 정확하게 계산한다")
        void getRetryStats_ShouldCalculateSuccessRateCorrectly() {
            // Given
            retryMetricsTracker.recordRetryAttempt();
            retryMetricsTracker.recordRetryAttempt();
            retryMetricsTracker.recordRetryAttempt();
            retryMetricsTracker.recordRetryAttempt();
            retryMetricsTracker.recordRetrySuccess();
            retryMetricsTracker.recordRetrySuccess();
            retryMetricsTracker.recordRetrySuccess();
            
            // When
            RetryMetricsTracker.RetryStats stats = retryMetricsTracker.getRetryStats();
            
            // Then
            assertThat(stats.getSuccessRate()).isEqualTo(75.0); // 3/4 = 75%
        }
        
        @Test
        @DisplayName("실패율을 정확하게 계산한다")
        void getRetryStats_ShouldCalculateFailureRateCorrectly() {
            // Given
            retryMetricsTracker.recordRetryAttempt();
            retryMetricsTracker.recordRetryAttempt();
            retryMetricsTracker.recordRetryAttempt();
            retryMetricsTracker.recordRetryAttempt();
            retryMetricsTracker.recordRetryFailure();
            
            // When
            RetryMetricsTracker.RetryStats stats = retryMetricsTracker.getRetryStats();
            
            // Then
            assertThat(stats.getFailureRate()).isEqualTo(25.0); // 1/4 = 25%
        }
        
        @Test
        @DisplayName("시도 횟수가 0일 때 성공률은 0%이다")
        void getRetryStats_WhenNoAttempts_ShouldReturnZeroSuccessRate() {
            // When
            RetryMetricsTracker.RetryStats stats = retryMetricsTracker.getRetryStats();
            
            // Then
            assertThat(stats.getTotalAttempts()).isEqualTo(0L);
            assertThat(stats.getSuccessRate()).isEqualTo(0.0);
            assertThat(stats.getFailureRate()).isEqualTo(0.0);
        }
    }
    
    @Nested
    @DisplayName("메트릭 등록")
    class MetricRegistration {
        
        @Test
        @DisplayName("재시도 시도 메트릭이 올바른 태그와 함께 등록된다")
        void metricsRegistration_ShouldHaveCorrectTags() {
            // When
            retryMetricsTracker.recordRetryAttempt();
            
            // Then
            Counter counter = meterRegistry.find("metrics.collection.retry.attempts")
                    .tag("type", "retry")
                    .counter();
            assertThat(counter).isNotNull();
        }
        
        @Test
        @DisplayName("재시도 성공 메트릭이 올바른 태그와 함께 등록된다")
        void successMetricRegistration_ShouldHaveCorrectTags() {
            // When
            retryMetricsTracker.recordRetrySuccess();
            
            // Then
            Counter counter = meterRegistry.find("metrics.collection.retry.success")
                    .tag("type", "success")
                    .counter();
            assertThat(counter).isNotNull();
        }
        
        @Test
        @DisplayName("재시도 실패 메트릭이 올바른 태그와 함께 등록된다")
        void failureMetricRegistration_ShouldHaveCorrectTags() {
            // When
            retryMetricsTracker.recordRetryFailure();
            
            // Then
            Counter counter = meterRegistry.find("metrics.collection.retry.failure")
                    .tag("type", "failure")
                    .counter();
            assertThat(counter).isNotNull();
        }
    }
}

