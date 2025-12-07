package com.agenticcp.core.domain.cloud.repository;

import com.agenticcp.core.domain.cloud.adapter.outbound.aws.account.AwsSessionCredential;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Redis 기반 세션 캐시 저장소 구현체
 * 
 * 클라우드 프로바이더 세션 자격 증명을 Redis에 저장하고 조회합니다.
 * 세션 정보는 JSON 형태로 직렬화되어 저장되며, TTL 기반으로 자동 만료됩니다.
 * Redis가 구성되지 않은 경우에도 동작하도록 설계되어 있으며,
 * 이 경우 캐시 작업은 무시되고 빈 결과를 반환합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-20
 */
@Slf4j
@Repository
public class RedisSessionCacheRepository implements SessionCacheRepository {

    /** Redis 캐시 키 접두사 */
    private static final String CACHE_KEY_PREFIX = "cloud:session:";
    /** 기본 TTL 값 (분 단위) - AWS STS 세션 토큰의 기본 만료 시간(60분)보다 5분 짧게 설정 */
    private static final int DEFAULT_TTL_MINUTES = 55;

    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * RedisSessionRepository 생성자
     * 
     * ObjectMapper에 JavaTimeModule을 등록하고 날짜 직렬화 설정을 구성합니다.
     * RedisTemplate이 선택적 의존성이므로 구성되지 않은 경우 null일 수 있습니다.
     * 
     * @param objectMapper JSON 직렬화/역직렬화를 위한 ObjectMapper
     * @param redisTemplate Redis 작업을 위한 RedisTemplate (선택적)
     */
    public RedisSessionCacheRepository(ObjectMapper objectMapper,
                                       @Autowired(required = false) RedisTemplate<String, Object> redisTemplate) {
        if (ObjectMapper.findModules().stream().noneMatch(m -> m instanceof JavaTimeModule)) {
            objectMapper.registerModule(new JavaTimeModule());
        }
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 세션 자격 증명을 Redis에 저장합니다.
     * 
     * 세션 정보를 JSON으로 직렬화하여 Redis에 저장하며, 지정된 TTL을 적용합니다.
     * Redis가 구성되지 않은 경우 작업을 건너뜁니다.
     * 
     * @param tenantKey 테넌트 식별자
     * @param accountScope 계정 범위 식별자
     * @param providerType 클라우드 프로바이더 타입
     * @param session 저장할 세션 자격 증명
     * @param ttlMinutes 캐시 유지 시간(분 단위)
     * @throws RuntimeException 세션 직렬화 실패 시
     */
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

    /**
     * Redis에서 세션 자격 증명을 조회합니다.
     * 
     * 캐시에서 세션을 조회한 후 유효성을 검사합니다.
     * 유효하지 않거나 만료된 세션은 자동으로 삭제하고 빈 결과를 반환합니다.
     * Redis가 구성되지 않았거나 역직렬화에 실패한 경우 빈 결과를 반환합니다.
     * 
     * @param tenantKey 테넌트 식별자
     * @param accountScope 계정 범위 식별자
     * @param providerType 클라우드 프로바이더 타입
     * @return 조회된 세션 자격 증명 (Optional), 없거나 유효하지 않은 경우 빈 Optional
     */
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

    /**
     * Redis에서 세션 자격 증명을 명시적으로 제거합니다.
     * 
     * 세션 만료나 로그아웃 시 캐시를 즉시 무효화하기 위해 사용됩니다.
     * Redis가 구성되지 않은 경우 작업을 건너뜁니다.
     * 
     * @param tenantKey 테넌트 식별자
     * @param accountScope 계정 범위 식별자
     * @param providerType 클라우드 프로바이더 타입
     */
    @Override
    public void evictSession(String tenantKey, String accountScope, ProviderType providerType) {
        if (redisTemplate == null) {
            return;
        }
        String cacheKey = buildCacheKey(tenantKey, accountScope, providerType);
        redisTemplate.delete(cacheKey);
        log.debug("[RedisSessionCacheAdapter] Session evicted - key={}", cacheKey);
    }

    /**
     * 기본 TTL 값을 반환합니다.
     * 
     * @return 기본 TTL 값(분 단위)
     */
    @Override
    public int getDefaultTtlMinutes() {
        return DEFAULT_TTL_MINUTES;
    }

    /**
     * Redis 캐시 키를 생성합니다.
     * 
     * 형식: "cloud:session:{tenantKey}:{providerType}:{accountScope}"
     * 예시: "cloud:session:tenant1:AWS:account-123"
     * 
     * @param tenantKey 테넌트 식별자
     * @param accountScope 계정 범위 식별자
     * @param providerType 클라우드 프로바이더 타입
     * @return 생성된 캐시 키
     */
    private String buildCacheKey(String tenantKey, String accountScope, ProviderType providerType) {
        return String.format("%s%s:%s:%s", CACHE_KEY_PREFIX, tenantKey, providerType.name(), accountScope);
    }

    /**
     * JSON 문자열을 프로바이더 타입에 맞는 세션 자격 증명 객체로 역직렬화합니다.
     * 
     * 프로바이더 타입에 따라 적절한 구현 클래스로 역직렬화합니다.
     * 현재는 AWS만 지원하며, 다른 프로바이더 타입은 경고 로그를 남기고 null을 반환합니다.
     * 
     * @param sessionJson 역직렬화할 JSON 문자열
     * @param providerType 클라우드 프로바이더 타입
     * @return 역직렬화된 세션 자격 증명 객체, 실패 시 null
     */
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

