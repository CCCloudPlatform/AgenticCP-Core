package com.agenticcp.core.domain.platform.exception;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.platform.enums.PlatformConfigErrorCode;

/**
 * 플랫폼 설정 검증 과정에서 발생하는 예외입니다.
 * <p>
 * 설정 키, 값, 타입 등의 검증 실패 시 발생하며, 
 * 클라이언트에게 구체적인 검증 오류 정보를 제공합니다.
 * </p>
 *
 * @author AgenticCP Team
 * @since 2025-09-26
 */
public class ConfigValidationException extends BusinessException {

    /**
     * 에러 코드만으로 예외를 생성합니다.
     * <p>
     * 에러 코드에 정의된 기본 메시지를 사용합니다.
     * </p>
     *
     * @param errorCode 플랫폼 설정 관련 에러 코드
     */
    public ConfigValidationException(PlatformConfigErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 에러 코드와 커스텀 메시지로 예외를 생성합니다.
     * <p>
     * 에러 코드의 기본 메시지 대신 커스텀 메시지를 사용합니다.
     * </p>
     *
     * @param errorCode 플랫폼 설정 관련 에러 코드
     * @param customMessage 커스텀 에러 메시지
     */
    public ConfigValidationException(PlatformConfigErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }
}
