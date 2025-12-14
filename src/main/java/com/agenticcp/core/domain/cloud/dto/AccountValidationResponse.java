package com.agenticcp.core.domain.cloud.dto;

import com.agenticcp.core.common.logging.masking.Masked;
import com.agenticcp.core.common.logging.masking.MaskingType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 클라우드 계정 검증 결과 DTO
 * 계정 검증 후 반환되는 정보를 담습니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountValidationResponse {
    
    /**
     * 검증 성공 여부
     */
    private Boolean valid;
    
    /**
     * 검증 결과 메시지
     */
    private String message;
    
    /**
     * 계정 범위 (Account Scope)
     * AWS: Account ID, Azure: Subscription ID, GCP: Project ID
     */
    @Masked(type = MaskingType.ACCOUNT_SCOPE)
    private String accountScope;
    
    /**
     * 기본 리전 정보
     */
    private String region;
    
    /**
     * 추가 메타데이터
     * Arn, UserId 등의 추가 정보
     */
    private Map<String, Object> metadata;
}

