package com.agenticcp.core.domain.platform.enums;

import com.agenticcp.core.common.dto.exception.BaseErrorCode;
import com.agenticcp.core.common.enums.ErrorCategory;
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
    /**
     * 설정 키가 제공되지 않았을 때 발생하는 에러
     */
    CONFIG_KEY_REQUIRED(HttpStatus.BAD_REQUEST, 6001, "설정 키는 필수입니다."),
    
    /**
     * 설정 키 형식이 올바르지 않을 때 발생하는 에러
     * <p>
     * 설정 키는 영문자로 시작하고 영문자, 숫자, 점(.), 언더스코어(_), 하이픈(-)만 포함할 수 있습니다.
     * </p>
     */
    CONFIG_KEY_INVALID_FORMAT(HttpStatus.BAD_REQUEST, 6002, "설정 키 형식이 올바르지 않습니다."),
    
    /**
     * 설정 키가 최대 길이(255자)를 초과했을 때 발생하는 에러
     */
    CONFIG_KEY_TOO_LONG(HttpStatus.BAD_REQUEST, 6003, "설정 키는 255자를 초과할 수 없습니다."),
    
    /**
     * 설정 키가 최소 길이(3자) 미만일 때 발생하는 에러
     */
    CONFIG_KEY_TOO_SHORT(HttpStatus.BAD_REQUEST, 6004, "설정 키는 최소 3자 이상이어야 합니다."),
    
    /**
     * 설정 값이 제공되지 않았을 때 발생하는 에러
     */
    CONFIG_VALUE_REQUIRED(HttpStatus.BAD_REQUEST, 6005, "설정 값은 필수입니다."),
    
    /**
     * STRING 타입 설정 값이 비어있을 때 발생하는 에러
     */
    STRING_VALUE_EMPTY(HttpStatus.BAD_REQUEST, 6006, "문자열 값은 비어있을 수 없습니다."),
    
    /**
     * NUMBER 타입 설정 값이 유효한 숫자 형식이 아닐 때 발생하는 에러
     * <p>
     * 정수(BigInteger) 또는 실수(BigDecimal) 형식이어야 합니다.
     * </p>
     */
    NUMBER_VALUE_INVALID_FORMAT(HttpStatus.BAD_REQUEST, 6007, "유효한 숫자 형식이 아닙니다."),
    
    /**
     * BOOLEAN 타입 설정 값이 'true' 또는 'false'가 아닐 때 발생하는 에러
     */
    BOOLEAN_VALUE_INVALID(HttpStatus.BAD_REQUEST, 6008, "불린 값은 'true' 또는 'false'만 허용됩니다."),
    
    /**
     * JSON 타입 설정 값이 유효한 JSON 형식이 아닐 때 발생하는 에러
     */
    JSON_VALUE_INVALID_FORMAT(HttpStatus.BAD_REQUEST, 6009, "유효한 JSON 형식이 아닙니다."),
    
    /**
     * ENCRYPTED 타입 설정 값이 비어있을 때 발생하는 에러
     */
    ENCRYPTED_VALUE_EMPTY(HttpStatus.BAD_REQUEST, 6010, "암호화된 값은 비어있을 수 없습니다."),
    
    // 설정 관리 관련 에러
    /**
     * 요청한 설정을 찾을 수 없을 때 발생하는 에러
     */
    CONFIG_NOT_FOUND(HttpStatus.NOT_FOUND, 6011, "요청한 설정을 찾을 수 없습니다."),
    
    /**
     * 이미 존재하는 설정 키로 생성하려고 할 때 발생하는 에러
     */
    CONFIG_ALREADY_EXISTS(HttpStatus.CONFLICT, 6012, "이미 존재하는 설정 키입니다."),
    
    /**
     * 시스템 설정을 삭제하려고 할 때 발생하는 에러
     * <p>
     * 시스템 설정(isSystem=true)은 삭제할 수 없습니다.
     * </p>
     */
    SYSTEM_CONFIG_CANNOT_DELETE(HttpStatus.FORBIDDEN, 6013, "시스템 설정은 삭제할 수 없습니다."),
    
    /**
     * 시스템 설정을 수정하려고 할 때 발생하는 에러
     * <p>
     * 시스템 설정은 수정할 수 없습니다.
     * </p>
     */
    SYSTEM_CONFIG_CANNOT_MODIFY(HttpStatus.FORBIDDEN, 6014, "시스템 설정은 수정할 수 없습니다."),
    
    // 검증 서비스 관련 에러
    /**
     * 설정 검증 과정에서 내부 오류가 발생했을 때 발생하는 에러
     */
    VALIDATION_SERVICE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 6015, "설정 검증 중 오류가 발생했습니다."),

    // 암복호화/보안 관련 에러 (PLATFORM 도메인 6000-6999 범위)
    /**
     * 설정 값 암호화에 실패했을 때 발생하는 에러
     */
    ENCRYPTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 6016, "설정 암호화에 실패했습니다."),
    
    /**
     * 설정 값 복호화에 실패했을 때 발생하는 에러
     */
    DECRYPTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 6017, "설정 복호화에 실패했습니다."),
    
    /**
     * 암호화 키가 구성되지 않았을 때 발생하는 에러
     */
    ENCRYPTION_KEY_MISSING(HttpStatus.INTERNAL_SERVER_ERROR, 6018, "암호화 키가 구성되지 않았습니다."),
    
    /**
     * 암호문 형식이 올바르지 않을 때 발생하는 에러
     * <p>
     * 복호화를 시도했지만 암호문 형식이 유효하지 않은 경우 발생합니다.
     * </p>
     */
    ENCRYPTED_PAYLOAD_INVALID(HttpStatus.BAD_REQUEST, 6019, "암호문 형식이 올바르지 않습니다."),
    
    // 시스템 설정 보호 관련 에러
    /**
     * 시스템 설정의 타입을 변경하려고 할 때 발생하는 에러
     * <p>
     * 시스템 설정(isSystem=true)의 타입은 변경할 수 없습니다.
     * </p>
     */
    SYSTEM_CONFIG_TYPE_CHANGE_FORBIDDEN(HttpStatus.FORBIDDEN, 6020, "시스템 설정의 타입은 변경할 수 없습니다."),
    
    /**
     * 시스템 설정이 'system.' 네임스페이스를 사용하지 않을 때 발생하는 에러
     * <p>
     * isSystem=true인 설정은 반드시 'system.'으로 시작하는 키를 사용해야 합니다.
     * </p>
     */
    SYSTEM_CONFIG_NAMESPACE_MISMATCH(HttpStatus.BAD_REQUEST, 6021, "시스템 설정은 'system.' 네임스페이스를 사용해야 합니다."),
    
    /**
     * 사용자 설정이 'system.' 네임스페이스를 사용할 때 발생하는 에러
     * <p>
     * isSystem=false인 설정은 'system.'으로 시작하는 키를 사용할 수 없습니다.
     * </p>
     */
    USER_CONFIG_NAMESPACE_MISMATCH(HttpStatus.BAD_REQUEST, 6022, "사용자 설정은 'system.' 네임스페이스를 사용할 수 없습니다."),
    
    /**
     * 설정 키가 유효한 네임스페이스를 사용하지 않을 때 발생하는 에러
     * <p>
     * 설정 키는 반드시 'system.' 또는 'user.'로 시작해야 합니다.
     * </p>
     */
    INVALID_CONFIG_NAMESPACE(HttpStatus.BAD_REQUEST, 6023, "설정 키는 'system.' 또는 'user.' 네임스페이스를 사용해야 합니다."),
    
    /**
     * 유효하지 않은 테넌트 키일 때 발생하는 에러
     * <p>
     * 테넌트가 DB에 존재하지 않거나 유효하지 않은 경우 발생합니다.
     * </p>
     */
    INVALID_TENANT_KEY(HttpStatus.BAD_REQUEST, 6024, "유효하지 않은 테넌트 키입니다. 테넌트가 존재하지 않습니다.");

    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;
    
    @Override
    public String getCode() {
        return ErrorCategory.PLATFORM.generate(codeNumber);
    }
}
