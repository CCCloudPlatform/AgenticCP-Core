package com.agenticcp.core.domain.cloud.adapter.outbound.aws.account;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.exception.CredentialErrorCode;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetSessionTokenRequest;
import software.amazon.awssdk.services.sts.model.GetSessionTokenResponse;


/**
 * AWS STS 세션 발급 어댑터
 * 
 * AWS STS GetSessionToken API를 사용하여 단기 세션을 발급합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AwsSessionProvider {
    
    private final AwsCredentialManager awsCredentialManager;
    private final AwsSessionCredentialMapper awsSessionCredentialMapper;
    
    /**
     * AWS STS 세션을 발급합니다.
     * 
     * @param credentialKey 자격증명 키
     * @param durationSeconds 세션 지속 시간 (초, 최대 43200초 = 12시간)
     * @return CloudSessionCredential
     */
    public CloudSessionCredential getSession(String credentialKey, int durationSeconds) {
        log.debug("[AwsSessionProvider] getSession - credentialKey={}, durationSeconds={}", 
                credentialKey, durationSeconds);
        
        try {
            // 장기 자격증명 조회
            AwsCredentialManager.AwsCredentials longTermCredentials = 
                    awsCredentialManager.getCredentials(credentialKey);
            
            // STS Client 생성
            StsClient stsClient = StsClient.builder()
                    .credentialsProvider(() -> 
                        software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create(
                            longTermCredentials.getAccessKeyId(),
                            longTermCredentials.getSecretAccessKey()
                        ))
                    .region(software.amazon.awssdk.regions.Region.of(
                        longTermCredentials.getRegion() != null ? longTermCredentials.getRegion() : "us-east-1"))
                    .build();
            
            try {
                // GetSessionToken 요청
                GetSessionTokenRequest request = GetSessionTokenRequest.builder()
                        .durationSeconds(Math.min(durationSeconds, 43200)) // 최대 12시간
                        .build();
                
                GetSessionTokenResponse response = stsClient.getSessionToken(request);
                
                CloudSessionCredential session = awsSessionCredentialMapper.toCloudSessionCredential(
                        response,
                        longTermCredentials.getRegion()
                );

                if (session == null) {
                    throw new BusinessException(
                            CredentialErrorCode.SESSION_ISSUANCE_FAILED,
                            "AWS 세션 매핑에 실패했습니다"
                    );
                }

                log.info("[AwsSessionProvider] getSession - success, expiresAt={}", session.getExpiresAt());
                return session;
                
            } finally {
                stsClient.close();
            }
            
        } catch (Exception e) {
            log.error("[AwsSessionProvider] getSession - failed", e);
            throw new BusinessException(
                CredentialErrorCode.SESSION_ISSUANCE_FAILED,
                "AWS 세션 발급에 실패했습니다: " + e.getMessage()
            );
        }
    }
}

