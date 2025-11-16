package com.agenticcp.core.common.exception;


import com.agenticcp.core.common.enums.CommonErrorCode;

/**
 * 인증은 되었으나 요청 리소스에 대한 접근 권한이 없을 때 발생하는 비즈니스 예외입니다.
 * {@code GlobalExceptionHandler}에 의해 HTTP 403(FORBIDDEN)으로 변환됩니다.
 * 
 * @see BusinessException
 * @see GlobalExceptionHandler
 * @author AgenticCP Team
 * @since 2025.09.22
 */
public class AuthorizationException extends BusinessException{

    public AuthorizationException() {
        super(CommonErrorCode.FORBIDDEN);
    }

    public AuthorizationException(Long userId, String resource, String action) {
        super(CommonErrorCode.FORBIDDEN,
                String.format("User(ID: %d): '%s' 리소스에 대한 '%s' 권한이 없습니다.",
                        userId, resource, action));
    }

    public AuthorizationException(Long userId, String requiredRole) {
        super(CommonErrorCode.FORBIDDEN,
                String.format("User(ID: %d): 할당된 '%s' 역할이 없습니다.",
                        userId, requiredRole));
    }

    public AuthorizationException(Long userId) {
        super(CommonErrorCode.FORBIDDEN,
                String.format("User(ID: %d): 접근 권한이 없습니다.", userId));
    }
}
