package com.agenticcp.core.domain.cloud.command;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import lombok.Builder;
import lombok.Value;

/**
 * 자격증명 조회 커맨드
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Value
@Builder
public class ResolveCredentialCommand {
    
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
}

