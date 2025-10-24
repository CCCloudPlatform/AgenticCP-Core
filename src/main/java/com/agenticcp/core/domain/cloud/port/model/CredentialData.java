package com.agenticcp.core.domain.cloud.port.model;

import com.agenticcp.core.domain.cloud.entity.CloudAccount.AuthMethod;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 클라우드 인증 정보 데이터 모델
 * 
 * CSP별로 다른 형식의 인증 정보를 포함합니다.
 * 실제 민감 정보는 암호화되어 저장됩니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-24
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CredentialData {
    
    /**
     * 프로바이더 타입
     */
    private ProviderType providerType;
    
    /**
     * 테넌트 키
     */
    private String tenantKey;
    
    /**
     * 계정 ID
     */
    private String accountId;
    
    /**
     * 인증 방식
     */
    private AuthMethod authMethod;
    
    /**
     * AWS IAM Role ARN
     */
    private String roleArn;
    
    /**
     * AWS External ID
     */
    private String externalId;
    
    /**
     * AWS Access Key ID
     */
    private String accessKeyId;
    
    /**
     * AWS Secret Access Key (암호화됨)
     */
    private String secretAccessKey;
    
    /**
     * GCP Service Account Email
     */
    private String serviceAccountEmail;
    
    /**
     * GCP Service Account JSON Key (암호화됨)
     */
    private String serviceAccountKey;
    
    /**
     * Azure Tenant ID
     */
    private String azureTenantId;
    
    /**
     * Azure Client ID (Service Principal)
     */
    private String azureClientId;
    
    /**
     * Azure Client Secret (암호화됨)
     */
    private String azureClientSecret;
    
    /**
     * 인증 정보 만료 시간 (임시 토큰의 경우)
     */
    private LocalDateTime expiresAt;
    
    /**
     * 인증 정보 생성 시간
     */
    private LocalDateTime createdAt;
    
    /**
     * 추가 인증 정보 (CSP별 확장)
     */
    private Map<String, String> additionalCredentials;
    
    /**
     * 인증 정보가 만료되었는지 확인
     */
    public boolean isExpired() {
        if (expiresAt == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(expiresAt);
    }
}

