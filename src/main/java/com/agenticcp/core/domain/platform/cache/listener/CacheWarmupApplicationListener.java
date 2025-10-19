package com.agenticcp.core.domain.platform.cache.listener;

import com.agenticcp.core.domain.platform.cache.service.FeatureFlagCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * 캐시 Warm-up 애플리케이션 리스너
 * <p>
 * 애플리케이션 시작 시 자동으로 모든 활성 기능 플래그를 캐시에 적재합니다.
 * `app.redis.enabled` 프로퍼티가 `true`일 때만 활성화됩니다.
 * </p>
 * 
 * <p>
 * <strong>동작 시점:</strong> ApplicationReadyEvent - 애플리케이션이 완전히 준비된 후 실행됩니다.
 * 이는 모든 Bean이 초기화되고 HTTP 요청을 받을 준비가 완료된 상태입니다.
 * </p>
 * 
 * <p>
 * <strong>실패 처리:</strong> Warm-up 실패 시 로그만 남기고 애플리케이션 시작은 계속 진행됩니다.
 * DB 조회를 통한 Fallback이 가능하므로 Warm-up 실패가 치명적이지 않습니다.
 * </p>
 *
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-17
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "true")
public class CacheWarmupApplicationListener implements ApplicationListener<ApplicationReadyEvent> {

    private final FeatureFlagCacheService cacheService;

    /**
     * 애플리케이션 준비 완료 이벤트 처리
     * <p>
     * 모든 활성 기능 플래그를 캐시에 적재합니다.
     * </p>
     *
     * @param event ApplicationReadyEvent
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("=================================================================");
        log.info("[CacheWarmupApplicationListener] Application ready - Starting cache warmup");
        log.info("=================================================================");

        long startTime = System.currentTimeMillis();

        try {
            int cachedCount = cacheService.warmupCache();
            long elapsedTime = System.currentTimeMillis() - startTime;

            log.info("=================================================================");
            log.info("[CacheWarmupApplicationListener] Cache warmup completed successfully");
            log.info("[CacheWarmupApplicationListener] Cached {} feature flags in {}ms", 
                    cachedCount, elapsedTime);
            log.info("=================================================================");

        } catch (Exception e) {
            long elapsedTime = System.currentTimeMillis() - startTime;

            log.error("=================================================================");
            log.error("[CacheWarmupApplicationListener] Cache warmup failed after {}ms", elapsedTime);
            log.error("[CacheWarmupApplicationListener] Error: {}", e.getMessage(), e);
            log.error("[CacheWarmupApplicationListener] Application will start without cache warmup");
            log.error("[CacheWarmupApplicationListener] Feature flags will be loaded from DB on-demand");
            log.error("=================================================================");

            // Warm-up 실패는 애플리케이션 시작을 중단시키지 않음
            // DB Fallback이 가능하므로 계속 진행
        }
    }
}

