package com.agenticcp.core.domain.cloud.service.vm;

import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.port.outbound.vm.VmDiscoveryPort;
import com.agenticcp.core.domain.cloud.port.outbound.vm.VmLifecyclePort;
import com.agenticcp.core.domain.cloud.port.outbound.vm.VmTaggingPort;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * VM 포트 라우터
 *
 * 다양한 클라우드 제공업체의 VM 관리 포트를 관리하고,
 * 요청된 제공업체 타입에 따라 적절한 포트를 선택하여 반환합니다.
 *
 * 핵사고날 아키텍처의 라우터 계층에 해당하며, 제공업체별 어댑터를 동적으로 선택합니다.
 */
@Slf4j
@Component
public class VmPortRouter {

    private final Map<ProviderType, VmDiscoveryPort> discoveryPorts;
    private final Map<ProviderType, VmLifecyclePort> lifecyclePorts;
    private final Map<ProviderType, VmTaggingPort> taggingPorts;

    public VmPortRouter(
            List<VmDiscoveryPort> discoveryPortList,
            List<VmLifecyclePort> lifecyclePortList,
            List<VmTaggingPort> taggingPortList
    ) {
        this.discoveryPorts = buildPortMap(discoveryPortList);
        this.lifecyclePorts = buildPortMap(lifecyclePortList);
        this.taggingPorts = buildPortMap(taggingPortList);

        log.info("[VmPortRouter] VM router initialized: discovery={}, lifecycle={}, tagging={}",
                discoveryPorts.keySet(), lifecyclePorts.keySet(), taggingPorts.keySet());
    }

    private <T> Map<ProviderType, T> buildPortMap(List<T> ports) {
        Map<ProviderType, T> map = new EnumMap<>(ProviderType.class);
        ports.stream()
                .filter(port -> port instanceof ProviderScoped)
                .forEach(port -> {
                    ProviderType providerType = ((ProviderScoped) port).getProviderType();
                    map.put(providerType, port);
                    log.debug("[VmPortRouter] Registered {} port for provider {}", port.getClass().getSimpleName(), providerType);
                });
        return map;
    }

    public VmDiscoveryPort discovery(ProviderType providerType) {
        VmDiscoveryPort port = discoveryPorts.get(providerType);
        if (port == null) {
            throw new IllegalArgumentException("지원하지 않는 프로바이더입니다: " + providerType);
        }
        return port;
    }

    public VmLifecyclePort lifecycle(ProviderType providerType) {
        VmLifecyclePort port = lifecyclePorts.get(providerType);
        if (port == null) {
            throw new IllegalArgumentException("지원하지 않는 프로바이더입니다: " + providerType);
        }
        return port;
    }

    public VmTaggingPort tagging(ProviderType providerType) {
        VmTaggingPort port = taggingPorts.get(providerType);
        if (port == null) {
            throw new IllegalArgumentException("태그 관리를 지원하지 않는 프로바이더입니다: " + providerType);
        }
        return port;
    }
}
