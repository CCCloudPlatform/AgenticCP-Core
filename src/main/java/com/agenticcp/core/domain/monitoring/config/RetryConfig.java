package com.agenticcp.core.domain.monitoring.config;

import com.agenticcp.core.domain.monitoring.service.RetryMetricsTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Spring Retry 설정
 * 
 * <p>메트릭 수집 실패 시 자동 재시도를 활성화합니다.
 * 
 * <p>재시도 정책:
 * <ul>
 *   <li>최대 재시도 횟수: 3회 (Issue #39 요구사항)</li>
 *   <li>초기 대기 시간: 1초</li>
 *   <li>백오프 배수: 2 (지수 백오프)</li>
 *   <li>재시도 간격: 1초 → 2초 → 4초</li>
 * </ul>
 * 
 * <p>Issue #39: Task 8 - 재시도 로직 및 오류 처리 구현
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@Configuration
@EnableRetry
@RequiredArgsConstructor
public class RetryConfig {
    
    /**
     * 최대 재시도 횟수
     * Issue #39 요구사항: 메트릭 수집 실패 시 재시도
     */
    public static final int MAX_RETRY_ATTEMPTS = 3;
    
    /**
     * 초기 백오프 지연 시간 (밀리초)
     */
    public static final long INITIAL_BACKOFF_DELAY = 1000L;
    
    /**
     * 백오프 배수 (지수 백오프)
     */
    public static final double BACKOFF_MULTIPLIER = 2.0;
    
    private final RetryMetricsTracker retryMetricsTracker;
    
    /**
     * RetryListener 등록
     * 
     * <p>재시도 이벤트를 감지하여 RetryMetricsTracker에 통계를 기록합니다.
     * 
     * @return RetryListener 구현체
     */
    @Bean
    public RetryListener retryListener() {
        return new RetryListener() {
            
            @Override
            public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
                // 재시도 시작 시 호출
                if (context.getRetryCount() > 0) {
                    retryMetricsTracker.recordRetryAttempt();
                    log.debug("재시도 시작: {}회차", context.getRetryCount());
                }
                return true;
            }
            
            @Override
            public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                // 재시도 종료 시 호출
                if (throwable == null && context.getRetryCount() > 0) {
                    // 재시도 후 성공
                    retryMetricsTracker.recordRetrySuccess();
                    log.info("재시도 성공: 총 {}회 시도", context.getRetryCount());
                } else if (throwable != null && context.getRetryCount() >= MAX_RETRY_ATTEMPTS) {
                    // 모든 재시도 실패
                    retryMetricsTracker.recordRetryFailure();
                    log.error("모든 재시도 실패: 총 {}회 시도", context.getRetryCount());
                }
            }
            
            @Override
            public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                // 재시도 중 오류 발생 시 호출
                log.warn("재시도 중 오류 발생: {}회차, error={}", context.getRetryCount(), throwable.getMessage());
            }
        };
    }
}

