package com.agenticcp.core.domain.cloud.port.model.storage;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class CreateObjectStorageContainerCommand {
    private final String containerName;
    private final String region;
    private final Map<String, String> tags;
    private final String objectOwnership;
    private final Boolean objectLockEnabled;
}
