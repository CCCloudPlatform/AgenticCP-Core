package com.agenticcp.core.domain.cloud.adapter.outbound.aws.config;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.account.AwsSessionCredential;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.ThreadLocalCredentialCache;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;

/**
 * AWS CloudFront 설정 클래스
 * 
 * CloudFrontClient Bean을 생성합니다.
 * ThreadLocal 기반 자격증명을 사용하여 멀티 테넌트 환경을 지원합니다.
 * CloudFront는 글로벌 서비스이므로 AWS_GLOBAL 리전을 사용합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Configuration
public class AwsCloudFrontConfig {

    /**
     * CloudFrontClient Bean을 생성합니다.
     * ThreadLocal 기반 자격증명을 사용하여 동적 자격증명을 지원합니다.
     * CloudFront는 글로벌 서비스로 리전 독립적입니다.
     * 
     * @return CloudFrontClient 인스턴스
     */
    @Bean
    public CloudFrontClient cloudFrontClient() {
        return CloudFrontClient.builder()
                .region(Region.AWS_GLOBAL)  // CloudFront는 글로벌 서비스
                .credentialsProvider(createThreadLocalCredentialsProvider())
                .build();
    }

    /**
     * 세션 자격증명으로 CloudFront Client를 생성합니다.
     * 
     * @param session AWS 세션 자격증명
     * @return CloudFrontClient 인스턴스
     */
    public CloudFrontClient createCloudFrontClient(CloudSessionCredential session) {
        AwsSessionCredential awsSession = validateAndCastSession(session);

        return CloudFrontClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(toSdkCredentials(awsSession)))
                .region(Region.AWS_GLOBAL)  // CloudFront는 글로벌 서비스
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
                return ThreadLocalCredentialCache.getFirstCredentials();
                
            } catch (Exception e) {
                log.error("ThreadLocal 자격증명 조회 실패: {}", e.getMessage(), e);
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

