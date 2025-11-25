package com.agenticcp.core.domain.cloud.port.model.storage;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class CreateObjectStorageContainerCommand {
    CloudProvider.ProviderType providerType;
    String accountScope;
    private final String containerName;
    private final String region;
    private final Map<String, String> tags;
    private final String objectOwnership;
    private final Boolean objectLockEnabled;
    private final CloudSessionCredential session;
}
