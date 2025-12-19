package com.agenticcp.core.domain.cloud.service.function;

import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.port.outbound.function.FunctionDiscoveryPort;
import com.agenticcp.core.domain.cloud.port.outbound.function.FunctionInvocationPort;
import com.agenticcp.core.domain.cloud.port.outbound.function.FunctionManagementPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Function 포트 라우터
 * 
 * ProviderType에 따라 적절한 Function 포트(Management, Discovery, Invocation)를 선택합니다.
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Component
public class FunctionPortRouter {
    
    private final Map<ProviderType, FunctionManagementPort> managementPorts;
    private final Map<ProviderType, FunctionDiscoveryPort> discoveryPorts;
    private final Map<ProviderType, FunctionInvocationPort> invocationPorts;

    public FunctionPortRouter(
            List<FunctionManagementPort> managementPortList,
            List<FunctionDiscoveryPort> discoveryPortList,
            List<FunctionInvocationPort> invocationPortList
    ) {
        this.managementPorts = buildPortMap(managementPortList);
        this.discoveryPorts = buildPortMap(discoveryPortList);
        this.invocationPorts = buildPortMap(invocationPortList);
    }

    private <T> Map<ProviderType, T> buildPortMap(List<T> ports) {
        return ports.stream()
                .filter(port -> port instanceof ProviderScoped)
                .collect(Collectors.toMap(
                        port -> ((ProviderScoped) port).getProviderType(),
                        Function.identity(),
                        (existing, replacement) -> existing // 중복 시 기존 것 유지
                ));
    }

    /**
     * Management 포트 반환
     *
     * @param providerType 프로바이더 타입
     * @return FunctionManagementPort
     * @throws IllegalArgumentException 지원하지 않는 프로바이더인 경우
     */
    public FunctionManagementPort management(ProviderType providerType) {
        FunctionManagementPort port = managementPorts.get(providerType);
        if (port == null) {
            throw new IllegalArgumentException("지원하지 않는 프로바이더입니다: " + providerType);
        }
        return port;
    }

    /**
     * Discovery 포트 반환
     *
     * @param providerType 프로바이더 타입
     * @return FunctionDiscoveryPort
     * @throws IllegalArgumentException 지원하지 않는 프로바이더인 경우
     */
    public FunctionDiscoveryPort discovery(ProviderType providerType) {
        FunctionDiscoveryPort port = discoveryPorts.get(providerType);
        if (port == null) {
            throw new IllegalArgumentException("지원하지 않는 프로바이더입니다: " + providerType);
        }
        return port;
    }

    /**
     * Invocation 포트 반환
     *
     * @param providerType 프로바이더 타입
     * @return FunctionInvocationPort
     * @throws IllegalArgumentException 지원하지 않는 프로바이더인 경우
     */
    public FunctionInvocationPort invocation(ProviderType providerType) {
        FunctionInvocationPort port = invocationPorts.get(providerType);
        if (port == null) {
            throw new IllegalArgumentException("지원하지 않는 프로바이더입니다: " + providerType);
        }
        return port;
    }
}
