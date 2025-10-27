package com.agenticcp.core.domain.cloud.config;

import com.agenticcp.core.domain.cloud.capability.CapabilityRegistry;
import com.agenticcp.core.domain.cloud.capability.CspCapability;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * S3 버킷 Capability 등록 설정
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
// TODO: AwsCapabilityConfig 로 통합?
public class S3BucketCapabilityConfig {

    private final CapabilityRegistry capabilityRegistry;

    @PostConstruct
    public void initializeS3BucketCapabilities() {
        // AWS S3 버킷 Capability 등록
        CspCapability awsS3Capability = CspCapability.builder()
                .supportsStart(false)
                .supportsStop(false)
                .supportsTerminate(true)
                .supportsTagging(true)
                .supportsListByTag(true)
                .build();

        capabilityRegistry.register(ProviderType.AWS, "S3", "BUCKET", awsS3Capability);
    }
}
