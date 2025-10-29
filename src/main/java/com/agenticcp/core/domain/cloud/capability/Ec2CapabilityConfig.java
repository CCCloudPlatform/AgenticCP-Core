package com.agenticcp.core.domain.cloud.capability;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class Ec2CapabilityConfig {

    private final CapabilityRegistry capabilityRegistry;

    @PostConstruct
    public void initializeEc2Capabilities() {
        log.info("[Ec2CapabilityConfig] Register EC2 capabilities for AWS");
        capabilityRegistry.register(
            ProviderType.AWS,
            "EC2",
            "INSTANCE",
            CspCapability.builder()
                .supportsCreate(true)
                .supportsUpdate(true)
                .supportsStart(true)
                .supportsStop(true)
                .supportsTerminate(true)
                .supportsTagging(true)
                .build()
        );
    }
}


