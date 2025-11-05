package com.agenticcp.core.domain.cloud.command;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import lombok.Builder;
import lombok.Value;

/**
 * 자격증명 삭제 커맨드
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Value
@Builder
public class DeleteCredentialCommand {
    
    /**
     * 프로바이더 타입
     */
    ProviderType providerType;
    
    /**
     * 자격증명 키 참조 (UUID)
     */
    String credentialKey;
}

