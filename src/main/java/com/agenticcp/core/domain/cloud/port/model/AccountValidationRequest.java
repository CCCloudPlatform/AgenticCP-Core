package com.agenticcp.core.domain.cloud.port.model;

import com.agenticcp.core.domain.cloud.entity.CloudAccount.AuthMethod;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 클라우드 계정 검증 요청 모델
 * 
 * Port를 통해 외부 클라우드 API에 전달되는 검증 요청 정보입니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-24
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountValidationRequest {
    
    /**
     * 클라우드 프로바이더 타입
     */
    private ProviderType providerType;
    
    /**
     * 테넌트 키 (격리를 위한 식별자)
     */
    private String tenantKey;
    
    /**
     * 계정 ID
     * - AWS: AWS Account ID (12자리 숫자)
     * - GCP: GCP Project ID
     * - Azure: Azure Subscription ID (UUID)
     */
    private String accountId;
    
    /**
     * 기본 리전/존/로케이션
     */
    private String region;
    
    /**
     * 인증 방식
     */
    private AuthMethod authMethod;
    
    /**
     * AWS 전용 필드: IAM Role ARN
     */
    private String roleArn;
    
    /**
     * AWS 전용 필드: External ID
     */
    private String externalId;
    
    /**
     * GCP 전용 필드: Service Account Email
     */
    private String serviceAccountEmail;
    
    /**
     * Azure 전용 필드: Tenant ID
     */
    private String azureTenantId;
    
    /**
     * Azure 전용 필드: Client ID
     */
    private String azureClientId;
    
    /**
     * 추가 메타데이터 (CSP별 확장 가능)
     */
    private Map<String, Object> additionalMetadata;
}

