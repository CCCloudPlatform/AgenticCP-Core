package com.agenticcp.core.domain.cloud.capability;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class VmCapabilityConfig {

    private final CapabilityRegistry capabilityRegistry;

    @PostConstruct
    public void initializeVmCapabilities() {
        log.info("[VmCapabilityConfig] Register VM capabilities for AWS");
        capabilityRegistry.register(
            ProviderType.AWS,
            "VM",
            "INSTANCE",
            CspCapability.builder()
                .supportsStart(true)
                .supportsStop(true)
                .supportsTerminate(true)
                .supportsTagging(true)
                .build()
        );
    }
}


