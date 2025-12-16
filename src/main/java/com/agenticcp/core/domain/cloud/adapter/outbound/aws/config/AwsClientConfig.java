package com.agenticcp.core.domain.cloud.adapter.outbound.aws.config;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.account.AwsSessionCredential;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.sts.StsClient;

/**
 * AWS Client 생성을 위한 유틸리티 클래스
 * 
 * 동적 자격증명 및 세션으로 AWS 클라이언트를 생성합니다.
 * PR #160 패턴에 따라 공통 헬퍼 메서드를 추출하여 일관된 클라이언트 생성을 지원합니다.
 * 
 * @author AgenticCP Team
 * @version 1.1.0
 */
@Component
public class AwsClientConfig {

    /**
     * 동적 자격증명으로 STS Client를 생성합니다.
     * GetCallerIdentity API 호출 등 계정 검증에 사용됩니다.
     * 
     * @param accessKeyId AWS Access Key ID
     * @param secretAccessKey AWS Secret Access Key
     * @param region AWS 리전 (예: us-east-1)
     * @return StsClient 인스턴스
     */
    public StsClient createStsClient(String accessKeyId, String secretAccessKey, String region) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);

        return StsClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of(resolveRegion(region)))
                .build();
    }

    /**
     * 세션 자격증명으로 EC2 Client를 생성합니다.
     * STS 세션 토큰을 포함한 임시 자격증명으로 클라이언트를 생성합니다.
     * 
     * @param session AWS 세션 자격증명
     * @param region AWS 리전 (예: us-east-1, null이면 세션의 region 사용)
     * @return Ec2Client 인스턴스
     * @throws BusinessException 세션이 유효하지 않거나 AWS 세션이 아닌 경우
     */
    public Ec2Client createEc2Client(CloudSessionCredential session, String region) {
        AwsSessionCredential awsSession = validateAndCastSession(session);
        String targetRegion = region != null ? region : awsSession.getRegion();

        return Ec2Client.builder()
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

