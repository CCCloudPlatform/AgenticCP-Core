package com.agenticcp.core.domain.cloud.dto;

import com.agenticcp.core.common.logging.masking.Masked;
import com.agenticcp.core.common.logging.masking.MaskingType;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 클라우드 계정 등록 요청 DTO
 * 새로운 클라우드 계정을 등록할 때 사용됩니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterCloudAccountRequest {
    
    /**
     * 프로바이더 타입
     */
    @NotNull(message = "프로바이더 타입은 필수입니다")
    private ProviderType providerType;
    
    /**
     * 계정 이름 (사용자 지정)
     */
    @NotBlank(message = "계정 이름은 필수입니다")
    @Size(min = 2, max = 100, message = "계정 이름은 2-100자 사이여야 합니다")
    private String accountName;
    
    /**
     * 계정 범위 (Account Scope)
     * AWS: Account ID, Azure: Subscription ID, GCP: Project ID
     * 검증 시 자동으로 채워질 수 있으므로 선택 사항
     */
    @Masked(type = MaskingType.ACCOUNT_SCOPE)
    private String accountScope;
    
    /**
     * Access Key
     * AWS: Access Key ID, Azure: Client ID, GCP: Service Account Key
     */
    @NotBlank(message = "Access Key는 필수입니다")
    @Masked(type = MaskingType.ACCESS_KEY)
    private String accessKey;
    
    /**
     * Secret Key
     * AWS: Secret Access Key, Azure: Client Secret, GCP: Service Account Secret
     */
    @NotBlank(message = "Secret Key는 필수입니다")
    @Masked(type = MaskingType.SECRET_KEY)
    private String secretKey;
    
    /**
     * 기본 리전
     */
    private String region;
    
    /**
     * 기본 계정 설정 여부
     */
    private Boolean isDefault;
    
    /**
     * 추가 메타데이터
     */
    private Map<String, String> metadata;
}

