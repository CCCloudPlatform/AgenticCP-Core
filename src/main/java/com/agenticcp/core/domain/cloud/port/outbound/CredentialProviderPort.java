package com.agenticcp.core.domain.cloud.port.outbound;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;

public interface CredentialProviderPort {
    Object resolveCredentials(String tenantKey, ProviderType providerType, String accountScope);
}
