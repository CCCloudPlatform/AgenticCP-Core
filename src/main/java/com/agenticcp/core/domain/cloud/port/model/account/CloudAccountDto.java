package com.agenticcp.core.domain.cloud.port.model.account;

import com.agenticcp.core.common.logging.masking.Masked;
import com.agenticcp.core.common.logging.masking.MaskingType;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.enums.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 클라우드 계정 DTO
 * 클라우드 계정 정보를 전달할 때 사용됩니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloudAccountDto {
    
    /**
     * 계정 ID
     */
    private Long id;
    
    /**
     * 테넌트 ID
     */
    private Long tenantId;
    
    /**
     * 테넌트 키
     */
    @Masked(type = MaskingType.TENANT_KEY)
    private String tenantKey;
    
    /**
     * 프로바이더 ID
     */
    private Long providerId;
    
    /**
     * 프로바이더 타입
     */
    private ProviderType providerType;
    
    /**
     * 프로바이더 이름
     */
    private String providerName;
    
    /**
     * 계정 이름 (사용자 지정)
     */
    private String accountName;
    
    /**
     * 계정 범위 (Account Scope)
     * AWS: Account ID, Azure: Subscription ID, GCP: Project ID
     */
    @Masked(type = MaskingType.ACCOUNT_SCOPE)
    private String accountScope;
    
    /**
     * 계정 상태
     */
    private AccountStatus accountStatus;
    
    /**
     * 기본 계정 여부
     */
    private Boolean isDefault;
    
    /**
     * 기본 리전
     */
    private String region;
    
    /**
     * 검증 완료 시간
     */
    private LocalDateTime verifiedAt;
    
    /**
     * 마지막 동기화 시간
     */
    private LocalDateTime lastSyncAt;
    
    /**
     * 생성 시간
     */
    private LocalDateTime createdAt;
    
    /**
     * 수정 시간
     */
    private LocalDateTime updatedAt;
    
    /**
     * 생성자
     */
    private String createdBy;
    
    /**
     * 수정자
     */
    private String updatedBy;
}

