package com.agenticcp.core.domain.tenant.enums;

import com.agenticcp.core.common.dto.exception.BaseErrorCode;
import com.agenticcp.core.common.enums.ErrorCategory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 테넌트 설정 관련 에러 코드
 */
@Getter
@RequiredArgsConstructor
public enum TenantConfigErrorCode implements BaseErrorCode {

    // 3000-3999: 테넌트 도메인
    TENANT_CONFIG_NOT_FOUND(HttpStatus.NOT_FOUND, 3001, "테넌트 설정을 찾을 수 없습니다."),
    TENANT_TYPE_CONFIG_NOT_FOUND(HttpStatus.NOT_FOUND, 3002, "테넌트 타입별 설정을 찾을 수 없습니다."),
    INVALID_CONFIG_VALUE(HttpStatus.BAD_REQUEST, 3003, "유효하지 않은 설정값입니다."),
    CONFIG_KEY_ALREADY_EXISTS(HttpStatus.CONFLICT, 3004, "이미 존재하는 설정 키입니다."),
    INVALID_CONFIG_TYPE(HttpStatus.BAD_REQUEST, 3005, "유효하지 않은 설정 타입입니다."),
    CONFIG_ENCRYPTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 3006, "설정 암호화에 실패했습니다."),
    CONFIG_DECRYPTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 3007, "설정 복호화에 실패했습니다."),
    TENANT_CONFIG_QUOTA_EXCEEDED(HttpStatus.BAD_REQUEST, 3008, "테넌트 설정 할당량을 초과했습니다."),
    CONFIG_INHERITANCE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 3009, "설정 상속 처리에 실패했습니다."),
    CACHE_EVICTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 3010, "캐시 무효화에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;

    @Override
    public String getCode() {
        return ErrorCategory.TENANT.generate(codeNumber);
    }
}
