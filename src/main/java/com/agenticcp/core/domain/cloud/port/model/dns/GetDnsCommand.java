package com.agenticcp.core.domain.cloud.port.model.dns;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;

import lombok.Builder;

@Builder
public record GetDnsCommand(
        CloudProvider.ProviderType providerType,
        String accountScope,
        String region,
        String providerResourceId,
        String serviceKey,
        String resourceType,
        CloudSessionCredential session
) {
}
