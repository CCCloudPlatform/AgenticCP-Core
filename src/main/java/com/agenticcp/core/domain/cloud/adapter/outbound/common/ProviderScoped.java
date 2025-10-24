package com.agenticcp.core.domain.cloud.adapter.outbound.common;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;

public interface ProviderScoped {
    ProviderType getProviderType();
}
