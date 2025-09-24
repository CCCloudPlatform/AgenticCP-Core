package com.agenticcp.core.common.enums;

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
    
    COMMON("COMMON_"),
    AUTH("AUTH_"),
    USER("USER_"),
    TENANT("TENANT_");

    private final String prefix;
    
    ErrorCategory(String prefix) {
        this.prefix = prefix;
    }
    
    public String generate(int codeNumber) {
        return this.prefix + codeNumber;
    }
}
