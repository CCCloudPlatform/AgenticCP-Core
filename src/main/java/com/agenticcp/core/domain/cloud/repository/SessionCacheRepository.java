package com.agenticcp.core.domain.cloud.repository;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;

import java.util.Optional;

/**
 * 세션 캐시 저장소 인터페이스
 * 
 * 클라우드 프로바이더 세션 자격 증명을 캐싱하기 위한 저장소 추상화입니다.
 * 도메인 계층이 구체적인 캐시 구현(Redis 등)에 의존하지 않도록 인터페이스로 분리합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-20
 */
public interface SessionCacheRepository {

    /**
     * 세션 자격 증명을 캐시에 저장합니다.
     * 
     * @param tenantKey 테넌트 식별자
     * @param accountScope 계정 범위 식별자
     * @param providerType 클라우드 프로바이더 타입
     * @param session 저장할 세션 자격 증명
     * @param ttlMinutes 캐시 유지 시간(분 단위)
     */
    void cacheSession(String tenantKey, String accountScope, ProviderType providerType,
                      CloudSessionCredential session, int ttlMinutes);

    /**
     * 캐시에서 세션 자격 증명을 조회합니다.
     * 
     * 캐시에 저장된 세션이 존재하고 유효한 경우에만 반환합니다.
     * 만료되었거나 유효하지 않은 세션은 자동으로 삭제됩니다.
     * 
     * @param tenantKey 테넌트 식별자
     * @param accountScope 계정 범위 식별자
     * @param providerType 클라우드 프로바이더 타입
     * @return 조회된 세션 자격 증명 (Optional)
     */
    Optional<CloudSessionCredential> getCachedSession(String tenantKey, String accountScope,
                                                      ProviderType providerType);

    /**
     * 캐시에서 세션 자격 증명을 명시적으로 제거합니다.
     * 
     * @param tenantKey 테넌트 식별자
     * @param accountScope 계정 범위 식별자
     * @param providerType 클라우드 프로바이더 타입
     */
    void evictSession(String tenantKey, String accountScope, ProviderType providerType);

    /**
     * 기본 TTL(Time To Live) 값을 분 단위로 반환합니다.
     * 
     * @return 기본 TTL 값(분)
     */
    int getDefaultTtlMinutes();
}

