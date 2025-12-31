package com.agenticcp.core.domain.cloud.service.cdn;

import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.port.outbound.cdn.CDNDiscoveryPort;
import com.agenticcp.core.domain.cloud.port.outbound.cdn.CDNInvalidationPort;
import com.agenticcp.core.domain.cloud.port.outbound.cdn.CDNManagementPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CDN Distribution 포트 라우터
 * 
 * 헥사고날 아키텍처의 애플리케이션 계층에서 클라우드 프로바이더 타입에 따라
 * 적절한 CDN Distribution 포트 구현체를 선택하는 라우터입니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Component
@Slf4j
public class CDNPortRouter {

    private final Map<CloudProvider.ProviderType, CDNManagementPort> managementPorts;
    private final Map<CloudProvider.ProviderType, CDNDiscoveryPort> discoveryPorts;
    private final Map<CloudProvider.ProviderType, CDNInvalidationPort> invalidationPorts;

    /**
     * 생성자 - Spring이 자동으로 어댑터들을 주입합니다.
     * 
     * @param managementPortList CDN Distribution 관리 포트 구현체 목록
     * @param discoveryPortList CDN Distribution 발견 포트 구현체 목록
     * @param invalidationPortList CDN 캐시 무효화 포트 구현체 목록
     */
    public CDNPortRouter(
            List<CDNManagementPort> managementPortList,
            List<CDNDiscoveryPort> discoveryPortList,
            List<CDNInvalidationPort> invalidationPortList) {
        
        // Management 포트 맵 초기화
        this.managementPorts = managementPortList.stream()
            .filter(port -> port instanceof ProviderScoped)
            .collect(Collectors.toMap(
                port -> ((ProviderScoped) port).getProviderType(),
                port -> port,
                (existing, replacement) -> {
                    log.warn("중복된 CDN Distribution Management 포트 발견: provider={}, existing={}, replacement={}", 
                            ((ProviderScoped) existing).getProviderType(), 
                            existing.getClass().getSimpleName(), 
                            replacement.getClass().getSimpleName());
                    return existing; // 기존 것을 유지
                }
            ));

        // Discovery 포트 맵 초기화
        this.discoveryPorts = discoveryPortList.stream()
            .filter(port -> port instanceof ProviderScoped)
            .collect(Collectors.toMap(
                port -> ((ProviderScoped) port).getProviderType(),
                port -> port,
                (existing, replacement) -> {
                    log.warn("중복된 CDN Distribution Discovery 포트 발견: provider={}, existing={}, replacement={}", 
                            ((ProviderScoped) existing).getProviderType(), 
                            existing.getClass().getSimpleName(), 
                            replacement.getClass().getSimpleName());
                    return existing; // 기존 것을 유지
                }
            ));

        // Invalidation 포트 맵 초기화
        this.invalidationPorts = invalidationPortList.stream()
            .filter(port -> port instanceof ProviderScoped)
            .collect(Collectors.toMap(
                port -> ((ProviderScoped) port).getProviderType(),
                port -> port,
                (existing, replacement) -> {
                    log.warn("중복된 CDN Invalidation 포트 발견: provider={}, existing={}, replacement={}", 
                            ((ProviderScoped) existing).getProviderType(), 
                            existing.getClass().getSimpleName(), 
                            replacement.getClass().getSimpleName());
                    return existing; // 기존 것을 유지
                }
            ));

        log.info("[CDNPortRouter] CDN Distribution 포트 라우터 초기화 완료: managementPorts={}, discoveryPorts={}, invalidationPorts={}", 
                managementPorts.keySet(), discoveryPorts.keySet(), invalidationPorts.keySet());
    }

    /**
     * 지정된 프로바이더 타입에 해당하는 CDN Distribution 관리 포트를 반환합니다.
     * 
     * @param providerType 클라우드 프로바이더 타입
     * @return CDN Distribution 관리 포트
     * @throws IllegalArgumentException 지원하지 않는 프로바이더 타입인 경우
     */
    public CDNManagementPort management(CloudProvider.ProviderType providerType) {
        CDNManagementPort port = managementPorts.get(providerType);
        if (port == null) {
            log.error("[CDNPortRouter] 지원하지 않는 프로바이더 타입: {}, 지원되는 타입: {}", providerType, managementPorts.keySet());
            throw new IllegalArgumentException("지원하지 않는 프로바이더 타입입니다: " + providerType);
        }
        
        log.debug("[CDNPortRouter] CDN Distribution Management 포트 선택: provider={}, port={}", 
                providerType, port.getClass().getSimpleName());
        
        return port;
    }

    /**
     * 지정된 프로바이더 타입에 해당하는 CDN Distribution 발견 포트를 반환합니다.
     * 
     * @param providerType 클라우드 프로바이더 타입
     * @return CDN Distribution 발견 포트
     * @throws IllegalArgumentException 지원하지 않는 프로바이더 타입인 경우
     */
    public CDNDiscoveryPort discovery(CloudProvider.ProviderType providerType) {
        CDNDiscoveryPort port = discoveryPorts.get(providerType);
        if (port == null) {
            log.error("[CDNPortRouter] 지원하지 않는 프로바이더 타입: {}, 지원되는 타입: {}", providerType, discoveryPorts.keySet());
            throw new IllegalArgumentException("지원하지 않는 프로바이더 타입입니다: " + providerType);
        }
        
        log.debug("[CDNPortRouter] CDN Distribution Discovery 포트 선택: provider={}, port={}", 
                providerType, port.getClass().getSimpleName());
        
        return port;
    }

    /**
     * 지정된 프로바이더 타입에 해당하는 CDN 캐시 무효화 포트를 반환합니다.
     * 
     * @param providerType 클라우드 프로바이더 타입
     * @return CDN 캐시 무효화 포트
     * @throws IllegalArgumentException 지원하지 않는 프로바이더 타입인 경우
     */
    public CDNInvalidationPort invalidation(CloudProvider.ProviderType providerType) {
        CDNInvalidationPort port = invalidationPorts.get(providerType);
        if (port == null) {
            log.error("[CDNPortRouter] 지원하지 않는 프로바이더 타입: {}, 지원되는 타입: {}", providerType, invalidationPorts.keySet());
            throw new IllegalArgumentException("지원하지 않는 프로바이더 타입입니다: " + providerType);
        }
        
        log.debug("[CDNPortRouter] CDN Invalidation 포트 선택: provider={}, port={}", 
                providerType, port.getClass().getSimpleName());
        
        return port;
    }
}

