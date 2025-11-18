package com.agenticcp.core.domain.cloud.command;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * 자격증명 저장 커맨드
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Value
@Builder
public class StoreCredentialCommand {
    
    /**
     * 테넌트 키
     */
    String tenantKey;
    
    /**
     * 프로바이더 타입
     */
    ProviderType providerType;
    
    /**
     * 계정 범위 (AccountId, SubscriptionId, ProjectId)
     */
    String accountScope;
    
    /**
     * 자격증명 정보 (평문)
     * 프로바이더별로 필드가 다를 수 있음
     */
    Map<String, String> credentials;
}

