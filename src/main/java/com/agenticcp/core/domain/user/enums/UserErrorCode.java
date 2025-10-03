package com.agenticcp.core.domain.user.enums;

import com.agenticcp.core.common.dto.BaseErrorCode;
import com.agenticcp.core.common.enums.ErrorCategory;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 사용자 도메인 관련 에러 코드를 정의하는 Enum 클래스입니다.
 * <p>
 * 사용자 관리 관련 비즈니스 로직에서 발생하는 예외 상황들을 정의합니다.
 * 에러 코드 범위: 2000-2999
 * </p>
 *
 * @see BaseErrorCode
 * @see ErrorCategory
 * @author AgenticCP Team
 * @since 2025-09-22
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum UserErrorCode implements BaseErrorCode {

    // 사용자 조회 관련 (2001-2010)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, 2001, "사용자를 찾을 수 없습니다."),
    USER_ALREADY_DELETED(HttpStatus.BAD_REQUEST, 2002, "이미 삭제된 사용자입니다."),
    USER_ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, 2003, "계정이 잠겨있습니다."),
    USER_ACCOUNT_SUSPENDED(HttpStatus.FORBIDDEN, 2004, "계정이 일시정지되었습니다."),
    
    // 사용자 생성/수정 관련 (2011-2020)
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, 2011, "이미 사용 중인 사용자명입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, 2012, "이미 사용 중인 이메일입니다."),
    INVALID_USER_STATUS(HttpStatus.BAD_REQUEST, 2013, "유효하지 않은 사용자 상태입니다."),
    INVALID_PASSWORD_FORMAT(HttpStatus.BAD_REQUEST, 2014, "비밀번호 형식이 올바르지 않습니다."),
    
    // 사용자 인증 관련 (2021-2030) - 인증 관련은 AuthErrorCode 사용
    // INVALID_CREDENTIALS는 AuthErrorCode.INVALID_CREDENTIALS 사용
    // TOO_MANY_FAILED_ATTEMPTS는 AuthErrorCode.ACCOUNT_LOCKED 사용
    // PASSWORD_EXPIRED는 AuthErrorCode.INVALID_CREDENTIALS 사용
    
    // 사용자 권한 관련 (2031-2040)
    INSUFFICIENT_PERMISSIONS(HttpStatus.FORBIDDEN, 2031, "권한이 부족합니다."),
    ROLE_ASSIGNMENT_FAILED(HttpStatus.BAD_REQUEST, 2032, "역할 할당에 실패했습니다."),
    PERMISSION_DENIED(HttpStatus.FORBIDDEN, 2033, "접근 권한이 없습니다.");

    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;
    
    @Override
    public String getCode() {
        return ErrorCategory.USER.generate(codeNumber);
    }
}
