package com.agenticcp.core.domain.cloud.adapter.outbound.aws.config;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.sts.StsClient;

/**
 * AWS Client 생성을 위한 유틸리티 클래스
 * 동적 자격증명으로 AWS 클라이언트를 생성합니다.
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
}

