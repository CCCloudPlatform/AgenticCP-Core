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
 * 동적 자격증명 및 세션으로 AWS 클라이언트를 생성합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
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
                .region(Region.of(region != null ? region : "us-east-1"))
                .build();
    }

    /**
     * 동적 자격증명으로 EC2 Client를 생성합니다.
     * EC2 인스턴스 조회, 관리 등에 사용됩니다.
     * 
     * @param accessKeyId AWS Access Key ID
     * @param secretAccessKey AWS Secret Access Key
     * @param region AWS 리전 (예: us-east-1)
     * @return Ec2Client 인스턴스
     */
    public Ec2Client createEc2Client(String accessKeyId, String secretAccessKey, String region) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
        
        return Ec2Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of(region != null ? region : "us-east-1"))
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
        if (session == null) {
            throw new BusinessException(CloudErrorCode.CLOUD_CONNECTION_FAILED,
                    "세션 자격증명이 필요합니다.");
        }

        if (!(session instanceof AwsSessionCredential awsSession)) {
            throw new BusinessException(CloudErrorCode.CLOUD_CONNECTION_FAILED,
                    "AWS 세션 자격증명이 필요합니다. 제공된 타입: " + session.getClass().getSimpleName());
        }

        if (!awsSession.isValid()) {
            throw new BusinessException(CloudErrorCode.CLOUD_CONNECTION_FAILED,
                    "세션이 만료되었습니다. expiresAt: " + awsSession.getExpiresAt());
        }

        // 세션 자격증명 생성 (sessionToken 포함)
        AwsSessionCredentials sessionCredentials = AwsSessionCredentials.builder()
                .accessKeyId(awsSession.getAccessKeyId())
                .secretAccessKey(awsSession.getSecretAccessKey())
                .sessionToken(awsSession.getSessionToken())
                .build();

        // 리전 결정: 파라미터 우선, 없으면 세션의 region 사용
        String resolvedRegion = region != null ? region : awsSession.getRegion();
        if (resolvedRegion == null || resolvedRegion.isBlank()) {
            resolvedRegion = "us-east-1"; // 기본값
        }

        return Ec2Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(sessionCredentials))
                .region(Region.of(resolvedRegion))
                .build();
    }
}

