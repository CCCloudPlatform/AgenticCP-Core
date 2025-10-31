package com.agenticcp.core.domain.cloud.service.aws;

import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.port.outbound.storage.ObjectStorageDiscoveryPort;
import com.agenticcp.core.domain.cloud.port.outbound.storage.ObjectStorageManagementPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Object Storage Container 포트 라우터
 * 
 * 헥사고날 아키텍처의 애플리케이션 계층에서 클라우드 프로바이더 타입에 따라
 * 적절한 Object Storage Container 포트 구현체를 선택하는 라우터입니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Component
@Slf4j
public class ObjectStoragePortRouter {

    private final Map<CloudProvider.ProviderType, ObjectStorageDiscoveryPort> discoveryPorts;
    private final Map<CloudProvider.ProviderType, ObjectStorageManagementPort> managementPorts;

    /**
     * 생성자 - Spring이 자동으로 어댑터들을 주입합니다.
     * 
     * @param discoveryPortList Object Storage Container 발견 포트 구현체 목록
     * @param managementPortList Object Storage Container 관리 포트 구현체 목록
     */
    public ObjectStoragePortRouter(List<ObjectStorageDiscoveryPort> discoveryPortList,
                                   List<ObjectStorageManagementPort> managementPortList) {
        
        // Discovery 포트 맵 초기화
        this.discoveryPorts = discoveryPortList.stream()
            .filter(port -> port instanceof ProviderScoped)
            .collect(java.util.stream.Collectors.toMap(
                port -> ((ProviderScoped) port).getProviderType(),
                port -> port,
                (existing, replacement) -> {
                    log.warn("중복된 Object Storage Container Discovery 포트 발견: provider={}, existing={}, replacement={}", 
                            ((ProviderScoped) existing).getProviderType(), 
                            existing.getClass().getSimpleName(), 
                            replacement.getClass().getSimpleName());
                    return existing; // 기존 것을 유지
                }
            ));

        // Management 포트 맵 초기화
        this.managementPorts = managementPortList.stream()
            .filter(port -> port instanceof ProviderScoped)
            .collect(java.util.stream.Collectors.toMap(
                port -> ((ProviderScoped) port).getProviderType(),
                port -> port,
                (existing, replacement) -> {
                    log.warn("중복된 Object Storage Container Management 포트 발견: provider={}, existing={}, replacement={}", 
                            ((ProviderScoped) existing).getProviderType(), 
                            existing.getClass().getSimpleName(), 
                            replacement.getClass().getSimpleName());
                    return existing; // 기존 것을 유지
                }
            ));

        log.info("Object Storage Container 포트 라우터 초기화 완료: discoveryPorts={}, managementPorts={}", 
                discoveryPorts.keySet(), managementPorts.keySet());
    }

    /**
     * 지정된 프로바이더 타입에 해당하는 Object Storage Container 발견 포트를 반환합니다.
     * 
     * @param providerType 클라우드 프로바이더 타입
     * @return Object Storage Container 발견 포트
     * @throws IllegalArgumentException 지원하지 않는 프로바이더 타입인 경우
     */
    public ObjectStorageDiscoveryPort discovery(CloudProvider.ProviderType providerType) {
        ObjectStorageDiscoveryPort port = discoveryPorts.get(providerType);
        if (port == null) {
            log.error("지원하지 않는 프로바이더 타입: {}, 지원되는 타입: {}", providerType, discoveryPorts.keySet());
            throw new IllegalArgumentException("지원하지 않는 프로바이더 타입입니다: " + providerType);
        }
        
        log.debug("Object Storage Container Discovery 포트 선택: provider={}, port={}", 
                providerType, port.getClass().getSimpleName());
        
        return port;
    }

    /**
     * 지정된 프로바이더 타입에 해당하는 Object Storage Container 관리 포트를 반환합니다.
     * 
     * @param providerType 클라우드 프로바이더 타입
     * @return Object Storage Container 관리 포트
     * @throws IllegalArgumentException 지원하지 않는 프로바이더 타입인 경우
     */
    public ObjectStorageManagementPort management(CloudProvider.ProviderType providerType) {
        ObjectStorageManagementPort port = managementPorts.get(providerType);
        if (port == null) {
            log.error("지원하지 않는 프로바이더 타입: {}, 지원되는 타입: {}", providerType, managementPorts.keySet());
            throw new IllegalArgumentException("지원하지 않는 프로바이더 타입입니다: " + providerType);
        }
        
        log.debug("Object Storage Container Management 포트 선택: provider={}, port={}", providerType, port.getClass().getSimpleName());

        return port;
    }

    /**
     * 라우터 상태 정보를 반환합니다.
     * 
     * @return 라우터 상태 정보
     */
    public RouterStatus getStatus() {
        return RouterStatus.builder()
                .supportedProviders(discoveryPorts.keySet())
                .discoveryPortCount(discoveryPorts.size())
                .managementPortCount(managementPorts.size())
                .build();
    }

    /**
     * 라우터 상태 정보를 담는 내부 클래스
     */
    @lombok.Builder
    @lombok.Data
    public static class RouterStatus {
        private final java.util.Set<CloudProvider.ProviderType> supportedProviders;
        private final int discoveryPortCount;
        private final int managementPortCount;
    }
}
