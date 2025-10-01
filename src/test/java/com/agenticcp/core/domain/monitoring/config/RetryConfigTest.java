package com.agenticcp.core.domain.monitoring.config;

import com.agenticcp.core.domain.monitoring.service.RetryMetricsTracker;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.retry.RetryListener;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RetryConfig 단위 테스트
 * 
 * <p>Issue #39: Task 8 - 재시도 로직 및 오류 처리 구현
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@DisplayName("RetryConfig 단위 테스트")
class RetryConfigTest {
    
    private RetryConfig retryConfig;
    private RetryMetricsTracker retryMetricsTracker;
    
    @BeforeEach
    void setUp() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        retryMetricsTracker = new RetryMetricsTracker(meterRegistry);
        retryConfig = new RetryConfig(retryMetricsTracker);
    }
    
    @Nested
    @DisplayName("재시도 설정 상수")
    class RetryConfigConstants {
        
        @Test
        @DisplayName("최대 재시도 횟수는 3회이다")
        void maxRetryAttempts_ShouldBe3() {
            // Then
            assertThat(RetryConfig.MAX_RETRY_ATTEMPTS).isEqualTo(3);
        }
        
        @Test
        @DisplayName("초기 백오프 지연 시간은 1초이다")
        void initialBackoffDelay_ShouldBe1Second() {
            // Then
            assertThat(RetryConfig.INITIAL_BACKOFF_DELAY).isEqualTo(1000L);
        }
        
        @Test
        @DisplayName("백오프 배수는 2.0이다")
        void backoffMultiplier_ShouldBe2() {
            // Then
            assertThat(RetryConfig.BACKOFF_MULTIPLIER).isEqualTo(2.0);
        }
    }
    
    @Nested
    @DisplayName("RetryListener 등록")
    class RetryListenerRegistration {
        
        @Test
        @DisplayName("RetryListener Bean이 생성된다")
        void retryListener_ShouldBeCreated() {
            // When
            RetryListener listener = retryConfig.retryListener();
            
            // Then
            assertThat(listener).isNotNull();
        }
    }
}


