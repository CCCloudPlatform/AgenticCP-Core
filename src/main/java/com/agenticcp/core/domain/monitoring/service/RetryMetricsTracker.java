package com.agenticcp.core.domain.monitoring.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 재시도 메트릭 추적 서비스
 * 
 * <p>메트릭 수집 재시도 패턴을 모니터링하고 통계를 수집합니다.
 * 
 * <p>수집하는 메트릭:
 * <ul>
 *   <li>metrics.collection.retry.attempts - 재시도 시도 횟수</li>
 *   <li>metrics.collection.retry.success - 재시도 후 성공 횟수</li>
 *   <li>metrics.collection.retry.failure - 재시도 후 최종 실패 횟수</li>
 * </ul>
 * 
 * <p>Issue #39: Task 8 - 재시도 로직 및 오류 처리 구현
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@Component
public class RetryMetricsTracker {
    
    private final Counter retryAttemptsCounter;
    private final Counter retrySuccessCounter;
    private final Counter retryFailureCounter;
    
    /**
     * RetryMetricsTracker 생성자
     * 
     * <p>Micrometer MeterRegistry를 사용하여 재시도 메트릭을 등록합니다.
     * 
     * @param meterRegistry Micrometer 메트릭 레지스트리
     */
    public RetryMetricsTracker(MeterRegistry meterRegistry) {
        
        // 재시도 시도 카운터 등록
        this.retryAttemptsCounter = Counter.builder("metrics.collection.retry.attempts")
            .description("메트릭 수집 재시도 시도 횟수")
            .tag("type", "retry")
            .register(meterRegistry);
        
        // 재시도 성공 카운터 등록
        this.retrySuccessCounter = Counter.builder("metrics.collection.retry.success")
            .description("메트릭 수집 재시도 후 성공 횟수")
            .tag("type", "success")
            .register(meterRegistry);
        
        // 재시도 실패 카운터 등록
        this.retryFailureCounter = Counter.builder("metrics.collection.retry.failure")
            .description("메트릭 수집 재시도 후 최종 실패 횟수")
            .tag("type", "failure")
            .register(meterRegistry);
        
        log.info("RetryMetricsTracker가 초기화되었습니다. Micrometer 카운터가 등록되었습니다.");
    }
    
    /**
     * 재시도 시도 기록
     * 
     * <p>메트릭 수집이 실패하여 재시도할 때마다 호출됩니다.
     */
    public void recordRetryAttempt() {
        retryAttemptsCounter.increment();
        log.debug("재시도 시도 기록됨. 총 시도 횟수: {}", retryAttemptsCounter.count());
    }
    
    /**
     * 재시도 성공 기록
     * 
     * <p>재시도 후 메트릭 수집에 성공했을 때 호출됩니다.
     */
    public void recordRetrySuccess() {
        retrySuccessCounter.increment();
        log.info("재시도 성공. 총 재시도 성공 횟수: {}", retrySuccessCounter.count());
    }
    
    /**
     * 재시도 실패 기록
     * 
     * <p>모든 재시도가 실패하여 최종적으로 실패했을 때 호출됩니다.
     */
    public void recordRetryFailure() {
        retryFailureCounter.increment();
        log.error("모든 재시도 실패. 총 재시도 실패 횟수: {}", retryFailureCounter.count());
    }
    
    /**
     * 재시도 통계 조회
     * 
     * @return 재시도 통계 정보
     */
    public RetryStats getRetryStats() {
        long totalAttempts = (long) retryAttemptsCounter.count();
        long totalSuccesses = (long) retrySuccessCounter.count();
        long totalFailures = (long) retryFailureCounter.count();
        
        return new RetryStats(totalAttempts, totalSuccesses, totalFailures);
    }
    
    /**
     * 재시도 통계 DTO
     * 
     * <p>재시도 관련 통계 정보를 담는 불변 객체입니다.
     */
    @Getter
    public static class RetryStats {
        private final Long totalAttempts;
        private final Long totalSuccesses;
        private final Long totalFailures;
        
        /**
         * RetryStats 생성자
         * 
         * @param totalAttempts 총 재시도 시도 횟수
         * @param totalSuccesses 총 재시도 성공 횟수
         * @param totalFailures 총 재시도 실패 횟수
         */
        public RetryStats(Long totalAttempts, Long totalSuccesses, Long totalFailures) {
            this.totalAttempts = totalAttempts;
            this.totalSuccesses = totalSuccesses;
            this.totalFailures = totalFailures;
        }
        
        /**
         * 성공률 계산
         * 
         * @return 재시도 성공률 (0.0 ~ 100.0)
         */
        public Double getSuccessRate() {
            if (totalAttempts == 0) {
                return 0.0;
            }
            return (totalSuccesses.doubleValue() / totalAttempts.doubleValue()) * 100.0;
        }
        
        /**
         * 실패율 계산
         * 
         * @return 재시도 실패율 (0.0 ~ 100.0)
         */
        public Double getFailureRate() {
            if (totalAttempts == 0) {
                return 0.0;
            }
            return (totalFailures.doubleValue() / totalAttempts.doubleValue()) * 100.0;
        }
        
        @Override
        public String toString() {
            return String.format("RetryStats{attempts=%d, successes=%d, failures=%d, successRate=%.2f%%, failureRate=%.2f%%}",
                    totalAttempts, totalSuccesses, totalFailures, getSuccessRate(), getFailureRate());
        }
    }
}

