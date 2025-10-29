package com.agenticcp.core.domain.cloud.port.model;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

@Value
@Builder
public class ResourceQuery {
    ProviderType providerType;
    String accountScope;
    Set<String> regions;
    String nameContains;
    String resourceType;
    Map<String, String> tagsEquals;
    Instant changedAfter;
    int page;
    int size;
}
