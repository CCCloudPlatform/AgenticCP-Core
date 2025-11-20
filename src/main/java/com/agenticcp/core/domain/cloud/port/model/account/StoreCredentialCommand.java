package com.agenticcp.core.domain.cloud.port.model.account;

import com.agenticcp.core.common.logging.masking.Masked;
import com.agenticcp.core.common.logging.masking.MaskingType;
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
    @Masked(type = MaskingType.TENANT_KEY)
    String tenantKey;
    
    /**
     * 프로바이더 타입
     */
    ProviderType providerType;
    
    /**
     * 계정 범위 (AccountId, SubscriptionId, ProjectId)
     */
    @Masked(type = MaskingType.ACCOUNT_SCOPE)
    String accountScope;
    
    /**
     * 자격증명 정보 (평문)
     * 프로바이더별로 필드가 다를 수 있음
     * Map 내부의 값은 MaskingService.maskMap()으로 자동 마스킹됨
     */
    Map<String, String> credentials;
}

