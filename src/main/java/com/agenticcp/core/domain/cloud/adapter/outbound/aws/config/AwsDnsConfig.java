package com.agenticcp.core.domain.cloud.adapter.outbound.aws.config;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.account.AwsSessionCredential;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.route53.Route53Client;

/**
 * AWS Route53 설정 클래스
 * 
 * Route53Client 생성을 담당합니다.
 * Route53은 글로벌 서비스이므로 리전이 중요하지 않지만,
 * 일관성을 위해 세션의 리전을 사용합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "aws.enabled", havingValue = "true", matchIfMissing = true)
public class AwsDnsConfig {

    /**
     * 세션 자격증명으로 Route53 Client를 생성합니다.
     * 
     * Route53은 글로벌 서비스이므로 리전이 중요하지 않지만,
     * 일관성을 위해 세션의 리전을 사용합니다.
     * 
     * @param session AWS 세션 자격증명
     * @param region AWS 리전 (Route53은 글로벌 서비스이므로 무시될 수 있음)
     * @return Route53Client 인스턴스
     */
    public Route53Client createRoute53Client(CloudSessionCredential session, String region) {
        AwsSessionCredential awsSession = validateAndCastSession(session);
        
        return Route53Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(toSdkCredentials(awsSession)))
                .region(Region.AWS_GLOBAL)  // Route53은 글로벌 서비스
                .build();
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
