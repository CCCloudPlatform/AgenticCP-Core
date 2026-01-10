package com.agenticcp.core.domain.cloud.adapter.outbound.aws.config;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.account.AwsSessionCredential;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.ThreadLocalCredentialCache;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

/**
 * AWS DynamoDB 클라이언트 설정 클래스
 * 
 * DynamoDbClient Bean을 생성합니다.
 * ThreadLocal 기반 자격증명을 사용하여 멀티 테넌트 환경을 지원합니다.
 * S3Config, VmConfig와 동일한 패턴을 따릅니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2026-01-10
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "aws.enabled", havingValue = "true", matchIfMissing = true)
public class AwsDynamoDbConfig {

    @Value("${aws.region:us-east-1}")
    private String defaultRegion;

    @Value("${aws.endpoint-override:}")
    private String endpointOverride;

    @Value("${aws.dynamodb.endpoint:}")
    private String dynamoDbEndpoint;

    @Value("${aws.connection-timeout:30000}")
    private int connectionTimeout;

    @Value("${aws.read-timeout:30000}")
    private int readTimeout;

    /**
     * DynamoDbClient Bean을 생성합니다.
     * ThreadLocal 기반 자격증명을 사용하여 동적 자격증명을 지원합니다.
     * 
     * @return DynamoDbClient 인스턴스
     */
    @Bean
    public DynamoDbClient dynamoDbClient() {
        log.info("[AwsDynamoDbConfig] Creating DynamoDB client for region: {}", defaultRegion);

        try {
            var builder = DynamoDbClient.builder()
                    .region(Region.of(resolveRegion(defaultRegion)))
                    .credentialsProvider(createThreadLocalCredentialsProvider());

            // 커스텀 엔드포인트 설정 (LocalStack 등 테스트 환경용)
            String endpoint = resolveEndpoint();
            if (endpoint != null && !endpoint.isEmpty()) {
                log.info("[AwsDynamoDbConfig] Using custom endpoint: {}", endpoint);
                builder.endpointOverride(URI.create(endpoint));
            }

            DynamoDbClient client = builder.build();
            log.info("[AwsDynamoDbConfig] DynamoDB client created successfully for region: {}", defaultRegion);
            return client;

        } catch (Exception e) {
            log.error("[AwsDynamoDbConfig] Failed to create DynamoDB client: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create AWS DynamoDB client", e);
        }
    }

    /**
     * 세션 자격증명으로 DynamoDB Client를 생성합니다.
     * STS 세션 토큰을 포함한 임시 자격증명으로 클라이언트를 생성합니다.
     * 
     * @param session AWS 세션 자격증명
     * @param region AWS 리전 (null이면 세션의 region 사용)
     * @return DynamoDbClient 인스턴스
     * @throws BusinessException 세션이 유효하지 않거나 AWS 세션이 아닌 경우
     */
    public DynamoDbClient createDynamoDbClient(CloudSessionCredential session, String region) {
        AwsSessionCredential awsSession = validateAndCastSession(session);
        String targetRegion = region != null ? region : awsSession.getRegion();

        log.debug("[AwsDynamoDbConfig] Creating DynamoDB client with session for region: {}", targetRegion);

        var builder = DynamoDbClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(toSdkCredentials(awsSession)))
                .region(Region.of(resolveRegion(targetRegion)));

        // 커스텀 엔드포인트 설정
        String endpoint = resolveEndpoint();
        if (endpoint != null && !endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        return builder.build();
    }

    /**
     * 기본 자격증명으로 DynamoDB Client를 생성합니다.
     * AWS CLI 또는 환경 변수에 설정된 자격증명을 사용합니다.
     * 
     * @param region AWS 리전 (null이면 기본 리전 사용)
     * @return DynamoDbClient 인스턴스
     */
    public DynamoDbClient createDynamoDbClientWithDefaultCredentials(String region) {
        String targetRegion = region != null ? region : defaultRegion;

        log.debug("[AwsDynamoDbConfig] Creating DynamoDB client with default credentials for region: {}", targetRegion);

        var builder = DynamoDbClient.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .region(Region.of(resolveRegion(targetRegion)));

        // 커스텀 엔드포인트 설정
        String endpoint = resolveEndpoint();
        if (endpoint != null && !endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        return builder.build();
    }

    /**
     * 엔드포인트 결정
     * DynamoDB 전용 엔드포인트가 있으면 우선 사용, 없으면 공통 엔드포인트 사용
     */
    private String resolveEndpoint() {
        if (dynamoDbEndpoint != null && !dynamoDbEndpoint.isEmpty()) {
            return dynamoDbEndpoint;
        }
        return endpointOverride;
    }

    /**
     * 리전 기본값 처리
     * 
     * @param region AWS 리전 (null이거나 빈 문자열이면 us-east-1 반환)
     * @return 처리된 리전 문자열
     */
    private String resolveRegion(String region) {
        return (region != null && !region.isBlank()) ? region : "us-east-1";
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
                    log.warn("[AwsDynamoDbConfig] 테넌트 컨텍스트가 설정되지 않음 - 기본 자격증명 사용");
                    return null; // 기본 자격증명 사용
                }

                // ThreadLocal 캐시에서 자격증명 조회
                return ThreadLocalCredentialCache.getFirstCredentials();

            } catch (Exception e) {
                log.error("[AwsDynamoDbConfig] ThreadLocal 자격증명 조회 실패: {}", e.getMessage(), e);
                return null; // 기본 자격증명 사용
            }
        };
    }

    /**
     * 세션 검증 및 AWS 세션으로 캐스팅
     * 
     * @param session 도메인 세션 자격증명
     * @return AWS 세션 자격증명
     * @throws BusinessException 세션이 유효하지 않거나 AWS 세션이 아닌 경우
     */
    private AwsSessionCredential validateAndCastSession(CloudSessionCredential session) {
        if (session == null) {
            throw new BusinessException(CloudErrorCode.CLOUD_CONNECTION_FAILED, "세션 자격증명이 필요합니다.");
        }
        if (!(session instanceof AwsSessionCredential awsSession)) {
            throw new BusinessException(CloudErrorCode.CLOUD_CONNECTION_FAILED,
                    "AWS 세션 자격증명이 필요합니다. 제공된 타입: " + session.getClass().getSimpleName());
        }
        if (!awsSession.isValid()) {
            throw new BusinessException(CloudErrorCode.CLOUD_CONNECTION_FAILED,
                    "세션이 만료되었습니다. expiresAt: " + awsSession.getExpiresAt());
        }
        return awsSession;
    }

    /**
     * 도메인 세션 객체를 AWS SDK 세션 객체로 변환
     * 
     * @param session AWS 도메인 세션 자격증명
     * @return AWS SDK 세션 자격증명
     */
    private AwsSessionCredentials toSdkCredentials(AwsSessionCredential session) {
        return AwsSessionCredentials.create(
                session.getAccessKeyId(),
                session.getSecretAccessKey(),
                session.getSessionToken()
        );
    }
}

