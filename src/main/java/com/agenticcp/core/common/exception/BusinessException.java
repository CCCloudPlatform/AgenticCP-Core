package com.agenticcp.core.common.exception;

import com.agenticcp.core.common.dto.exception.BaseErrorCode;
import lombok.Getter;

/**
 * 애플리케이션의 비즈니스 규칙 위반을 표현하는 최상위 예외입니다.
 * 모든 도메인 전용 예외는 이 클래스를 상속하여 {@link BaseErrorCode} 기반 코드를 제공합니다.
 *
 * @see BaseErrorCode
 * @see GlobalExceptionHandler
 * @author AgenticCP Team
 * @since 2025-09-22
 * @version 1.0.0
 */
@Getter
public class BusinessException extends RuntimeException {

    private final BaseErrorCode errorCode;

    /**
     * 표준 에러 코드에 정의된 메시지를 그대로 노출할 때 사용합니다.
     *
     * @param errorCode 비즈니스 에러 코드
     */
    public BusinessException(BaseErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 에러 코드와 별도의 커스텀 메시지를 제공해야 할 때 사용합니다.
     *
     * @param errorCode     비즈니스 에러 코드
     * @param customMessage 사용자 정의 메시지
     */
    public BusinessException(BaseErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }
}
