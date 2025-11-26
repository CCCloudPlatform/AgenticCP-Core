package com.agenticcp.core.domain.cloud.port.model.account;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import java.time.LocalDateTime;

/**
 * 클라우드 세션/토큰 자격증명 공통 인터페이스
 * 
 * 프로바이더별 단기 세션/토큰(STS, OAuth2, JWT 등)을 추상화합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
public interface CloudSessionCredential {
    
    /**
     * 프로바이더 타입 반환
     * 
     * @return ProviderType
     */
    ProviderType getProviderType();
    
    /**
     * 세션/토큰 만료 시간
     * 
     * @return 만료 시간
     */
    LocalDateTime getExpiresAt();
    
    /**
     * 세션/토큰이 유효한지 확인
     * 
     * @return 유효하면 true
     */
    default boolean isValid() {
        return getExpiresAt() != null && getExpiresAt().isAfter(LocalDateTime.now());
    }
    
    /**
     * 세션/토큰이 곧 만료될지 확인 (갱신 필요 여부 판단)
     * 
     * @param bufferMinutes 갱신 버퍼 시간 (분)
     * @return 곧 만료되면 true
     */
    default boolean isExpiringSoon(int bufferMinutes) {
        if (getExpiresAt() == null) {
            return true;
        }
        return LocalDateTime.now().plusMinutes(bufferMinutes).isAfter(getExpiresAt());
    }
}

