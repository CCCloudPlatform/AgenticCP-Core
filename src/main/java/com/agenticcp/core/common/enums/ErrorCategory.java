package com.agenticcp.core.common.enums;

import com.agenticcp.core.common.dto.exception.BaseErrorCode;

/**
 * API 에러 코드의 카테고리를 정의하는 클래스입니다.
 *
 * @see BaseErrorCode
 * @see CommonErrorCode
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-09-22
 */
public enum ErrorCategory {
    
    COMMON("COMMON_"),      // 0000-0999 (HTTP 상태 코드 그대로)
    AUTH("AUTH_"),          // 1000-1999
    USER("USER_"),          // 2000-2999
    TENANT("TENANT_"),      // 3000-3999
    CLOUD("CLOUD_"),        // 4000-4999
    SECURITY("SECURITY_"),  // 5000-5999
    PLATFORM("PLATFORM_"),  // 6000-6999
    COST("COST_"),          // 7000-7999
    MONITORING("MONITORING_"), // 8000-8999
    INTEGRATION("INTEGRATION_"), // 9000-9999
    UI("UI_");              // 10000-10999

    private final String prefix;
    
    ErrorCategory(String prefix) {
        this.prefix = prefix;
    }
    
    public String generate(int codeNumber) {
        return this.prefix + codeNumber;
    }
}
