package com.agenticcp.core.domain.cloud.service.vpc;

import com.agenticcp.core.domain.cloud.port.outbound.vpc.VpcManagementPort;
import org.springframework.stereotype.Component;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class VpcPortRouter {
    
    private final Map<ProviderType, VpcManagementPort> vpcPorts;

    public VpcPortRouter(List<VpcManagementPort> vpcManagementPorts) {
        this.vpcPorts = vpcManagementPorts.stream()
                .filter(port -> port instanceof ProviderScoped)
                .collect(Collectors.toMap(
                        port -> ((ProviderScoped) port).getProviderType(),
                        Function.identity()
                ));
    }
    
    public VpcManagementPort getPort(ProviderType provider) {
        VpcManagementPort port = vpcPorts.get(provider);
        
        if (port == null) {
            throw new IllegalArgumentException("지원하지 않는 프로바이더입니다: " + provider);
        }
        
        return port;
    }
}