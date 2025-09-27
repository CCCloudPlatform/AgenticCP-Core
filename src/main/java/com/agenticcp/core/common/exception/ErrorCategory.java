package com.agenticcp.core.common.exception;

import com.agenticcp.core.common.dto.BaseErrorCode;

/**
 * API 에러 코드의 카테고리를 정의하는 클래스입니다.
 * <p>
 * {@link BaseErrorCode}를 구현하는 각각의 에러 코드 Enum은 반드시 이곳에 정의된
 * 카테고리 중 하나와 매핑되어야 합니다. 새로운 도메인(예: Product)이 추가되고
 * {@code ProductErrorCode}가 생성되면, 이곳에도 {@code PRODUCT("PRODUCT_")}와 같이
 * 새로운 카테고리를 추가해야 하는 규칙을 가집니다.
 * </p>
 *
 * @see BaseErrorCode
 * @see CommonErrorCode
 * @author hyobinyang
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
    INTEGRATION("INTEGRATION_"); // 9000-9999

    private final String prefix;
    
    ErrorCategory(String prefix) {
        this.prefix = prefix;
    }
    
    public String generate(int codeNumber) {
        return this.prefix + codeNumber;
    }
}
