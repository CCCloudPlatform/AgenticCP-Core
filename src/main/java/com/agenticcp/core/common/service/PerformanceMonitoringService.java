package com.agenticcp.core.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 성능 모니터링 서비스
 * JWT 인증 관련 성능 요구사항 측정 및 모니터링
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PerformanceMonitoringService {

    private final RedisTemplate<String, String> redisTemplate;

    // 빈 주입
    @Autowired
    @Qualifier("tenantTaskExecutor")
    private TaskExecutor tenantTaskExecutor;
    
    private static final String LOGIN_PERFORMANCE_PREFIX = "perf:login:";
    private static final String TOKEN_REFRESH_PERFORMANCE_PREFIX = "perf:refresh:";
    private static final String REDIS_PERFORMANCE_PREFIX = "perf:redis:";
    private static final long LOGIN_THRESHOLD_MS = 200L; // 200ms
    private static final long REDIS_THRESHOLD_MS = 50L; // 50ms

    /**
     * 로그인 성능 측정
     */
    public <T> PerformanceMetrics measureLoginPerformance(String username, Supplier<T> loginOperation) {
        Instant start = Instant.now();
        
        try {
            T result = loginOperation.get();
            Instant end = Instant.now();
            long duration = Duration.between(start, end).toMillis();
            
            PerformanceMetrics metrics = new PerformanceMetrics(
                    "LOGIN", username, duration, duration <= LOGIN_THRESHOLD_MS
            );
            
            logPerformanceMetrics(metrics);
            storePerformanceMetrics(LOGIN_PERFORMANCE_PREFIX + username, metrics);
            
            return metrics;
        } catch (Exception e) {
            Instant end = Instant.now();
            long duration = Duration.between(start, end).toMillis();
            
            PerformanceMetrics metrics = new PerformanceMetrics(
                    "LOGIN", username, duration, false, e.getMessage()
            );
            
            logPerformanceMetrics(metrics);
            return metrics;
        }
    }

    /**
     * 토큰 갱신 성능 측정
     */
    public <T> PerformanceMetrics measureTokenRefreshPerformance(String username, Supplier<T> refreshOperation) {
        Instant start = Instant.now();
        
        try {
            T result = refreshOperation.get();
            Instant end = Instant.now();
            long duration = Duration.between(start, end).toMillis();
            
            PerformanceMetrics metrics = new PerformanceMetrics(
                    "TOKEN_REFRESH", username, duration, duration <= LOGIN_THRESHOLD_MS
            );
            
            logPerformanceMetrics(metrics);
            storePerformanceMetrics(TOKEN_REFRESH_PERFORMANCE_PREFIX + username, metrics);
            
            return metrics;
        } catch (Exception e) {
            Instant end = Instant.now();
            long duration = Duration.between(start, end).toMillis();
            
            PerformanceMetrics metrics = new PerformanceMetrics(
                    "TOKEN_REFRESH", username, duration, false, e.getMessage()
            );
            
            logPerformanceMetrics(metrics);
            return metrics;
        }
    }

    /**
     * Redis 작업 성능 측정
     */
    public <T> PerformanceMetrics measureRedisPerformance(String operation, Supplier<T> redisOperation) {
        Instant start = Instant.now();
        
        try {
            T result = redisOperation.get();
            Instant end = Instant.now();
            long duration = Duration.between(start, end).toMillis();
            
            PerformanceMetrics metrics = new PerformanceMetrics(
                    "REDIS_" + operation, "system", duration, duration <= REDIS_THRESHOLD_MS
            );
            
            if (duration > REDIS_THRESHOLD_MS) {
                log.warn("Redis 성능 저하 감지: operation={}, duration={}ms", operation, duration);
            }
            
            return metrics;
        } catch (Exception e) {
            Instant end = Instant.now();
            long duration = Duration.between(start, end).toMillis();
            
            PerformanceMetrics metrics = new PerformanceMetrics(
                    "REDIS_" + operation, "system", duration, false, e.getMessage()
            );
            
            log.error("Redis 작업 실패: operation={}, duration={}ms, error={}", 
                    operation, duration, e.getMessage());
            
            return metrics;
        }
    }

    /**
     * 비동기 성능 측정
     */
    public <T> CompletableFuture<PerformanceMetrics> measureAsyncPerformance(
            String operation, String identifier, Supplier<T> operationToMeasure) {
        
        return CompletableFuture.supplyAsync(() -> {
            Instant start = Instant.now();
            
            try {
                T result = operationToMeasure.get();
                Instant end = Instant.now();
                long duration = Duration.between(start, end).toMillis();
                
                PerformanceMetrics metrics = new PerformanceMetrics(
                        operation, identifier, duration, true
                );
                
                logPerformanceMetrics(metrics);
                return metrics;
            } catch (Exception e) {
                Instant end = Instant.now();
                long duration = Duration.between(start, end).toMillis();
                
                PerformanceMetrics metrics = new PerformanceMetrics(
                        operation, identifier, duration, false, e.getMessage()
                );
                
                logPerformanceMetrics(metrics);
                return metrics;
            }
        }, tenantTaskExecutor);
    }

    /**
     * 성능 메트릭 저장
     */
    private void storePerformanceMetrics(String key, PerformanceMetrics metrics) {
        try {
            redisTemplate.opsForValue().set(
                    key,
                    metrics.toJson(),
                    Duration.ofHours(24) // 24시간 보관
            );
        } catch (Exception e) {
            log.warn("성능 메트릭 저장 실패: {}", e.getMessage());
        }
    }

    /**
     * 성능 메트릭 로깅
     */
    private void logPerformanceMetrics(PerformanceMetrics metrics) {
        if (metrics.isSuccess() && metrics.getDuration() <= getThresholdForOperation(metrics.getOperation())) {
            log.debug("성능 메트릭: operation={}, identifier={}, duration={}ms", 
                    metrics.getOperation(), metrics.getIdentifier(), metrics.getDuration());
        } else if (!metrics.isSuccess()) {
            log.error("성능 메트릭 (실패): operation={}, identifier={}, duration={}ms, error={}", 
                    metrics.getOperation(), metrics.getIdentifier(), metrics.getDuration(), metrics.getError());
        } else {
            log.warn("성능 임계값 초과: operation={}, identifier={}, duration={}ms (임계값: {}ms)", 
                    metrics.getOperation(), metrics.getIdentifier(), metrics.getDuration(), 
                    getThresholdForOperation(metrics.getOperation()));
        }
    }

    /**
     * 작업별 임계값 조회
     */
    private long getThresholdForOperation(String operation) {
        if (operation.startsWith("REDIS_")) {
            return REDIS_THRESHOLD_MS;
        }
        return LOGIN_THRESHOLD_MS;
    }

    /**
     * 성능 메트릭 클래스
     */
    public static class PerformanceMetrics {
        private final String operation;
        private final String identifier;
        private final long duration;
        private final boolean success;
        private final String error;
        private final Instant timestamp;
        private Object result;

        public PerformanceMetrics(String operation, String identifier, long duration, boolean success) {
            this(operation, identifier, duration, success, null);
        }

        public PerformanceMetrics(String operation, String identifier, long duration, boolean success, String error) {
            this.operation = operation;
            this.identifier = identifier;
            this.duration = duration;
            this.success = success;
            this.error = error;
            this.timestamp = Instant.now();
        }

        public String getOperation() { return operation; }
        public String getIdentifier() { return identifier; }
        public long getDuration() { return duration; }
        public boolean isSuccess() { return success; }
        public String getError() { return error; }
        public Instant getTimestamp() { return timestamp; }
        
        @SuppressWarnings("unchecked")
        public <T> T getResult() { return (T) result; }
        public void setResult(Object result) { this.result = result; }

        public String toJson() {
            return String.format(
                    "{\"operation\":\"%s\",\"identifier\":\"%s\",\"duration\":%d,\"success\":%b,\"error\":\"%s\",\"timestamp\":\"%s\"}",
                    operation, identifier, duration, success, error != null ? error : "", timestamp.toString()
            );
        }
    }
}
