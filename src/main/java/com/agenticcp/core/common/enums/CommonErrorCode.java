package com.agenticcp.core.common.enums;

import com.agenticcp.core.common.dto.BaseErrorCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 애플리케이션 전역에서 사용되는 공통 에러 코드를 정의하는 Enum 클래스입니다.
 * <p>
 * 모든 도메인별 에러 코드는 반드시 {@link BaseErrorCode} 인터페이스를 구현해야 합니다.
 * </p>
 *
 * <pre>
 * {@literal @}Override
 * public String getCode() {
 * // ErrorCategory는 도메인에 맞게 변경 (예: USER)
 * return ErrorCategory.USER.generate(codeNumber);
 * }
 * }
 * </pre>
 *
 * @see BaseErrorCode
 * @see ErrorCategory
 * @author hyobinyang
 * @since 2025-09-22
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum CommonErrorCode implements BaseErrorCode {

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 001, "서버 내부 오류가 발생했습니다."),
    DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 002, "데이터베이스 오류가 발생했습니다."),

    BAD_REQUEST(HttpStatus.BAD_REQUEST, 400, "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, 401, "인증되지 않은 사용자입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, 403, "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, 404, "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, 405, "지원하지 않는 HTTP 메서드입니다."),

    FIELD_VALIDATION_ERROR(HttpStatus.BAD_REQUEST, 422, "필드 유효성 검증에 실패했습니다."),

    VALIDATION_CODE_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, 003, "정의되지 않은 유효성 검사 코드입니다."),
    
    // 컨텍스트 관련 (004-010)
    TENANT_CONTEXT_NOT_SET(HttpStatus.INTERNAL_SERVER_ERROR, 004, "테넌트 컨텍스트가 설정되지 않았습니다.");

    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;
    
    @Override
    public String getCode() {
        return ErrorCategory.COMMON.generate(codeNumber);
    }
}