package com.agenticcp.core.domain.cloud.service;

import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.port.outbound.ResourceDiscoveryPort;
import com.agenticcp.core.domain.cloud.port.outbound.ResourceLifecyclePort;
import com.agenticcp.core.domain.cloud.port.outbound.ResourceTaggingPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ResourcePortRouter {

    private final Map<ProviderType, ResourceDiscoveryPort> discovery = new EnumMap<>(ProviderType.class);
    private final Map<ProviderType, ResourceLifecyclePort> lifecycle = new EnumMap<>(ProviderType.class);
    private final Map<ProviderType, ResourceTaggingPort> tagging = new EnumMap<>(ProviderType.class);

    public ResourcePortRouter(List<ResourceDiscoveryPort> discoveryPorts,
                              List<ResourceLifecyclePort> lifecyclePorts,
                              List<ResourceTaggingPort> taggingPorts) {
        discoveryPorts.stream().filter(p -> p instanceof ProviderScoped).forEach(p ->
                discovery.put(((ProviderScoped) p).getProviderType(), p));
        lifecyclePorts.stream().filter(p -> p instanceof ProviderScoped).forEach(p ->
                lifecycle.put(((ProviderScoped) p).getProviderType(), p));
        taggingPorts.stream().filter(p -> p instanceof ProviderScoped).forEach(p ->
                tagging.put(((ProviderScoped) p).getProviderType(), p));
    }

    public ResourceDiscoveryPort discovery(ProviderType type) { return require(discovery, type); }
    public ResourceLifecyclePort lifecycle(ProviderType type) { return require(lifecycle, type); }
    public ResourceTaggingPort tagging(ProviderType type) { return require(tagging, type); }

    private static <T> T require(Map<ProviderType, T> map, ProviderType type) {
        T bean = map.get(type);
        if (bean == null) throw new IllegalArgumentException("Unsupported provider: " + type);
        return bean;
    }
}
