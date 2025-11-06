package com.agenticcp.core.domain.cloud.adapter.outbound.aws;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.port.model.AwsSessionCredential;
import com.agenticcp.core.domain.cloud.service.AwsCredentialManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetSessionTokenRequest;
import software.amazon.awssdk.services.sts.model.GetSessionTokenResponse;

import java.time.LocalDateTime;

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
    
    /**
     * AWS STS 세션을 발급합니다.
     * 
     * @param credentialKey 자격증명 키
     * @param durationSeconds 세션 지속 시간 (초, 최대 43200초 = 12시간)
     * @return AwsSessionCredential
     */
    public AwsSessionCredential getSession(String credentialKey, int durationSeconds) {
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
                
                // 세션 만료 시간 계산 (Instant를 LocalDateTime으로 변환)
                LocalDateTime expiresAt = java.time.LocalDateTime.ofInstant(
                        response.credentials().expiration(),
                        java.time.ZoneId.systemDefault()
                );
                
                // 세션 자격증명 생성
                AwsSessionCredential session = AwsSessionCredential.builder()
                        .accessKeyId(response.credentials().accessKeyId())
                        .secretAccessKey(response.credentials().secretAccessKey())
                        .sessionToken(response.credentials().sessionToken())
                        .region(longTermCredentials.getRegion())
                        .expiresAt(expiresAt)
                        .build();
                
                log.info("[AwsSessionProvider] getSession - success, expiresAt={}", expiresAt);
                return session;
                
            } finally {
                stsClient.close();
            }
            
        } catch (Exception e) {
            log.error("[AwsSessionProvider] getSession - failed", e);
            throw new BusinessException(
                CloudErrorCode.SESSION_ISSUANCE_FAILED,
                "AWS 세션 발급에 실패했습니다: " + e.getMessage()
            );
        }
    }
}

