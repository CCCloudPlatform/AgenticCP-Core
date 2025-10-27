package com.agenticcp.core.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.ec2.Ec2Client;

import java.net.URI;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "aws.enabled", havingValue = "true")
public class AwsConfig {
    
    @Value("${aws.region:us-east-1}")
    private String region;
    
    @Value("${aws.access-key-id:}")
    private String accessKeyId;
    
    @Value("${aws.secret-access-key:}")
    private String secretAccessKey;
    
    @Value("${aws.session-token:}")
    private String sessionToken;
    
    @Value("${aws.endpoint-override:}")
    private String endpointOverride;
    
    @Value("${aws.connection-timeout:30000}")
    private int connectionTimeout;
    
    @Value("${aws.read-timeout:30000}")
    private int readTimeout;
    
    @Bean
    public Ec2Client ec2Client() {
        log.info("[AwsConfig] Creating EC2 client for region: {}", region);
        
        // AWS SDK Builder 패턴 사용
        var builder = Ec2Client.builder()
            .region(software.amazon.awssdk.regions.Region.of(region))
            .credentialsProvider(createCredentialsProvider());
        
        // 테스트용 엔드포인트 오버라이드 (LocalStack 등)
        if (!endpointOverride.isEmpty()) {
            log.info("[AwsConfig] Using custom endpoint: {}", endpointOverride);
            builder.endpointOverride(URI.create(endpointOverride));
        }
        
        return builder.build();
    }
    
    /**
     * AWS 자격증명 프로바이더 생성
     * 1. 액세스 키/시크릿 키가 설정된 경우: StaticCredentialsProvider 사용
     * 2. 그렇지 않은 경우: DefaultCredentialsProvider 사용 (환경변수, IAM 역할 등)
     */
    private software.amazon.awssdk.auth.credentials.AwsCredentialsProvider createCredentialsProvider() {
        if (!accessKeyId.isEmpty() && !secretAccessKey.isEmpty()) {
            log.info("[AwsConfig] Using static credentials (access key: {})", 
                accessKeyId.substring(0, Math.min(4, accessKeyId.length())) + "****");
            
            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
            
            if (!sessionToken.isEmpty()) {
                // 세션 토큰이 있는 경우 임시 자격증명 사용
                AwsSessionCredentials sessionCredentials = AwsSessionCredentials.create(
                    accessKeyId, secretAccessKey, sessionToken);
                return StaticCredentialsProvider.create(sessionCredentials);
            } else {
                return StaticCredentialsProvider.create(credentials);
            }
        } else {
            log.info("[AwsConfig] Using default credentials provider (environment variables, IAM role, etc.)");
            return DefaultCredentialsProvider.create();
        }
    }
}
