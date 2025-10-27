package com.agenticcp.core.domain.cloud.service.aws;

import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.port.outbound.aws.Ec2ManagementPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * EC2 포트 라우터
 * 
 * 다양한 클라우드 제공업체의 EC2 관리 포트를 관리하고,
 * 요청된 제공업체 타입에 따라 적절한 포트를 선택하여 반환합니다.
 * 
 * 핵사고날 아키텍처의 라우터 계층에 해당하며, 제공업체별 어댑터를 동적으로 선택합니다.
 */
@Slf4j
@Component
public class Ec2PortRouter {

    private final Map<ProviderType, Ec2ManagementPort> ec2Ports = new EnumMap<>(ProviderType.class);

    /**
     * EC2 포트 라우터 생성자
     * 
     * @param ports 등록된 모든 Ec2ManagementPort 구현체들
     */
    public Ec2PortRouter(List<Ec2ManagementPort> ports) {
        log.info("[Ec2PortRouter] Initializing EC2 port router with {} ports", ports.size());
        
        ports.stream()
            .filter(p -> p instanceof ProviderScoped)
            .forEach(p -> {
                ProviderType providerType = ((ProviderScoped) p).getProviderType();
                ec2Ports.put(providerType, p);
                log.debug("[Ec2PortRouter] Registered EC2 port for provider: {} -> {}", 
                    providerType, p.getClass().getSimpleName());
            });
        
        log.info("[Ec2PortRouter] EC2 port router initialized with {} providers: {}", 
            ec2Ports.size(), ec2Ports.keySet());
    }

    /**
     * 지정된 제공업체 타입에 해당하는 EC2 관리 포트를 반환합니다.
     * 
     * @param type 클라우드 제공업체 타입
     * @return 해당 제공업체의 Ec2ManagementPort 구현체
     * @throws IllegalArgumentException 지원되지 않는 제공업체 타입인 경우
     */
    public Ec2ManagementPort ec2(ProviderType type) {
        log.debug("[Ec2PortRouter] Requesting EC2 port for provider: {}", type);
        
        Ec2ManagementPort port = ec2Ports.get(type);
        if (port == null) {
            log.error("[Ec2PortRouter] Unsupported provider type: {}. Available providers: {}", 
                type, ec2Ports.keySet());
            throw new IllegalArgumentException("Unsupported provider: " + type + 
                ". Available providers: " + ec2Ports.keySet());
        }
        
        log.debug("[Ec2PortRouter] Returning EC2 port: {} for provider: {}", 
            port.getClass().getSimpleName(), type);
        return port;
    }

    /**
     * 현재 등록된 모든 제공업체 타입을 반환합니다.
     * 
     * @return 등록된 제공업체 타입들의 Set
     */
    public java.util.Set<ProviderType> getSupportedProviders() {
        return ec2Ports.keySet();
    }

    /**
     * 지정된 제공업체가 지원되는지 확인합니다.
     * 
     * @param type 확인할 제공업체 타입
     * @return 지원 여부
     */
    public boolean isProviderSupported(ProviderType type) {
        return ec2Ports.containsKey(type);
    }

    /**
     * 등록된 EC2 포트의 개수를 반환합니다.
     * 
     * @return 등록된 포트 개수
     */
    public int getPortCount() {
        return ec2Ports.size();
    }
}
