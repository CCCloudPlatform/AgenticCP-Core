package com.agenticcp.core.common.exception;

import com.agenticcp.core.common.dto.exception.BaseErrorCode;
import lombok.Getter;

/**
 * 애플리케이션의 비즈니스 규칙 위반을 표현하는 최상위 예외입니다.
 * 하위 도메인 예외들은 이 클래스를 상속합니다.
 * 
 * @see BaseErrorCode
 * @see GlobalExceptionHandler
 * @author AgenticCP Team
 * @since 2025.09.22
 */
@Getter
public class BusinessException extends RuntimeException {

    private final BaseErrorCode errorCode;

    public BusinessException(BaseErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(BaseErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }
}
