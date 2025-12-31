package com.agenticcp.core.domain.cloud.adapter.outbound.aws.config;

import com.agenticcp.core.domain.cloud.capability.CapabilityRegistry;
import com.agenticcp.core.domain.cloud.capability.CspCapability;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * AWS Capability 등록 설정
 *
 * 애플리케이션 시작 시 AWS 관련 Capability를 등록합니다.
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AwsCapabilityConfig {

    private final CapabilityRegistry capabilityRegistry;

    @PostConstruct
    public void initializeVpcCapabilities() {
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

    @PostConstruct
    public void initializeVmCapabilities() {
        log.info("Registering AWS VM capabilities...");

        CspCapability awsVmCapability = CspCapability.builder()
                .supportsStart(true)      // VM 인스턴스 시작 지원
                .supportsStop(true)       // VM 인스턴스 중지 지원
                .supportsTerminate(true)  // VM 인스턴스 종료 지원
                .supportsTagging(true)    // AWS EC2 인스턴스 태그 지원
                .build();

        capabilityRegistry.register(
                CloudProvider.ProviderType.AWS,
                "VM",           // 서비스 타입
                "INSTANCE",     // 리소스 타입
                awsVmCapability
        );

        log.info("AWS VM capabilities registered successfully - AWS|VM|INSTANCE");
    }

    @PostConstruct
    public void initializeS3BucketCapabilities() {
        log.info("Registering AWS S3 Bucket capabilities...");

        CspCapability awsS3Capability = CspCapability.builder()
                .supportsStart(false)      // S3 버킷은 start/stop 개념이 없음
                .supportsStop(false)
                .supportsTerminate(true)   // S3 버킷 삭제 지원
                .supportsTagging(true)     // AWS S3 버킷 태그 지원
                .supportsListByTag(true)   // 태그 기반 목록 조회 지원
                .build();

        capabilityRegistry.register(
                CloudProvider.ProviderType.AWS,
                "S3",  // 서비스 타입
                "BUCKET",       // 리소스 타입
                awsS3Capability
        );

        log.info("AWS S3 Bucket capabilities registered successfully - AWS|S3|BUCKET");
    }

    @PostConstruct
    public void initializeDnsCapabilities() {
        log.info("Registering AWS DNS capabilities...");

        CspCapability awsDnsCapability = CspCapability.builder()
                .supportsStart(false)      // DNS 호스팅 존은 start/stop 개념이 없음
                .supportsStop(false)
                .supportsTerminate(true)   // DNS 호스팅 존 삭제 지원
                .supportsTagging(true)     // AWS Route53 호스팅 존 태그 지원
                .supportsListByTag(true)   // 태그 기반 목록 조회 지원
                .build();

        capabilityRegistry.register(
                CloudProvider.ProviderType.AWS,
                "ROUTE53",      // 서비스 키
                "DNS_ZONE",     // 리소스 타입
                awsDnsCapability
        );
  
        log.info("AWS DNS capabilities registered successfully - AWS|ROUTE53|DNS_ZONE");
    }
  
    @PostConstruct
    public void initializeCloudFrontCapabilities() {
        log.info("Registering AWS CloudFront capabilities...");

        CspCapability awsCloudFrontCapability = CspCapability.builder()
                .supportsStart(false)      // CloudFront Distribution은 start/stop 개념이 없음
                .supportsStop(false)
                .supportsTerminate(true)   // Distribution 삭제 지원
                .supportsTagging(true)     // AWS CloudFront Distribution 태그 지원
                .supportsListByTag(true)   // 태그 기반 목록 조회 지원
                .build();
        capabilityRegistry.register(
                CloudProvider.ProviderType.AWS,
                "CloudFront",           // 서비스 타입
                "CDN_DISTRIBUTION",     // 리소스 타입
                awsCloudFrontCapability
        );
        log.info("AWS CloudFront capabilities registered successfully - AWS|CloudFront|CDN_DISTRIBUTION");
    }
  
    @PostConstruct
    public void initializeRdsCapabilities() {
        log.info("Registering AWS RDS capabilities...");

        CspCapability awsRdsCapability = CspCapability.builder()
                .supportsStart(true)      // RDS 인스턴스 시작 지원 (일부 엔진만 지원)
                .supportsStop(true)       // RDS 인스턴스 중지 지원 (일부 엔진만 지원)
                .supportsTerminate(true)  // RDS 인스턴스 삭제 지원
                .supportsTagging(true)    // AWS RDS 인스턴스 태그 지원
                .supportsListByTag(true)  // 태그 기반 목록 조회 지원
                .build();

        capabilityRegistry.register(
                CloudProvider.ProviderType.AWS,
                "RDS",  // 서비스 타입
                "DATABASE",       // 리소스 타입
                awsRdsCapability
        );

        log.info("AWS RDS capabilities registered successfully - AWS|RDS|DATABASE");
    }
}
