package com.agenticcp.core.domain.cloud.adapter.outbound.aws.config;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.account.AwsSessionCredential;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.resourcegroupstaggingapi.ResourceGroupsTaggingApiClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sts.StsClient;

/**
 * AWS S3 설정 클래스
 * 
 * 세션 자격증명 기반으로 S3Client, ResourceGroupsTaggingApiClient를 생성합니다.
 * StsClient Bean을 생성합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "aws.enabled", havingValue = "true", matchIfMissing = true)
public class AwsS3Config {

    @Value("${aws.s3.region:us-east-1}")
    private String defaultRegion;

    /**
     * 세션 자격증명으로 S3 Client를 생성합니다.
     * 
     * @param session AWS 세션 자격증명
     * @param region AWS 리전 (S3는 리전이 중요하므로 명시적 전달 권장, null이면 세션 리전 사용)
     * @return S3Client 인스턴스
     */
    public S3Client createS3Client(CloudSessionCredential session, String region) {
        AwsSessionCredential awsSession = validateAndCastSession(session);
        String targetRegion = region != null ? region : awsSession.getRegion();

        return S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(toSdkCredentials(awsSession)))
                .region(Region.of(resolveRegion(targetRegion)))
                .build();
    }

    /**
     * 세션 자격증명으로 ResourceGroupsTaggingApiClient를 생성합니다.
     * 태그 기반 리소스 조회에 사용됩니다.
     * 
     * @param session AWS 세션 자격증명
     * @param region AWS 리전 (null이면 세션 리전 사용)
     * @return ResourceGroupsTaggingApiClient 인스턴스
     */
    public ResourceGroupsTaggingApiClient createResourceGroupsTaggingApiClient(CloudSessionCredential session, String region) {
        AwsSessionCredential awsSession = validateAndCastSession(session);
        String targetRegion = region != null ? region : awsSession.getRegion();

        return ResourceGroupsTaggingApiClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(toSdkCredentials(awsSession)))
                .region(Region.of(resolveRegion(targetRegion)))
                .build();
    }
    
    /**
     * StsClient Bean을 생성합니다.
     * 계정 검증 및 세션 토큰 발급에 사용됩니다.
     * 
     * @return StsClient 인스턴스
     */
    @Bean
    public StsClient stsClient() {
        return StsClient.builder()
                .region(Region.of(resolveRegion(defaultRegion)))
                .build();
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
