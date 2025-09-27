package com.agenticcp.core.common.enums;

import com.agenticcp.core.common.dto.BaseErrorCode;
import com.agenticcp.core.common.exception.ErrorCategory;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 인증(Auth) 도메인 에러 코드: 1000-1999
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum AuthErrorCode implements BaseErrorCode {

    // 1xxx: 인증/인가
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, 1001, "잘못된 사용자명 또는 비밀번호입니다"),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, 1002, "계정이 비활성화되었습니다"),
    ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, 1003, "계정이 잠금되었습니다"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, 1004, "인증 토큰이 만료되었습니다"),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, 1005, "유효하지 않은 인증 토큰입니다"),
    TOKEN_BLACKLISTED(HttpStatus.UNAUTHORIZED, 1006, "블랙리스트에 등록된 토큰입니다"),
    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, 1007, "유효하지 않은 리프레시 토큰입니다"),
    PERMISSION_DENIED(HttpStatus.FORBIDDEN, 1008, "권한이 없습니다"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, 1009, "인증이 필요합니다"),

    // 2FA: 101x
    TWO_FACTOR_REQUIRED(HttpStatus.UNAUTHORIZED, 1010, "2FA 코드가 필요합니다"),
    TWO_FACTOR_INVALID(HttpStatus.UNAUTHORIZED, 1011, "잘못된 2FA 코드입니다"),
    TWO_FACTOR_NOT_ENABLED(HttpStatus.BAD_REQUEST, 1012, "2FA가 활성화되지 않았습니다"),
    TWO_FACTOR_ALREADY_ENABLED(HttpStatus.BAD_REQUEST, 1013, "2FA가 이미 활성화되어 있습니다"),
    TWO_FACTOR_ATTEMPTS_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, 1014, "2FA 검증 시도 횟수를 초과했습니다"),
    TWO_FACTOR_SESSION_EXPIRED(HttpStatus.UNAUTHORIZED, 1015, "2FA 설정 세션이 만료되었습니다"),

    // 사용자 관련: 102x
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, 1020, "사용자를 찾을 수 없습니다"),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, 1021, "사용자가 이미 존재합니다"),
    USER_INVALID_STATUS(HttpStatus.BAD_REQUEST, 1022, "유효하지 않은 사용자 상태입니다");

    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;

    @Override
    public String getCode() {
        return ErrorCategory.AUTH.generate(codeNumber);
    }
}
