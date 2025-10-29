package com.agenticcp.core.domain.cloud.port.model;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ResourceIdentity {
    ProviderType providerType;
    String accountScope; // AWS: AccountId, Azure: SubscriptionId, GCP: ProjectId
    String region;
    String providerResourceId;
    String serviceKey; // e.g., EC2, COMPUTE, VM
    String resourceType; // domain-level type string if needed in adapters
}
