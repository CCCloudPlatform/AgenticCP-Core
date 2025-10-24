package com.agenticcp.core.domain.cloud.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

/**
 * AWS S3 설정 클래스
 * S3Client Bean을 생성
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Configuration
public class AwsS3Config {

    @Value("${aws.s3.region:us-east-1}")
    private String defaultRegion;

    @Value("${aws.s3.endpoint:}")
    private String endpoint;

    @Bean
    public S3Client s3Client() {
        log.info("Creating S3Client with default region: {}", defaultRegion);
        
        var builder = S3Client.builder()
                .region(Region.of(defaultRegion));
        
        // 커스텀 엔드포인트 설정 (LocalStack 등 테스트 환경용)
        if (endpoint != null && !endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(endpoint));
            log.debug("Using custom endpoint: {}", endpoint);
        }
        
        S3Client client = builder.build();
        log.info("S3Client created successfully");
        
        return client;
    }
}
