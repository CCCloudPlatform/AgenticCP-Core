package com.agenticcp.core.domain.cloud.service.aws;

import org.springframework.stereotype.Component;

import com.agenticcp.core.domain.cloud.port.outbound.aws.VpcManagementPort;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;

import java.util.Map;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class VpcPortRouter {
    
    private final Map<ProviderType, VpcManagementPort> vpcPorts = new EnumMap<>(ProviderType.class);
    
    public VpcManagementPort getPort(ProviderType provider) {
        return vpcPorts.get(provider);
    }
}