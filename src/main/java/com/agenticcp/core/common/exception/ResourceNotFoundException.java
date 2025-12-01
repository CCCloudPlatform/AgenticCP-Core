package com.agenticcp.core.common.exception;

import com.agenticcp.core.common.dto.exception.BaseErrorCode;
import com.agenticcp.core.common.enums.CommonErrorCode;

/**
 * 요청한 리소스를 찾을 수 없을 때 발생하는 비즈니스 예외입니다.
 * {@code GlobalExceptionHandler}에 의해 HTTP 404(NOT_FOUND)로 변환되며,
 * 조회 조건을 메시지에 포함해 디버깅을 돕습니다.
 *
 * @see BusinessException
 * @see GlobalExceptionHandler
 * @author AgenticCP Team
 * @since 2025-09-22
 * @version 1.0.0
 */
public class ResourceNotFoundException extends BusinessException {

    /**
     * 공통 NOT_FOUND 코드로 예외를 던질 때 사용합니다.
     */
    public ResourceNotFoundException() {
        super(CommonErrorCode.NOT_FOUND);
    }

    /**
     * 도메인 전용 {@link BaseErrorCode}가 있는 경우 해당 코드를 그대로 전달합니다.
     *
     * @param errorCode 리소스 조회 실패를 나타내는 에러 코드
     */
    public ResourceNotFoundException(BaseErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 리소스 이름과 조회 필드, 값 정보를 메시지에 포함합니다.
     *
     * @param resource 리소스 종류 (예: User)
     * @param field    조회에 사용한 필드명
     * @param value    조회 값
     */
    public ResourceNotFoundException(String resource, String field, Object value) {
        super(CommonErrorCode.NOT_FOUND,
                String.format("%s 리소스를 찾을 수 없습니다. (%s: %s)", resource, field, value));
    }
}
