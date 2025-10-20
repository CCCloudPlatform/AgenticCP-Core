package com.agenticcp.core.domain.platform.cache.service;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.platform.cache.config.RedisCacheConfig.FeatureFlagCacheProperties;
import com.agenticcp.core.domain.platform.cache.dto.FeatureFlagCacheDto;
import com.agenticcp.core.domain.platform.entity.FeatureFlag;
import com.agenticcp.core.domain.platform.exception.FeatureFlagCacheErrorCode;
import com.agenticcp.core.domain.platform.service.FeatureFlagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 기능 플래그 캐시 서비스
 * Redis 캐시를 통한 플래그 조회/저장/무효화 및 분산 락 기능 제공
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-17
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "true")
public class FeatureFlagCacheService {

    private static final String CACHE_NAME = "featureFlags";
    private static final String LOCK_PREFIX = "agenticcp:ff:lock:";
    private static final int LOCK_WAIT_TIME = 10; // 초
    private static final int LOCK_LEASE_TIME = 30; // 초

    private final CacheManager cacheManager;
    private final RedissonClient redissonClient;
    private final FeatureFlagService featureFlagService;
    private final FeatureFlagCacheProperties cacheProperties;

    /**
     * 캐시에서 플래그 조회 (Fallback 포함)
     * 캐시 미스 또는 예외 시 DB에서 조회
     * 
     * @param flagKey 플래그 키
     * @return 캐시된 플래그 DTO (없으면 null)
     */
    public FeatureFlagCacheDto getFromCache(String flagKey) {
        try {
            Cache cache = getCache();
            FeatureFlagCacheDto cached = cache.get(flagKey, FeatureFlagCacheDto.class);
            
            if (cached != null) {
                log.debug("[FeatureFlagCacheService] Cache HIT - flagKey={}", flagKey);
                return cached;
            }
            
            log.debug("[FeatureFlagCacheService] Cache MISS - flagKey={}, loading from DB", flagKey);
            return loadAndCache(flagKey);
            
        } catch (Exception e) {
            log.warn("[FeatureFlagCacheService] Cache error, fallback to DB - flagKey={}, error={}", 
                    flagKey, e.getMessage());
            return loadFromDatabase(flagKey);
        }
    }

    /**
     * 캐시에 플래그 저장 (개별 TTL 적용)
     * TTL 범위 검증 후 저장
     * 
     * @param featureFlag 저장할 플래그
     * @throws BusinessException TTL이 유효하지 않을 때
     */
    public void putToCache(FeatureFlag featureFlag) {
        try {
            FeatureFlagCacheDto cacheDto = FeatureFlagCacheDto.from(featureFlag);
            
            // TTL 유효성 검증
            if (!cacheDto.isValidTtl(cacheProperties.getMinTtlSeconds(), 
                                     cacheProperties.getMaxTtlSeconds())) {
                throw new BusinessException(FeatureFlagCacheErrorCode.INVALID_CACHE_TTL);
            }
            
            Cache cache = getCache();
            cache.put(featureFlag.getFlagKey(), cacheDto);
            
            log.debug("[FeatureFlagCacheService] Cache PUT - flagKey={}, ttl={}s", 
                    featureFlag.getFlagKey(), 
                    cacheDto.getEffectiveTtl(cacheProperties.getDefaultTtlSeconds()));
                    
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[FeatureFlagCacheService] Cache PUT failed - flagKey={}, error={}", 
                    featureFlag.getFlagKey(), e.getMessage());
            throw new BusinessException(FeatureFlagCacheErrorCode.CACHE_OPERATION_FAILED);
        }
    }

    /**
     * 분산 락을 사용하여 플래그 업데이트 및 캐시 갱신
     * 
     * @param flagKey 플래그 키
     * @param updatedFlag 업데이트할 플래그
     * @throws BusinessException 락 획득 실패 시
     */
    public void updateWithLock(String flagKey, FeatureFlag updatedFlag) {
        String lockKey = LOCK_PREFIX + flagKey;
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            boolean acquired = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
            
            if (!acquired) {
                log.warn("[FeatureFlagCacheService] Failed to acquire lock - flagKey={}", flagKey);
                throw new BusinessException(FeatureFlagCacheErrorCode.DISTRIBUTED_LOCK_TIMEOUT);
            }
            
            log.debug("[FeatureFlagCacheService] Lock acquired - flagKey={}", flagKey);
            
            try {
                // 캐시 무효화 후 재저장 (예외 전파)
                Cache cache = getCache();
                cache.evict(flagKey);
                log.debug("[FeatureFlagCacheService] Cache evicted - flagKey={}", flagKey);
                
                FeatureFlagCacheDto dto = FeatureFlagCacheDto.from(updatedFlag);
                int effectiveTtl = dto.getEffectiveTtl(cacheProperties.getDefaultTtlSeconds());
                
                if (!dto.isValidTtl(cacheProperties.getMinTtlSeconds(), cacheProperties.getMaxTtlSeconds())) {
                    log.error("[FeatureFlagCacheService] Invalid TTL value - flagKey={}, ttl={}s", flagKey, effectiveTtl);
                    throw new BusinessException(FeatureFlagCacheErrorCode.INVALID_CACHE_TTL);
                }
                
                cache.put(flagKey, dto);
                log.info("[FeatureFlagCacheService] Cache updated with lock - flagKey={}, ttl={}s", flagKey, effectiveTtl);
                
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("[FeatureFlagCacheService] Cache operation failed - flagKey={}, error={}", 
                        flagKey, e.getMessage());
                throw new BusinessException(FeatureFlagCacheErrorCode.CACHE_OPERATION_FAILED);
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                    log.debug("[FeatureFlagCacheService] Lock released - flagKey={}", flagKey);
                }
            }
            
        } catch (BusinessException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[FeatureFlagCacheService] Lock interrupted - flagKey={}", flagKey);
            throw new BusinessException(FeatureFlagCacheErrorCode.DISTRIBUTED_LOCK_FAILED);
        }
    }

    /**
     * 특정 플래그 캐시 무효화
     * 
     * @param flagKey 플래그 키
     */
    public void invalidateCache(String flagKey) {
        try {
            Cache cache = getCache();
            cache.evict(flagKey);
            log.debug("[FeatureFlagCacheService] Cache evicted - flagKey={}", flagKey);
            
        } catch (Exception e) {
            log.warn("[FeatureFlagCacheService] Cache eviction failed - flagKey={}, error={}", 
                    flagKey, e.getMessage());
            // 무효화 실패는 예외를 던지지 않음 (로그만 기록)
        }
    }

    /**
     * 전체 캐시 무효화
     */
    public void invalidateAllCache() {
        try {
            Cache cache = getCache();
            cache.clear();
            log.info("[FeatureFlagCacheService] All cache cleared");
            
        } catch (Exception e) {
            log.warn("[FeatureFlagCacheService] Cache clear failed - error={}", e.getMessage());
            // 무효화 실패는 예외를 던지지 않음
        }
    }

    /**
     * 모든 플래그를 캐시에 적재 (Warm-up)
     * 
     * @return 캐싱된 플래그 수
     */
    public int warmupCache() {
        log.info("[FeatureFlagCacheService] Cache warm-up started");
        
        try {
            List<FeatureFlag> allFlags = featureFlagService.getAllFlags();
            int cached = 0;
            
            for (FeatureFlag flag : allFlags) {
                try {
                    putToCache(flag);
                    cached++;
                } catch (Exception e) {
                    log.warn("[FeatureFlagCacheService] Failed to cache flag during warm-up - flagKey={}, error={}", 
                            flag.getFlagKey(), e.getMessage());
                    // 일부 실패해도 계속 진행
                }
            }
            
            log.info("[FeatureFlagCacheService] Cache warm-up completed - total={}, cached={}", 
                    allFlags.size(), cached);
            return cached;
            
        } catch (Exception e) {
            log.error("[FeatureFlagCacheService] Cache warm-up failed - error={}", e.getMessage());
            throw new BusinessException(FeatureFlagCacheErrorCode.CACHE_WARMUP_FAILED);
        }
    }

    /**
     * Cache 인스턴스 조회
     * 
     * @return Cache
     * @throws BusinessException 캐시를 사용할 수 없을 때
     */
    private Cache getCache() {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache == null) {
            log.error("[FeatureFlagCacheService] Cache not available - cacheName={}", CACHE_NAME);
            throw new BusinessException(FeatureFlagCacheErrorCode.CACHE_UNAVAILABLE);
        }
        return cache;
    }

    /**
     * DB에서 플래그 조회 후 캐시에 저장
     * 
     * @param flagKey 플래그 키
     * @return 플래그 DTO (없으면 null)
     */
    private FeatureFlagCacheDto loadAndCache(String flagKey) {
        FeatureFlagCacheDto dto = loadFromDatabase(flagKey);
        
        if (dto != null) {
            try {
                Cache cache = getCache();
                cache.put(flagKey, dto);
                log.debug("[FeatureFlagCacheService] Loaded from DB and cached - flagKey={}", flagKey);
            } catch (Exception e) {
                log.warn("[FeatureFlagCacheService] Failed to cache after DB load - flagKey={}, error={}", 
                        flagKey, e.getMessage());
                // 캐싱 실패해도 DTO는 반환
            }
        }
        
        return dto;
    }

    /**
     * DB에서 플래그 조회
     * 
     * @param flagKey 플래그 키
     * @return 플래그 DTO (없으면 null)
     */
    private FeatureFlagCacheDto loadFromDatabase(String flagKey) {
        try {
            Optional<FeatureFlag> flagOpt = featureFlagService.getFlagByKey(flagKey);
            return flagOpt.map(FeatureFlagCacheDto::from).orElse(null);
            
        } catch (Exception e) {
            log.error("[FeatureFlagCacheService] Failed to load from DB - flagKey={}, error={}", 
                    flagKey, e.getMessage());
            return null;
        }
    }
}

