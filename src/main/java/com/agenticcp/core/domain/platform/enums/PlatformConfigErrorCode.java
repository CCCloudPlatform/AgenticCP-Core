package com.agenticcp.core.domain.platform.enums;

import com.agenticcp.core.common.dto.BaseErrorCode;
import com.agenticcp.core.common.exception.ErrorCategory;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 플랫폼 설정 관련 에러 코드를 정의하는 Enum 클래스입니다.
 * <p>
 * 플랫폼 설정 검증, 생성, 수정, 삭제 과정에서 발생할 수 있는 
 * 비즈니스 예외 상황에 대한 에러 코드를 제공합니다.
 * </p>
 *
 * @author AgenticCP Team
 * @since 2025-09-26
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum PlatformConfigErrorCode implements BaseErrorCode {

    // 설정 검증 관련 에러
    CONFIG_KEY_REQUIRED(HttpStatus.BAD_REQUEST, 6001, "설정 키는 필수입니다."),
    CONFIG_KEY_INVALID_FORMAT(HttpStatus.BAD_REQUEST, 6002, "설정 키 형식이 올바르지 않습니다."),
    CONFIG_KEY_TOO_LONG(HttpStatus.BAD_REQUEST, 6003, "설정 키는 255자를 초과할 수 없습니다."),
    CONFIG_KEY_TOO_SHORT(HttpStatus.BAD_REQUEST, 6004, "설정 키는 최소 3자 이상이어야 합니다."),
    
    CONFIG_VALUE_REQUIRED(HttpStatus.BAD_REQUEST, 6005, "설정 값은 필수입니다."),
    STRING_VALUE_EMPTY(HttpStatus.BAD_REQUEST, 6006, "문자열 값은 비어있을 수 없습니다."),
    NUMBER_VALUE_INVALID_FORMAT(HttpStatus.BAD_REQUEST, 6007, "유효한 숫자 형식이 아닙니다."),
    BOOLEAN_VALUE_INVALID(HttpStatus.BAD_REQUEST, 6008, "불린 값은 'true' 또는 'false'만 허용됩니다."),
    JSON_VALUE_INVALID_FORMAT(HttpStatus.BAD_REQUEST, 6009, "유효한 JSON 형식이 아닙니다."),
    ENCRYPTED_VALUE_EMPTY(HttpStatus.BAD_REQUEST, 6010, "암호화된 값은 비어있을 수 없습니다."),
    
    // 설정 관리 관련 에러
    CONFIG_NOT_FOUND(HttpStatus.NOT_FOUND, 6011, "요청한 설정을 찾을 수 없습니다."),
    CONFIG_ALREADY_EXISTS(HttpStatus.CONFLICT, 6012, "이미 존재하는 설정 키입니다."),
    SYSTEM_CONFIG_CANNOT_DELETE(HttpStatus.FORBIDDEN, 6013, "시스템 설정은 삭제할 수 없습니다."),
    SYSTEM_CONFIG_CANNOT_MODIFY(HttpStatus.FORBIDDEN, 6014, "시스템 설정은 수정할 수 없습니다."),
    
    // 검증 서비스 관련 에러
    VALIDATION_SERVICE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 6015, "설정 검증 중 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;
    
    @Override
    public String getCode() {
        return ErrorCategory.PLATFORM.generate(codeNumber);
    }
}
