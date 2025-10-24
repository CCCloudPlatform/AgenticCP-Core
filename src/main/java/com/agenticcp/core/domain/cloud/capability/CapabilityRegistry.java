package com.agenticcp.core.domain.cloud.capability;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class CapabilityRegistry {

    private final Map<String, CspCapability> registry = new ConcurrentHashMap<>();

    public void register(ProviderType providerType, String serviceKey, String resourceType, CspCapability capability) {
        registry.put(key(providerType, serviceKey, resourceType), capability);
    }

    public CspCapability get(ProviderType providerType, String serviceKey, String resourceType) {
        return registry.get(key(providerType, serviceKey, resourceType));
    }

    private String key(ProviderType providerType, String serviceKey, String resourceType) {
        return providerType.name() + "|" + serviceKey + "|" + resourceType;
    }
}
