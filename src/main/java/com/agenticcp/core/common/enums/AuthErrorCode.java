package com.agenticcp.core.common.enums;

import lombok.Getter;

/**
 * 인증 관련 에러 코드
 * 에러 코드 규칙: [도메인]_[타입]_[상세]
 */
@Getter
public enum AuthErrorCode {
    
    // 인증 관련 에러
    AUTH_LOGIN_FAILED("AUTH_LOGIN_FAILED", "잘못된 사용자명 또는 비밀번호입니다"),
    AUTH_ACCOUNT_DISABLED("AUTH_ACCOUNT_DISABLED", "계정이 비활성화되었습니다"),
    AUTH_ACCOUNT_LOCKED("AUTH_ACCOUNT_LOCKED", "계정이 잠금되었습니다"),
    AUTH_TOKEN_EXPIRED("AUTH_TOKEN_EXPIRED", "인증 토큰이 만료되었습니다"),
    AUTH_TOKEN_INVALID("AUTH_TOKEN_INVALID", "유효하지 않은 인증 토큰입니다"),
    AUTH_TOKEN_BLACKLISTED("AUTH_TOKEN_BLACKLISTED", "블랙리스트에 등록된 토큰입니다"),
    AUTH_REFRESH_TOKEN_INVALID("AUTH_REFRESH_TOKEN_INVALID", "유효하지 않은 리프레시 토큰입니다"),
    AUTH_PERMISSION_DENIED("AUTH_PERMISSION_DENIED", "권한이 없습니다"),
    AUTH_UNAUTHORIZED("AUTH_UNAUTHORIZED", "인증이 필요합니다"),
    
    // 2FA 관련 에러
    AUTH_2FA_REQUIRED("AUTH_2FA_REQUIRED", "2FA 코드가 필요합니다"),
    AUTH_2FA_INVALID("AUTH_2FA_INVALID", "잘못된 2FA 코드입니다"),
    AUTH_2FA_NOT_ENABLED("AUTH_2FA_NOT_ENABLED", "2FA가 활성화되지 않았습니다"),
    AUTH_2FA_ALREADY_ENABLED("AUTH_2FA_ALREADY_ENABLED", "2FA가 이미 활성화되어 있습니다"),
    AUTH_2FA_ATTEMPTS_EXCEEDED("AUTH_2FA_ATTEMPTS_EXCEEDED", "2FA 검증 시도 횟수를 초과했습니다"),
    AUTH_2FA_SESSION_EXPIRED("AUTH_2FA_SESSION_EXPIRED", "2FA 설정 세션이 만료되었습니다"),
    
    // 사용자 관련 에러
    USER_NOT_FOUND("USER_NOT_FOUND", "사용자를 찾을 수 없습니다"),
    USER_ALREADY_EXISTS("USER_ALREADY_EXISTS", "사용자가 이미 존재합니다"),
    USER_INVALID_STATUS("USER_INVALID_STATUS", "유효하지 않은 사용자 상태입니다"),
    
    // 검증 관련 에러
    VALIDATION_REQUIRED_FIELD("VALIDATION_REQUIRED_FIELD", "필수 필드가 누락되었습니다"),
    VALIDATION_INVALID_FORMAT("VALIDATION_INVALID_FORMAT", "잘못된 형식입니다"),
    VALIDATION_OUT_OF_RANGE("VALIDATION_OUT_OF_RANGE", "범위를 벗어났습니다"),
    VALIDATION_ERROR("VALIDATION_ERROR", "입력 데이터 검증 실패"),
    
    // 시스템 관련 에러
    SYSTEM_INTERNAL_ERROR("SYSTEM_INTERNAL_ERROR", "시스템 내부 오류가 발생했습니다"),
    SYSTEM_SERVICE_UNAVAILABLE("SYSTEM_SERVICE_UNAVAILABLE", "서비스를 사용할 수 없습니다"),
    SYSTEM_REDIS_ERROR("SYSTEM_REDIS_ERROR", "Redis 서비스 오류가 발생했습니다");
    
    private final String code;
    private final String message;
    
    AuthErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
