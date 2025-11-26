package com.agenticcp.core.domain.cloud.port.model.storage;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * Object Storage Container 생성 명령
 * 어댑터 레이어에서 사용하는 내부 명령 객체
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-05
 */
@Getter
@Builder
public class CreateObjectStorageContainerCommand {
    /**
     * 클라우드 프로바이더 타입
     */
    CloudProvider.ProviderType providerType;
    
    /**
     * 계정 스코프
     */
    String accountScope;
    
    /**
     * 생성할 Container 이름
     */
    private final String containerName;
    
    /**
     * 클라우드 리전
     */
    private final String region;
    
    /**
     * 태그 (키-값 쌍)
     */
    private final Map<String, String> tags;
    
    /**
     * 객체 소유권 설정
     * 예: "BucketOwnerEnforced", "ObjectWriter"
     */
    private final String objectOwnership;
    
    /**
     * 객체 잠금 활성화 여부
     * true: WORM(Write Once Read Many) 활성화
     */
    private final Boolean objectLockEnabled;
    
    /**
     * 클라우드 세션 자격증명
     */
    private final CloudSessionCredential session;
}
