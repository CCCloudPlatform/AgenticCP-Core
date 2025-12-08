package com.agenticcp.core.common.exception;


import com.agenticcp.core.common.enums.CommonErrorCode;

/**
 * 인증은 되었으나 요청 리소스에 대한 접근 권한이 없을 때 발생하는 비즈니스 예외입니다.
 * {@code GlobalExceptionHandler}에 의해 HTTP 403(FORBIDDEN)으로 변환되며,
 * 호출 컨텍스트에 따라 사용자·리소스 정보를 포함한 메시지를 제공합니다.
 *
 * @see BusinessException
 * @see GlobalExceptionHandler
 * @author AgenticCP Team
 * @since 2025-09-22
 * @version 1.0.0
 */
public class AuthorizationException extends BusinessException {

    /**
     * 권한 부족을 일반화하여 전달할 때 사용합니다.
     */
    public AuthorizationException() {
        super(CommonErrorCode.FORBIDDEN);
    }

    /**
     * 특정 리소스에 대한 행위 권한이 부족한 경우의 예외입니다.
     *
     * @param userId   권한을 검사한 사용자 ID
     * @param resource 접근하려는 리소스 명
     * @param action   수행하려던 행위
     */
    public AuthorizationException(Long userId, String resource, String action) {
        super(CommonErrorCode.FORBIDDEN,
                String.format("User(ID: %d): '%s' 리소스에 대한 '%s' 권한이 없습니다.",
                        userId, resource, action));
    }

    /**
     * 특정 역할이 필요한 시나리오에서 부족한 역할 정보를 함께 전달합니다.
     *
     * @param userId       권한을 검사한 사용자 ID
     * @param requiredRole 요구되는 역할 이름
     */
    public AuthorizationException(Long userId, String requiredRole) {
        super(CommonErrorCode.FORBIDDEN,
                String.format("User(ID: %d): 할당된 '%s' 역할이 없습니다.",
                        userId, requiredRole));
    }

    /**
     * 사용자 ID만으로 권한 부족을 알릴 때 사용합니다.
     *
     * @param userId 권한을 검사한 사용자 ID
     */
    public AuthorizationException(Long userId) {
        super(CommonErrorCode.FORBIDDEN,
                String.format("User(ID: %d): 접근 권한이 없습니다.", userId));
    }
}
