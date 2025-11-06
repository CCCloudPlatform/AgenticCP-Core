package com.agenticcp.core.domain.cloud.config;

import com.agenticcp.core.domain.cloud.adapter.outbound.common.ThreadLocalCredentialCache;
import com.agenticcp.core.common.context.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.resourcegroupstaggingapi.ResourceGroupsTaggingApiClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sts.StsClient;

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
// TODO: AwsConfig로 통합?
public class AwsS3Config {

    @Value("${aws.s3.region:us-east-1}")
    private String defaultRegion;

    @Value("${aws.s3.endpoint:}")
    private String endpoint;

    @Bean
    public S3Client s3Client() {
        var builder = S3Client.builder()
                .region(Region.of(defaultRegion))
                .credentialsProvider(createThreadLocalCredentialsProvider());
        
        // 커스텀 엔드포인트 설정 (LocalStack 등 테스트 환경용)
        if (endpoint != null && !endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }

    @Bean
    public ResourceGroupsTaggingApiClient resourceGroupsTaggingApiClient() {
        var builder = ResourceGroupsTaggingApiClient.builder()
                .region(Region.of(defaultRegion))
                .credentialsProvider(createThreadLocalCredentialsProvider());

        if (endpoint != null && !endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }
    
    @Bean
    public StsClient stsClient() {
        return StsClient.builder()
                .region(Region.of(defaultRegion))
                .build();
    }
    
    /**
     * ThreadLocal 기반 자격증명 제공자 생성
     * 
     * 현재 스레드의 ThreadLocal 캐시에서 자격증명을 조회하여 제공합니다.
     * 멀티 테넌트 환경에서 각 요청별로 다른 자격증명을 사용할 수 있도록 합니다.
     */
    private AwsCredentialsProvider createThreadLocalCredentialsProvider() {
        return () -> {
            try {
                // 현재 테넌트 키 조회
                String tenantKey = TenantContextHolder.getCurrentTenantKey();
                
                if (tenantKey == null) {
                    log.warn("테넌트 컨텍스트가 설정되지 않음 - 기본 자격증명 사용");
                    return null; // 기본 자격증명 사용
                }
                
                // ThreadLocal 캐시에서 자격증명 조회
                // 현재는 첫 번째 캐시된 자격증명을 반환
                // TODO: 실제로는 요청 컨텍스트에서 프로바이더 타입과 계정 스코프를 가져와야 함
                return ThreadLocalCredentialCache.getFirstCredentials();
                
            } catch (Exception e) {
                log.error("ThreadLocal 자격증명 조회 실패: {}", e.getMessage(), e);
                return null; // 기본 자격증명 사용
            }
        };
    }
}
