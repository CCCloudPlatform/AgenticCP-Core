package com.agenticcp.core.domain.cloud.service.account;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.account.AwsSessionCredential;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 세션 캐시 서비스
 * 
 * 단기 세션/토큰을 Redis에 캐싱하고 조회하는 기능을 제공합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Service
public class SessionCacheService {
    
    private static final String CACHE_KEY_PREFIX = "cloud:session:";
    private static final int DEFAULT_TTL_MINUTES = 55; // 세션 만료 전 미리 갱신하기 위해 55분
    
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    
    public SessionCacheService(ObjectMapper objectMapper,
                               @Autowired(required = false) RedisTemplate<String, Object> redisTemplate) {
        // ObjectMapper에 JavaTimeModule이 이미 등록되어 있을 수 있으므로 확인 후 등록
        if (objectMapper.findModules().stream().noneMatch(m -> m instanceof JavaTimeModule)) {
            objectMapper.registerModule(new JavaTimeModule());
        }
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 알 수 없는 필드는 무시하도록 설정 (예: providerType, valid 등)
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * 세션을 Redis에 캐싱합니다.
     * 
     * @param tenantKey 테넌트 키
     * @param accountScope 계정 범위 (AWS Account ID, Azure Subscription ID, GCP Project ID)
     * @param providerType 프로바이더 타입
     * @param session 세션 자격증명
     * @param ttlMinutes TTL (분)
     */
    public void cacheSession(String tenantKey, String accountScope, ProviderType providerType,
                             CloudSessionCredential session, int ttlMinutes) {
        if (redisTemplate == null) {
            log.debug("[SessionCacheService] Redis not configured, skipping cache");
            return;
        }
        
        try {
            String cacheKey = buildCacheKey(tenantKey, accountScope, providerType);
            String sessionJson = objectMapper.writeValueAsString(session);
            
            redisTemplate.opsForValue().set(cacheKey, sessionJson, ttlMinutes, TimeUnit.MINUTES);
            log.debug("[SessionCacheService] Session cached - key={}, ttl={}m", cacheKey, ttlMinutes);
        } catch (JsonProcessingException e) {
            log.error("[SessionCacheService] Failed to serialize session", e);
            throw new RuntimeException("Failed to cache session", e);
        }
    }
    
    /**
     * 세션을 Redis에서 조회합니다.
     * 
     * @param tenantKey 테넌트 키
     * @param accountScope 계정 범위 (AWS Account ID, Azure Subscription ID, GCP Project ID)
     * @param providerType 프로바이더 타입
     * @return Optional<CloudSessionCredential>
     */
    public Optional<CloudSessionCredential> getCachedSession(String tenantKey, String accountScope, ProviderType providerType) {
        if (redisTemplate == null) {
            log.debug("[SessionCacheService] Redis not configured, returning empty");
            return Optional.empty();
        }
        
        try {
            String cacheKey = buildCacheKey(tenantKey, accountScope, providerType);
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            
            if (cached == null) {
                log.debug("[SessionCacheService] No cached session found - key={}", cacheKey);
                return Optional.empty();
            }
            
            String sessionJson = cached.toString();
            
            // 프로바이더 타입에 따라 적절한 클래스로 역직렬화
            CloudSessionCredential session = deserializeSession(sessionJson, providerType);
            
            // 세션이 유효한지 확인
            if (session != null && session.isValid()) {
                log.debug("[SessionCacheService] Cached session found and valid - key={}", cacheKey);
                return Optional.of(session);
            } else {
                log.debug("[SessionCacheService] Cached session expired - key={}", cacheKey);
                redisTemplate.delete(cacheKey);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("[SessionCacheService] Failed to deserialize session", e);
            return Optional.empty();
        }
    }
    
    /**
     * 세션을 Redis에서 삭제합니다.
     * 
     * @param tenantKey 테넌트 키
     * @param accountScope 계정 범위 (AWS Account ID, Azure Subscription ID, GCP Project ID)
     * @param providerType 프로바이더 타입
     */
    public void evictSession(String tenantKey, String accountScope, ProviderType providerType) {
        if (redisTemplate == null) {
            return;
        }
        
        String cacheKey = buildCacheKey(tenantKey, accountScope, providerType);
        redisTemplate.delete(cacheKey);
        log.debug("[SessionCacheService] Session evicted - key={}", cacheKey);
    }
    
    /**
     * 캐시 키를 생성합니다.
     * 
     * @param tenantKey 테넌트 키
     * @param accountScope 계정 범위 (AWS Account ID, Azure Subscription ID, GCP Project ID)
     * @param providerType 프로바이더 타입
     * @return 캐시 키
     */
    private String buildCacheKey(String tenantKey, String accountScope, ProviderType providerType) {
        return String.format("%s%s:%s:%s", CACHE_KEY_PREFIX, tenantKey, providerType.name(), accountScope);
    }
    
    /**
     * 기본 TTL을 반환합니다.
     * 
     * @return TTL (분)
     */
    public int getDefaultTtlMinutes() {
        return DEFAULT_TTL_MINUTES;
    }
    
    /**
     * 프로바이더 타입에 따라 적절한 클래스로 세션을 역직렬화합니다.
     * 
     * @param sessionJson 세션 JSON 문자열
     * @param providerType 프로바이더 타입
     * @return CloudSessionCredential
     */
    private CloudSessionCredential deserializeSession(String sessionJson, ProviderType providerType) {
        try {
            switch (providerType) {
                case AWS:
                    return objectMapper.readValue(sessionJson, AwsSessionCredential.class);
                default:
                    log.warn("[SessionCacheService] Unknown provider type for deserialization: {}", providerType);
                    return null;
            }
        } catch (JsonProcessingException e) {
            log.error("[SessionCacheService] Failed to deserialize session for provider: {}", providerType, e);
            return null;
        }
    }
}

