package com.agenticcp.core.domain.cloud.port.model.function;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import lombok.Builder;

/**
 * Serverless Function 단건 조회 커맨드
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Builder
public record GetFunctionCommand(
        CloudProvider.ProviderType providerType,
        String accountScope,
        String region,
        String providerResourceId,
        String serviceKey,
        String resourceType,
        CloudSessionCredential session
) {
}
