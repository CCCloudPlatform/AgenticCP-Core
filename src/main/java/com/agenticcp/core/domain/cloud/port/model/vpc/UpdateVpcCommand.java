package com.agenticcp.core.domain.cloud.port.model.vpc;

import java.util.Map;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;

import lombok.Builder;

/**
 * VPC 수정 명령.
 */
@Builder
public record UpdateVpcCommand(
        CloudProvider.ProviderType providerType,
        String accountScope,
        String region,
        String providerResourceId,
        String vpcName,
        String description,
        Map<String, String> tags,
        String tenantKey,
        Map<String, Object> providerSpecificConfig,
        CloudSessionCredential session
) {
}

