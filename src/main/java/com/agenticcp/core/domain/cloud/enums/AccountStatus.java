package com.agenticcp.core.domain.cloud.enums;

/**
 * 클라우드 계정 상태를 나타내는 Enum
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
public enum AccountStatus {
    /**
     * 활성 상태
     */
    ACTIVE,
    
    /**
     * 비활성 상태
     */
    INACTIVE,
    
    /**
     * 검증 중
     */
    VERIFYING,
    
    /**
     * 검증 완료
     */
    VERIFIED,
    
    /**
     * 검증 실패
     */
    FAILED,
    
    /**
     * 일시 중지
     */
    SUSPENDED
}

