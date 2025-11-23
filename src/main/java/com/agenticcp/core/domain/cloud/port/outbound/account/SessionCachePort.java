package com.agenticcp.core.domain.cloud.port.outbound.account;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;

import java.util.Optional;

/**
 * 세션 캐시 포트
 *
 * 도메인 계층이 세션 캐시 저장소 구현(Redis 등)에 의존하지 않도록 추상화합니다.
 */
public interface SessionCachePort {

    void cacheSession(String tenantKey, String accountScope, ProviderType providerType,
                      CloudSessionCredential session, int ttlMinutes);

    Optional<CloudSessionCredential> getCachedSession(String tenantKey, String accountScope,
                                                      ProviderType providerType);

    void evictSession(String tenantKey, String accountScope, ProviderType providerType);

    int getDefaultTtlMinutes();
}

