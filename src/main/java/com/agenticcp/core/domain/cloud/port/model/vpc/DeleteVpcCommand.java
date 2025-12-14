package com.agenticcp.core.domain.cloud.port.model.vpc;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;

import lombok.Builder;

/**
 * VPC 삭제 명령.
 */
@Builder
public record DeleteVpcCommand(
        CloudProvider.ProviderType providerType,
        String accountScope,
        String region,
        String providerResourceId,
        String serviceKey,
        String resourceType,
        String tenantKey,
        CloudSessionCredential session
) {
}

