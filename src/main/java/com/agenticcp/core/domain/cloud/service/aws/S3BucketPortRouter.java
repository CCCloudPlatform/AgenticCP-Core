package com.agenticcp.core.domain.cloud.service.aws;

import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.port.outbound.aws.S3BucketDiscoveryPort;
import com.agenticcp.core.domain.cloud.port.outbound.aws.S3BucketManagementPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * S3 버킷 포트 라우터
 * 
 * 헥사고날 아키텍처의 애플리케이션 계층에서 클라우드 프로바이더 타입에 따라
 * 적절한 S3 버킷 포트 구현체를 선택하는 라우터입니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Component
@Slf4j
public class S3BucketPortRouter {

    private final Map<CloudProvider.ProviderType, S3BucketDiscoveryPort> discoveryPorts;
    private final Map<CloudProvider.ProviderType, S3BucketManagementPort> managementPorts;

    /**
     * 생성자 - Spring이 자동으로 어댑터들을 주입합니다.
     * 
     * @param discoveryPortList S3 버킷 발견 포트 구현체 목록
     * @param managementPortList S3 버킷 관리 포트 구현체 목록
     */
    public S3BucketPortRouter(List<S3BucketDiscoveryPort> discoveryPortList,
                              List<S3BucketManagementPort> managementPortList) {
        
        // Discovery 포트 맵 초기화
        this.discoveryPorts = discoveryPortList.stream()
            .filter(port -> port instanceof ProviderScoped)
            .collect(java.util.stream.Collectors.toMap(
                port -> ((ProviderScoped) port).getProviderType(),
                port -> port,
                (existing, replacement) -> {
                    log.warn("중복된 S3 버킷 Discovery 포트 발견: provider={}, existing={}, replacement={}", 
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
                    log.warn("중복된 S3 버킷 Management 포트 발견: provider={}, existing={}, replacement={}", 
                            ((ProviderScoped) existing).getProviderType(), 
                            existing.getClass().getSimpleName(), 
                            replacement.getClass().getSimpleName());
                    return existing; // 기존 것을 유지
                }
            ));

        log.info("S3 버킷 포트 라우터 초기화 완료: discoveryPorts={}, managementPorts={}", 
                discoveryPorts.keySet(), managementPorts.keySet());
    }

    /**
     * 지정된 프로바이더 타입에 해당하는 S3 버킷 발견 포트를 반환합니다.
     * 
     * @param providerType 클라우드 프로바이더 타입
     * @return S3 버킷 발견 포트
     * @throws IllegalArgumentException 지원하지 않는 프로바이더 타입인 경우
     */
    public S3BucketDiscoveryPort discovery(CloudProvider.ProviderType providerType) {
        S3BucketDiscoveryPort port = discoveryPorts.get(providerType);
        if (port == null) {
            log.error("지원하지 않는 프로바이더 타입: {}, 지원되는 타입: {}", providerType, discoveryPorts.keySet());
            throw new IllegalArgumentException("지원하지 않는 프로바이더 타입입니다: " + providerType);
        }
        
        log.debug("S3 버킷 Discovery 포트 선택: provider={}, port={}", 
                providerType, port.getClass().getSimpleName());
        
        return port;
    }

    /**
     * 지정된 프로바이더 타입에 해당하는 S3 버킷 관리 포트를 반환합니다.
     * 
     * @param providerType 클라우드 프로바이더 타입
     * @return S3 버킷 관리 포트
     * @throws IllegalArgumentException 지원하지 않는 프로바이더 타입인 경우
     */
    public S3BucketManagementPort management(CloudProvider.ProviderType providerType) {
        S3BucketManagementPort port = managementPorts.get(providerType);
        if (port == null) {
            log.error("지원하지 않는 프로바이더 타입: {}, 지원되는 타입: {}", providerType, managementPorts.keySet());
            throw new IllegalArgumentException("지원하지 않는 프로바이더 타입입니다: " + providerType);
        }
        
        log.debug("S3 버킷 Management 포트 선택: provider={}, port={}", 
                providerType, port.getClass().getSimpleName());
        
        return port;
    }

    /**
     * 지원되는 프로바이더 타입 목록을 반환합니다.
     * 
     * @return 지원되는 프로바이더 타입 목록
     */
    public java.util.Set<CloudProvider.ProviderType> getSupportedProviders() {
        return discoveryPorts.keySet();
    }

    /**
     * 지정된 프로바이더 타입이 지원되는지 확인합니다.
     * 
     * @param providerType 클라우드 프로바이더 타입
     * @return 지원 여부
     */
    public boolean isProviderSupported(CloudProvider.ProviderType providerType) {
        return discoveryPorts.containsKey(providerType) && managementPorts.containsKey(providerType);
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
