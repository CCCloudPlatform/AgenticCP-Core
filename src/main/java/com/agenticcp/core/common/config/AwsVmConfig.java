package com.agenticcp.core.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.services.ec2.Ec2Client;

import java.net.URI;

/**
 * AWS VM(EC2) 클라이언트 설정
 *
 * AWS CLI에 설정된 자격증명을 사용하여 EC2 클라이언트를 구성합니다.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "aws.enabled", havingValue = "true", matchIfMissing = true)
public class AwsVmConfig {
    
    @Value("${aws.region:us-east-1}")
    private String region;
    
    @Value("${aws.endpoint-override:}")
    private String endpointOverride;
    
    @Value("${aws.connection-timeout:30000}")
    private int connectionTimeout;
    
    @Value("${aws.read-timeout:30000}")
    private int readTimeout;
    
    @Bean
    public Ec2Client ec2Client() {
        log.info("[AwsVmConfig] Creating EC2 client for region: {}", region);
        
        try {
            // AWS SDK Builder 패턴 사용 - DefaultCredentialsProvider 사용 (AWS CLI 자격증명)
            var builder = Ec2Client.builder()
                .region(software.amazon.awssdk.regions.Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create());
            
            // 테스트용 엔드포인트 오버라이드 (LocalStack 등)
            if (!endpointOverride.isEmpty()) {
                log.info("[AwsVmConfig] Using custom endpoint: {}", endpointOverride);
                builder.endpointOverride(URI.create(endpointOverride));
            }
            
            Ec2Client client = builder.build();
            log.info("[AwsVmConfig] EC2 client created successfully for region: {}", region);
            log.info("[AwsVmConfig] Using AWS CLI credentials (DefaultCredentialsProvider)");
            return client;
            
        } catch (Exception e) {
            log.error("[AwsVmConfig] Failed to create EC2 client: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create AWS EC2 client", e);
        }
    }
}