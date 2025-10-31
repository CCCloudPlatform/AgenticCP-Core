package com.agenticcp.core.common.config;

import com.agenticcp.core.domain.cloud.capability.CapabilityRegistry;
import com.agenticcp.core.domain.cloud.capability.CspCapability;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * AWS VPC Capability 등록 설정
 * 
 * 애플리케이션 시작 시 AWS VPC 관련 Capability를 등록합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AwsVpcCapabilityConfig {

    private final CapabilityRegistry capabilityRegistry;

    @PostConstruct
    public void init() {
        log.info("Registering AWS VPC capabilities...");
        
        CspCapability awsVpcCapability = CspCapability.builder()
                .supportsStart(false)      // VPC는 가상 네트워크로 start/stop 개념이 없음
                .supportsStop(false)
                .supportsTerminate(true)   // VPC는 삭제(terminate) 가능
                .supportsTagging(true)     // AWS VPC 태그 지원
                .supportsListByTag(true)   // 태그 기반 목록 조회 지원
                .build();
        
        capabilityRegistry.register(
                CloudProvider.ProviderType.AWS,
                "EC2",           // AWS VPC는 EC2 서비스에 속함
                "VPC",          // 리소스 타입
                awsVpcCapability
        );
        
        log.info("AWS VPC capabilities registered successfully - AWS|EC2|VPC");
    }
}

