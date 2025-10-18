package com.agenticcp.core.domain.monitoring.health.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class HealthCheckCacheService {
    
    private final CacheManager cacheManager;
    
    public void evictHealthCheckCache() {
        log.info("Evicting health check cache");
        Cache healthCheckCache = cacheManager.getCache("healthCheck");
        if (healthCheckCache != null) {
            healthCheckCache.clear();
            log.info("Health check cache cleared");
        }
    }
    
    public void evictComponentCache(String componentName) {
        log.info("Evicting cache for component: {}", componentName);
        Cache healthCheckCache = cacheManager.getCache("healthCheck");
        if (healthCheckCache != null) {
            healthCheckCache.evict(componentName);
            log.info("Cache evicted for component: {}", componentName);
        }
    }
    
    public void evictOverallCache() {
        log.info("Evicting overall health check cache");
        Cache healthCheckCache = cacheManager.getCache("healthCheck");
        if (healthCheckCache != null) {
            healthCheckCache.evict("overall");
            log.info("Overall health check cache evicted");
        }
    }
}

