package com.agenticcp.core.domain.cloud.port.model.function;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import lombok.Builder;

import java.util.Map;

/**
 * Serverless Function 삭제 도메인 커맨드
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Builder
public record FunctionDeleteCommand(
        CloudProvider.ProviderType providerType,
        String accountScope,
        String region,
        String providerResourceId,
        String serviceKey,
        String resourceType,
        String tenantKey,
        Map<String, Object> providerSpecificConfig,  // CSP별 특화 삭제 옵션
        CloudSessionCredential session
) {
}
