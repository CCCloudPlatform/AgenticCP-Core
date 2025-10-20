package com.agenticcp.core.domain.platform.cache.service;

import com.agenticcp.core.domain.platform.cache.dto.CacheHealthStatusDto;
import com.agenticcp.core.domain.platform.cache.dto.CacheMetricsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 기능 플래그 캐시 헬스체크 서비스
 * <p>
 * Redis 캐시의 상태를 모니터링하고, 연속 3회 실패 시 Fallback 모드로 전환합니다.
 * 캐시 히트율, 응답 시간 등의 메트릭을 수집합니다.
 * </p>
 *
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-17
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "true")
public class FeatureFlagCacheHealthService {

    private static final String CACHE_NAME = "feature-flags";
    private static final int FALLBACK_THRESHOLD = 3; // 연속 실패 임계값

    private final CacheManager cacheManager;
    private final RedisConnectionFactory redisConnectionFactory;

    // 헬스체크 상태
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private volatile boolean fallbackMode = false;
    private volatile LocalDateTime lastCheckTime = LocalDateTime.now();

    // 메트릭
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong totalResponseTime = new AtomicLong(0);
    private final AtomicLong fallbackCount = new AtomicLong(0);

    /**
     * Redis 캐시 헬스체크 수행
     * <p>
     * 연속 3회 실패 시 Fallback 모드로 전환됩니다.
     * </p>
     *
     * @return 캐시 헬스 상태
     */
    public CacheHealthStatusDto checkHealth() {
        lastCheckTime = LocalDateTime.now();

        try {
            // 1. 캐시 매니저에서 캐시 조회
            Cache cache = cacheManager.getCache(CACHE_NAME);
            if (cache == null) {
                handleHealthCheckFailure();
                return CacheHealthStatusDto.error(
                        "캐시를 찾을 수 없습니다: " + CACHE_NAME,
                        consecutiveFailures.get()
                );
            }

            // 2. Redis 연결 확인
            RedisConnection connection = redisConnectionFactory.getConnection();
            connection.close();

            // 3. 헬스체크 성공
            handleHealthCheckSuccess();

            if (fallbackMode) {
                return CacheHealthStatusDto.recovering();
            }

            return CacheHealthStatusDto.healthy();

        } catch (Exception e) {
            log.warn("[FeatureFlagCacheHealthService] Health check failed: {}", e.getMessage());
            handleHealthCheckFailure();
            
            if (fallbackMode) {
                return CacheHealthStatusDto.fallback(consecutiveFailures.get());
            }

            return CacheHealthStatusDto.error(
                    "헬스체크 실패: " + e.getMessage(),
                    consecutiveFailures.get()
            );
        }
    }

    /**
     * 헬스체크 성공 처리
     * <p>
     * 연속 실패 카운트를 리셋하고, Fallback 모드를 해제합니다.
     * </p>
     */
    private void handleHealthCheckSuccess() {
        int previousFailures = consecutiveFailures.getAndSet(0);
        
        if (fallbackMode) {
            fallbackMode = false;
            log.info("[FeatureFlagCacheHealthService] Fallback 모드 해제 - 이전 실패 횟수: {}", previousFailures);
        }
    }

    /**
     * 헬스체크 실패 처리
     * <p>
     * 연속 실패 카운트를 증가시키고, 임계값 도달 시 Fallback 모드로 전환합니다.
     * </p>
     */
    private void handleHealthCheckFailure() {
        int failures = consecutiveFailures.incrementAndGet();
        
        if (failures >= FALLBACK_THRESHOLD && !fallbackMode) {
            fallbackMode = true;
            log.error("[FeatureFlagCacheHealthService] Fallback 모드 전환 - 연속 실패 횟수: {}", failures);
        }
    }

    /**
     * Fallback 모드 여부 확인
     *
     * @return Fallback 모드이면 true
     */
    public boolean isFallbackMode() {
        return fallbackMode;
    }

    /**
     * 캐시 히트 기록
     *
     * @param responseTimeMs 응답 시간 (밀리초)
     */
    public void recordCacheHit(long responseTimeMs) {
        totalRequests.incrementAndGet();
        cacheHits.incrementAndGet();
        totalResponseTime.addAndGet(responseTimeMs);
        
        log.debug("[FeatureFlagCacheHealthService] Cache hit recorded - responseTime={}ms", responseTimeMs);
    }

    /**
     * 캐시 미스 기록
     *
     * @param responseTimeMs 응답 시간 (밀리초)
     */
    public void recordCacheMiss(long responseTimeMs) {
        totalRequests.incrementAndGet();
        cacheMisses.incrementAndGet();
        totalResponseTime.addAndGet(responseTimeMs);
        
        log.debug("[FeatureFlagCacheHealthService] Cache miss recorded - responseTime={}ms", responseTimeMs);
    }

    /**
     * Fallback 발생 기록
     */
    public void recordFallback() {
        fallbackCount.incrementAndGet();
        log.debug("[FeatureFlagCacheHealthService] Fallback occurred - total={}", fallbackCount.get());
    }

    /**
     * 캐시 메트릭 조회
     *
     * @return 캐시 메트릭
     */
    public CacheMetricsDto getMetrics() {
        long total = totalRequests.get();
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long totalTime = totalResponseTime.get();
        long fallbacks = fallbackCount.get();

        double hitRate = total > 0 ? (hits * 100.0 / total) : 0.0;
        double avgResponseTime = total > 0 ? (totalTime * 1.0 / total) : 0.0;

        return CacheMetricsDto.builder()
                .totalRequests(total)
                .cacheHits(hits)
                .cacheMisses(misses)
                .hitRate(hitRate)
                .avgResponseTimeMs(avgResponseTime)
                .fallbackCount(fallbacks)
                .build();
    }

    /**
     * 메트릭 리셋
     */
    public void resetMetrics() {
        totalRequests.set(0);
        cacheHits.set(0);
        cacheMisses.set(0);
        totalResponseTime.set(0);
        fallbackCount.set(0);
        
        log.info("[FeatureFlagCacheHealthService] Metrics reset");
    }
}

