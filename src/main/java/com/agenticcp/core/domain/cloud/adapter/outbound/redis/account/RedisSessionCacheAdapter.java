package com.agenticcp.core.domain.cloud.adapter.outbound.redis.account;

import com.agenticcp.core.domain.cloud.adapter.outbound.aws.account.AwsSessionCredential;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.outbound.account.SessionCachePort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Redis 기반 세션 캐시 어댑터
 */
@Slf4j
@Component
public class RedisSessionCacheAdapter implements SessionCachePort {

    private static final String CACHE_KEY_PREFIX = "cloud:session:";
    private static final int DEFAULT_TTL_MINUTES = 55;

    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    public RedisSessionCacheAdapter(ObjectMapper objectMapper,
                                    @Autowired(required = false) RedisTemplate<String, Object> redisTemplate) {
        if (ObjectMapper.findModules().stream().noneMatch(m -> m instanceof JavaTimeModule)) {
            objectMapper.registerModule(new JavaTimeModule());
        }
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void cacheSession(String tenantKey, String accountScope, ProviderType providerType,
                             CloudSessionCredential session, int ttlMinutes) {
        if (redisTemplate == null) {
            log.debug("[RedisSessionCacheAdapter] Redis not configured, skipping cache");
            return;
        }

        try {
            String cacheKey = buildCacheKey(tenantKey, accountScope, providerType);
            String sessionJson = objectMapper.writeValueAsString(session);
            redisTemplate.opsForValue().set(cacheKey, sessionJson, ttlMinutes, TimeUnit.MINUTES);
            log.debug("[RedisSessionCacheAdapter] Session cached - key={}, ttl={}m", cacheKey, ttlMinutes);
        } catch (JsonProcessingException e) {
            log.error("[RedisSessionCacheAdapter] Failed to serialize session", e);
            throw new RuntimeException("Failed to cache session", e);
        }
    }

    @Override
    public Optional<CloudSessionCredential> getCachedSession(String tenantKey, String accountScope,
                                                             ProviderType providerType) {
        if (redisTemplate == null) {
            log.debug("[RedisSessionCacheAdapter] Redis not configured, returning empty");
            return Optional.empty();
        }

        try {
            String cacheKey = buildCacheKey(tenantKey, accountScope, providerType);
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached == null) {
                log.debug("[RedisSessionCacheAdapter] No cached session found - key={}", cacheKey);
                return Optional.empty();
            }

            String sessionJson = cached.toString();
            CloudSessionCredential session = deserializeSession(sessionJson, providerType);

            if (session != null && session.isValid()) {
                log.debug("[RedisSessionCacheAdapter] Cached session found and valid - key={}", cacheKey);
                return Optional.of(session);
            } else {
                log.debug("[RedisSessionCacheAdapter] Cached session expired - key={}", cacheKey);
                redisTemplate.delete(cacheKey);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("[RedisSessionCacheAdapter] Failed to deserialize session", e);
            return Optional.empty();
        }
    }

    @Override
    public void evictSession(String tenantKey, String accountScope, ProviderType providerType) {
        if (redisTemplate == null) {
            return;
        }
        String cacheKey = buildCacheKey(tenantKey, accountScope, providerType);
        redisTemplate.delete(cacheKey);
        log.debug("[RedisSessionCacheAdapter] Session evicted - key={}", cacheKey);
    }

    @Override
    public int getDefaultTtlMinutes() {
        return DEFAULT_TTL_MINUTES;
    }

    private String buildCacheKey(String tenantKey, String accountScope, ProviderType providerType) {
        return String.format("%s%s:%s:%s", CACHE_KEY_PREFIX, tenantKey, providerType.name(), accountScope);
    }

    private CloudSessionCredential deserializeSession(String sessionJson, ProviderType providerType) {
        try {
            switch (providerType) {
                case AWS:
                    return objectMapper.readValue(sessionJson, AwsSessionCredential.class);
                default:
                    log.warn("[RedisSessionCacheAdapter] Unknown provider type for deserialization: {}", providerType);
                    return null;
            }
        } catch (JsonProcessingException e) {
            log.error("[RedisSessionCacheAdapter] Failed to deserialize session for provider: {}", providerType, e);
            return null;
        }
    }
}

