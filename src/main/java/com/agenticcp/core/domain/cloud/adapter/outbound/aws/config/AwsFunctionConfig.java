package com.agenticcp.core.domain.cloud.adapter.outbound.aws.config;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.account.AwsSessionCredential;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;

/**
 * AWS Lambda Client 생성을 위한 설정 클래스
 *
 * 세션 자격증명 기반으로 LambdaClient를 생성합니다.
 * Lambda는 리전별로 관리되므로 리전을 명시적으로 지정해야 합니다.
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Component
public class AwsFunctionConfig {

    /**
     * 세션 자격증명으로 Lambda Client를 생성합니다.
     *
     * @param session AWS 세션 자격증명
     * @param region AWS 리전 (Lambda는 리전별로 관리)
     * @return LambdaClient 인스턴스
     * @throws BusinessException 세션이 유효하지 않거나 AWS 세션이 아닌 경우
     */
    public LambdaClient createLambdaClient(CloudSessionCredential session, String region) {
        AwsSessionCredential awsSession = validateAndCastSession(session);
        String targetRegion = region != null ? region : awsSession.getRegion();

        return LambdaClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(toSdkCredentials(awsSession)))
                .region(Region.of(resolveRegion(targetRegion)))
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

    /**
     * 리전 기본값 처리
     */
    private String resolveRegion(String region) {
        return (region != null && !region.isBlank()) ? region : "us-east-1";
    }
}
