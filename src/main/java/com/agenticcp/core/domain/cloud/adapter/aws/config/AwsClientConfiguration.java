package com.agenticcp.core.domain.cloud.adapter.aws.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.sts.StsClient;

/**
 * AWS SDK 클라이언트 설정
 * 
 * AWS SDK for Java v2 클라이언트들을 Spring Bean으로 등록합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-24
 */
@Slf4j
@Configuration
public class AwsClientConfiguration {
    
    @Value("${aws.region:us-east-1}")
    private String awsRegion;
    
    @Bean
    public StsClient stsClient() {
        log.info("[AwsClientConfiguration] Creating STS client for region: {}", awsRegion);
        
        return StsClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
    
    @Bean
    public IamClient iamClient() {
        log.info("[AwsClientConfiguration] Creating IAM client for region: {}", awsRegion);
        
        return IamClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
