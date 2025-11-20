package com.agenticcp.core.domain.cloud.service.vm;

import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.port.outbound.aws.VmManagementPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

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

    private final Map<ProviderType, VmManagementPort> vmPorts = new EnumMap<>(ProviderType.class);

    /**
     * VM 포트 라우터 생성자
     *
     * @param ports 등록된 모든 VmManagementPort 구현체들
     */
    public VmPortRouter(List<VmManagementPort> ports) {
        log.info("[VmPortRouter] Initializing VM port router with {} ports", ports.size());
        
        ports.stream()
            .filter(p -> p instanceof ProviderScoped)
            .forEach(p -> {
                ProviderType providerType = ((ProviderScoped) p).getProviderType();
                vmPorts.put(providerType, p);
                log.debug("[VmPortRouter] Registered VM port for provider: {} -> {}", 
                    providerType, p.getClass().getSimpleName());
            });
        
        log.info("[VmPortRouter] VM port router initialized with {} providers: {}", 
            vmPorts.size(), vmPorts.keySet());
    }

    /**
     * 지정된 제공업체 타입에 해당하는 VM 관리 포트를 반환합니다.
     *
     * @param type 클라우드 제공업체 타입
     * @return 해당 제공업체의 VmManagementPort 구현체
     * @throws IllegalArgumentException 지원되지 않는 제공업체 타입인 경우
     */
    public VmManagementPort vm(ProviderType type) {
        log.debug("[VmPortRouter] Requesting VM port for provider: {}", type);
        
        VmManagementPort port = vmPorts.get(type);
        if (port == null) {
            log.error("[VmPortRouter] Unsupported provider type: {}. Available providers: {}", 
                type, vmPorts.keySet());
            throw new IllegalArgumentException("Unsupported provider: " + type + 
                ". Available providers: " + vmPorts.keySet());
        }
        
        log.debug("[VmPortRouter] Returning VM port: {} for provider: {}", 
            port.getClass().getSimpleName(), type);
        return port;
    }

    /**
     * 현재 등록된 모든 제공업체 타입을 반환합니다.
     * 
     * @return 등록된 제공업체 타입들의 Set
     */
    public java.util.Set<ProviderType> getSupportedProviders() {
        return vmPorts.keySet();
    }

    /**
     * 지정된 제공업체가 지원되는지 확인합니다.
     * 
     * @param type 확인할 제공업체 타입
     * @return 지원 여부
     */
    public boolean isProviderSupported(ProviderType type) {
        return vmPorts.containsKey(type);
    }

    /**
     * 등록된 VM 포트의 개수를 반환합니다.
     *
     * @return 등록된 포트 개수
     */
    public int getPortCount() {
        return vmPorts.size();
    }
}
