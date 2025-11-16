package com.agenticcp.core.common.exception;

import com.agenticcp.core.common.dto.exception.BaseErrorCode;
import com.agenticcp.core.common.enums.CommonErrorCode;

/**
 * 요청한 리소스를 찾을 수 없을 때 발생하는 비즈니스 예외입니다.
 * {@code GlobalExceptionHandler}에 의해 HTTP 404(NOT_FOUND)로 변환됩니다.
 * 
 * @see BusinessException
 * @see GlobalExceptionHandler
 * @author AgenticCP Team
 * @since 2025-09-22
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException() {
        super(CommonErrorCode.NOT_FOUND);
    }

    public ResourceNotFoundException(BaseErrorCode errorCode) {
        super(errorCode);
    }

    public ResourceNotFoundException(String resource, String field, Object value) {
        super(CommonErrorCode.NOT_FOUND,
                String.format("%s 리소스를 찾을 수 없습니다. (%s: %s)", resource, field, value));
    }
}
