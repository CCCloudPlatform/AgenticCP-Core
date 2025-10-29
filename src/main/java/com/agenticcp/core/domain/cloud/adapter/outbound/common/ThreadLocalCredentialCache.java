package com.agenticcp.core.domain.cloud.adapter.outbound.common;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsCredentials;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ThreadLocal 기반 자격증명 캐시 관리 클래스
 * 
 * 멀티 테넌트 환경에서 각 요청 스레드별로 AWS 자격증명을 안전하게 관리합니다.
 * STS AssumeRole을 통해 획득한 임시 자격증명을 ThreadLocal에 저장하고 관리합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
public class ThreadLocalCredentialCache {
    
    private static final ThreadLocal<ConcurrentHashMap<String, CachedCredentials>> CREDENTIAL_CACHE = 
            ThreadLocal.withInitial(ConcurrentHashMap::new);
    
    /**
     * 자격증명을 캐시에 저장
     * 
     * @param cacheKey 캐시 키 (tenantKey + providerType + accountScope)
     * @param credentials AWS 자격증명
     * @param expirationTime 만료 시간
     */
    public static void putCredentials(String cacheKey, AwsCredentials credentials, LocalDateTime expirationTime) {
        log.debug("자격증명 캐시 저장: cacheKey={}, expirationTime={}", cacheKey, expirationTime);
        
        CachedCredentials cachedCredentials = CachedCredentials.builder()
                .credentials(credentials)
                .expirationTime(expirationTime)
                .cachedAt(LocalDateTime.now())
                .build();
        
        CREDENTIAL_CACHE.get().put(cacheKey, cachedCredentials);
    }
    
    /**
     * 캐시에서 자격증명 조회
     * 
     * @param cacheKey 캐시 키
     * @return 캐시된 자격증명 (없거나 만료된 경우 null)
     */
    public static AwsCredentials getCredentials(String cacheKey) {
        CachedCredentials cachedCredentials = CREDENTIAL_CACHE.get().get(cacheKey);
        
        if (cachedCredentials == null) {
            log.debug("캐시에서 자격증명을 찾을 수 없음: cacheKey={}", cacheKey);
            return null;
        }
        
        // 만료 시간 확인
        if (LocalDateTime.now().isAfter(cachedCredentials.getExpirationTime())) {
            log.debug("자격증명이 만료됨: cacheKey={}, expirationTime={}", 
                    cacheKey, cachedCredentials.getExpirationTime());
            CREDENTIAL_CACHE.get().remove(cacheKey);
            return null;
        }
        
        log.debug("캐시에서 자격증명 조회 성공: cacheKey={}", cacheKey);
        return cachedCredentials.getCredentials();
    }
    
    /**
     * 특정 캐시 키의 자격증명 제거
     * 
     * @param cacheKey 캐시 키
     */
    public static void removeCredentials(String cacheKey) {
        log.debug("자격증명 캐시 제거: cacheKey={}", cacheKey);
        CREDENTIAL_CACHE.get().remove(cacheKey);
    }
    
    /**
     * 현재 스레드의 모든 자격증명 캐시 제거
     */
    public static void clearAll() {
        log.debug("현재 스레드의 모든 자격증명 캐시 제거");
        CREDENTIAL_CACHE.get().clear();
    }
    
    /**
     * 현재 스레드의 자격증명 캐시 상태 조회
     * 
     * @return 캐시된 자격증명 개수
     */
    public static int getCacheSize() {
        return CREDENTIAL_CACHE.get().size();
    }
    
    /**
     * 현재 스레드의 첫 번째 자격증명 조회
     * 
     * @return 첫 번째 캐시된 자격증명 (없으면 null)
     */
    public static AwsCredentials getFirstCredentials() {
        var cache = CREDENTIAL_CACHE.get();
        if (cache.isEmpty()) {
            return null;
        }
        
        // 첫 번째 자격증명 반환
        return cache.values().iterator().next().getCredentials();
    }
    
    /**
     * 캐시된 자격증명 정보
     */
    @lombok.Builder
    @lombok.Getter
    private static class CachedCredentials {
        private final AwsCredentials credentials;
        private final LocalDateTime expirationTime;
        private final LocalDateTime cachedAt;
    }
}
