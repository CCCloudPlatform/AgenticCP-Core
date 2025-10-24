package com.agenticcp.core.domain.cloud.port.model;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 클라우드 계정 검증 결과 모델
 * 
 * 외부 클라우드 API 검증 후 반환되는 결과 정보입니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-24
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountValidationResult {
    
    /**
     * 검증 성공 여부
     */
    private boolean valid;
    
    /**
     * 계정 ID
     */
    private String accountId;
    
    /**
     * 프로바이더 타입
     */
    private ProviderType providerType;
    
    /**
     * 검증 메시지 (성공/실패 상세 정보)
     */
    private String message;
    
    /**
     * 에러 코드 (검증 실패 시)
     */
    private String errorCode;
    
    /**
     * 계정 메타데이터 (검증 성공 시)
     */
    private AccountMetadata accountMetadata;
    
    /**
     * 검증 수행 시간
     */
    private LocalDateTime validatedAt;
    
    /**
     * 추가 정보 (CSP별 확장 가능)
     */
    private Map<String, Object> additionalInfo;
    
    /**
     * 검증 성공 결과 생성 팩토리 메서드
     */
    public static AccountValidationResult success(String accountId, ProviderType providerType, AccountMetadata metadata) {
        return AccountValidationResult.builder()
                .valid(true)
                .accountId(accountId)
                .providerType(providerType)
                .message("계정 검증 성공")
                .accountMetadata(metadata)
                .validatedAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * 검증 실패 결과 생성 팩토리 메서드
     */
    public static AccountValidationResult failure(String accountId, ProviderType providerType, String errorCode, String message) {
        return AccountValidationResult.builder()
                .valid(false)
                .accountId(accountId)
                .providerType(providerType)
                .errorCode(errorCode)
                .message(message)
                .validatedAt(LocalDateTime.now())
                .build();
    }
}

