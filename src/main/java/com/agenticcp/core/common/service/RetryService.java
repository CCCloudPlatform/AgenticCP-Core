package com.agenticcp.core.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * 재시도 서비스
 * 토큰 갱신 실패 시 재시도 로직 제공
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RetryService {

    private final RedisTemplate<String, String> redisTemplate;
    
    private static final String RETRY_COUNT_PREFIX = "retry:count:";
    private static final String RETRY_BACKOFF_PREFIX = "retry:backoff:";
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long BASE_DELAY_MS = 1000L; // 1초
    private static final long MAX_DELAY_MS = 10000L; // 10초

    // 빈 주입
    @Autowired
    @Qualifier("tenantTaskExecutor")
    private TaskExecutor tenantTaskExecutor;

    /**
     * 토큰 갱신 재시도 로직
     */
    public <T> T retryTokenRefresh(String username, Supplier<T> operation) {
        return retryWithBackoff(
                "TOKEN_REFRESH",
                username,
                operation,
                MAX_RETRY_ATTEMPTS,
                BASE_DELAY_MS,
                MAX_DELAY_MS
        );
    }

    /**
     * 일반적인 재시도 로직
     */
    public <T> T retryWithBackoff(String operation, String identifier, Supplier<T> operationToRetry, 
                                 int maxAttempts, long baseDelayMs, long maxDelayMs) {
        
        int attempt = 1;
        Exception lastException = null;
        
        while (attempt <= maxAttempts) {
            try {
                log.debug("재시도 시도: operation={}, identifier={}, attempt={}/{}", 
                        operation, identifier, attempt, maxAttempts);
                
                T result = operationToRetry.get();
                
                // 성공 시 재시도 카운트 초기화
                resetRetryCount(operation, identifier);
                
                if (attempt > 1) {
                    log.info("재시도 성공: operation={}, identifier={}, attempt={}", 
                            operation, identifier, attempt);
                }
                
                return result;
                
            } catch (Exception e) {
                lastException = e;
                log.warn("재시도 실패: operation={}, identifier={}, attempt={}/{}, error={}", 
                        operation, identifier, attempt, maxAttempts, e.getMessage());
                
                if (attempt == maxAttempts) {
                    break;
                }
                
                // 백오프 지연
                long delay = calculateBackoffDelay(attempt, baseDelayMs, maxDelayMs);
                incrementRetryCount(operation, identifier);
                
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("재시도 중 인터럽트 발생", ie);
                }
                
                attempt++;
            }
        }
        
        // 최대 재시도 횟수 초과
        log.error("최대 재시도 횟수 초과: operation={}, identifier={}, attempts={}", 
                operation, identifier, maxAttempts);
        
        throw new RuntimeException(
                String.format("재시도 실패: operation=%s, identifier=%s, attempts=%d", 
                        operation, identifier, maxAttempts), 
                lastException
        );
    }

    /**
     * 비동기 재시도 로직
     */
    public <T> CompletableFuture<T> retryAsync(String operation, String identifier, 
                                             Supplier<T> operationToRetry, int maxAttempts) {
        
        return CompletableFuture.supplyAsync(() -> 
                retryWithBackoff(operation,
                        identifier,
                        operationToRetry,
                        maxAttempts,
                        BASE_DELAY_MS,
                        MAX_DELAY_MS),
                tenantTaskExecutor // 커스텀 executor
        );
    }


    /**
     * 지수 백오프 지연 계산
     */
    private long calculateBackoffDelay(int attempt, long baseDelayMs, long maxDelayMs) {
        // 지수 백오프: baseDelay * 2^(attempt-1)
        long delay = baseDelayMs * (1L << (attempt - 1));
        
        // 최대 지연 시간 제한
        delay = Math.min(delay, maxDelayMs);
        
        // 지터 추가 (±25% 랜덤 변동)
        double jitter = 0.75 + (Math.random() * 0.5); // 0.75 ~ 1.25
        delay = (long) (delay * jitter);
        
        return delay;
    }

    /**
     * 재시도 횟수 증가
     */
    private void incrementRetryCount(String operation, String identifier) {
        String key = RETRY_COUNT_PREFIX + operation + ":" + identifier;
        try {
            String count = redisTemplate.opsForValue().get(key);
            int currentCount = count != null ? Integer.parseInt(count) : 0;
            redisTemplate.opsForValue().set(key, String.valueOf(currentCount + 1), Duration.ofHours(1));
        } catch (Exception e) {
            log.warn("재시도 횟수 증가 실패: {}", e.getMessage());
        }
    }

    /**
     * 재시도 횟수 조회
     */
    public int getRetryCount(String operation, String identifier) {
        String key = RETRY_COUNT_PREFIX + operation + ":" + identifier;
        try {
            String count = redisTemplate.opsForValue().get(key);
            return count != null ? Integer.parseInt(count) : 0;
        } catch (Exception e) {
            log.warn("재시도 횟수 조회 실패: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * 재시도 횟수 초기화
     */
    private void resetRetryCount(String operation, String identifier) {
        String key = RETRY_COUNT_PREFIX + operation + ":" + identifier;
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("재시도 횟수 초기화 실패: {}", e.getMessage());
        }
    }

    /**
     * 백오프 상태 설정
     */
    public void setBackoffState(String operation, String identifier, long backoffMs) {
        String key = RETRY_BACKOFF_PREFIX + operation + ":" + identifier;
        try {
            redisTemplate.opsForValue().set(key, "true", Duration.ofMillis(backoffMs));
        } catch (Exception e) {
            log.warn("백오프 상태 설정 실패: {}", e.getMessage());
        }
    }

    /**
     * 백오프 상태 확인
     */
    public boolean isInBackoffState(String operation, String identifier) {
        String key = RETRY_BACKOFF_PREFIX + operation + ":" + identifier;
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.warn("백오프 상태 확인 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 백오프 상태 해제
     */
    public void clearBackoffState(String operation, String identifier) {
        String key = RETRY_BACKOFF_PREFIX + operation + ":" + identifier;
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("백오프 상태 해제 실패: {}", e.getMessage());
        }
    }
}
