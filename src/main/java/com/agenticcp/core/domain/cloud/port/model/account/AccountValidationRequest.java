package com.agenticcp.core.domain.cloud.port.model.account;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 클라우드 계정 검증 요청 DTO
 * 계정 검증을 요청할 때 필요한 정보를 담습니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountValidationRequest {
    
    /**
     * 프로바이더 타입
     */
    @NotNull(message = "프로바이더 타입은 필수입니다")
    private ProviderType providerType;
    
    /**
     * Access Key ID
     * AWS: Access Key ID, Azure: Client ID, GCP: Service Account Key
     */
    @NotBlank(message = "Access Key ID는 필수입니다")
    private String accessKeyId;
    
    /**
     * Secret Access Key
     * AWS: Secret Access Key, Azure: Client Secret, GCP: Service Account Secret
     */
    @NotBlank(message = "Secret Access Key는 필수입니다")
    private String secretAccessKey;
    
    /**
     * 리전 정보
     * 기본 리전 (선택 사항)
     */
    private String region;
}

