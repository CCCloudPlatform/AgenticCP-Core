package com.agenticcp.core.domain.cloud.service.rdbms;

import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.port.outbound.rdbms.RdbmsDiscoveryPort;
import com.agenticcp.core.domain.cloud.port.outbound.rdbms.RdbmsLifecyclePort;
import com.agenticcp.core.domain.cloud.port.outbound.rdbms.RdbmsManagementPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * RDBMS 포트 라우터
 *
 * 다양한 클라우드 제공업체의 RDBMS 관리 포트를 관리하고,
 * 요청된 제공업체 타입에 따라 적절한 포트를 선택하여 반환합니다.
 *
 * 헥사고날 아키텍처의 라우터 계층에 해당하며, 제공업체별 어댑터를 동적으로 선택합니다.
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class RdbmsPortRouter {

    private final Map<ProviderType, RdbmsManagementPort> managementPorts;
    private final Map<ProviderType, RdbmsDiscoveryPort> discoveryPorts;
    private final Map<ProviderType, RdbmsLifecyclePort> lifecyclePorts;

    public RdbmsPortRouter(
            List<RdbmsManagementPort> managementPortList,
            List<RdbmsDiscoveryPort> discoveryPortList,
            List<RdbmsLifecyclePort> lifecyclePortList
    ) {
        this.managementPorts = buildPortMap(managementPortList);
        this.discoveryPorts = buildPortMap(discoveryPortList);
        this.lifecyclePorts = buildPortMap(lifecyclePortList);

        log.info("[RdbmsPortRouter] RDBMS router initialized: management={}, discovery={}, lifecycle={}",
                managementPorts.keySet(), discoveryPorts.keySet(), lifecyclePorts.keySet());
    }

    private <T> Map<ProviderType, T> buildPortMap(List<T> ports) {
        Map<ProviderType, T> map = new EnumMap<>(ProviderType.class);
        ports.stream()
                .filter(port -> port instanceof ProviderScoped)
                .forEach(port -> {
                    ProviderType providerType = ((ProviderScoped) port).getProviderType();
                    map.put(providerType, port);
                    log.debug("[RdbmsPortRouter] Registered {} port for provider {}", 
                            port.getClass().getSimpleName(), providerType);
                });
        return map;
    }

    public RdbmsManagementPort management(ProviderType providerType) {
        RdbmsManagementPort port = managementPorts.get(providerType);
        if (port == null) {
            throw new IllegalArgumentException("지원하지 않는 프로바이더입니다: " + providerType);
        }
        return port;
    }

    public RdbmsDiscoveryPort discovery(ProviderType providerType) {
        RdbmsDiscoveryPort port = discoveryPorts.get(providerType);
        if (port == null) {
            throw new IllegalArgumentException("지원하지 않는 프로바이더입니다: " + providerType);
        }
        return port;
    }

    public RdbmsLifecyclePort lifecycle(ProviderType providerType) {
        RdbmsLifecyclePort port = lifecyclePorts.get(providerType);
        if (port == null) {
            throw new IllegalArgumentException("생명주기 관리를 지원하지 않는 프로바이더입니다: " + providerType);
        }
        return port;
    }
}
