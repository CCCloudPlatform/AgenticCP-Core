package com.agenticcp.core.domain.cloud.service.dns;

import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.port.outbound.dns.DnsDiscoveryPort;
import com.agenticcp.core.domain.cloud.port.outbound.dns.DnsManagementPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class DnsPortRouter {
    
    private final Map<ProviderType, DnsManagementPort> managementPorts;
    private final Map<ProviderType, DnsDiscoveryPort> discoveryPorts;

    public DnsPortRouter(
            List<DnsManagementPort> dnsManagementPorts,
            List<DnsDiscoveryPort> dnsDiscoveryPorts) {
        this.managementPorts = dnsManagementPorts.stream()
                .filter(port -> port instanceof ProviderScoped)
                .collect(Collectors.toMap(
                        port -> ((ProviderScoped) port).getProviderType(),
                        Function.identity()
                ));
        this.discoveryPorts = dnsDiscoveryPorts.stream()
                .filter(port -> port instanceof ProviderScoped)
                .collect(Collectors.toMap(
                        port -> ((ProviderScoped) port).getProviderType(),
                        Function.identity()
                ));
    }
    
    public DnsManagementPort management(ProviderType provider) {
        DnsManagementPort port = managementPorts.get(provider);
        
        if (port == null) {
            throw new IllegalArgumentException("지원하지 않는 프로바이더입니다: " + provider);
        }
        
        return port;
    }
    
    public DnsDiscoveryPort discovery(ProviderType provider) {
        DnsDiscoveryPort port = discoveryPorts.get(provider);
        
        if (port == null) {
            throw new IllegalArgumentException("지원하지 않는 프로바이더입니다: " + provider);
        }
        
        return port;
    }
}
