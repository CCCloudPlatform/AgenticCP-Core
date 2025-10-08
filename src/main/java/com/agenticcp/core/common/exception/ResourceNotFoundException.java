package com.agenticcp.core.common.exception;

import com.agenticcp.core.common.dto.BaseErrorCode;
import com.agenticcp.core.common.enums.CommonErrorCode;

/**
 * 요청한 리소스를 시스템에서 찾을 수 없을 때 발생하는 예외입니다.
 * <p>
 * 이 예외는 일반적으로 서비스 계층에서 데이터베이스 조회 결과가 없을 때 사용되며,
 * {@code GlobalExceptionHandler}에 의해 HTTP 404 Not Found 상태 코드로 변환됩니다.
 * NPE(NullPointerException)를 발생시키는 대신 이 예외를 사용하여
 * "데이터가 없음"이라는 비즈니스 상황을 명확하게 표현하는 것을 권장합니다.
 * </p>
 *
 * <pre>
 * // [사용 예시 - Service Layer]
 * public User findById(Long id) {
 * return userRepository.findById(id)
 * .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
 * }
 * </pre>
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
