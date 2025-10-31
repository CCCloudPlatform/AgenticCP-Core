package com.agenticcp.core.domain.cloud.adapter.outbound.common;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;

/**
 * ProviderScoped 인터페이스
 * 어댑터가 특정 클라우드 프로바이더에 속함을 나타내는 마커 인터페이스
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
public interface ProviderScoped {

    /**
     * 이 어댑터가 지원하는 클라우드 프로바이더 타입을 반환합니다.
     *
     * @return ProviderType
     */
    CloudProvider.ProviderType getProviderType();
}
