package com.agenticcp.core.domain.user.enums;

/**
 * 인증 종류 Enum
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-20
 */
public enum AuthType {
    /**
     * 회원가입
     */
    REGISTER("회원가입"),
    
    /**
     * 로그인
     */
    LOGIN("로그인"),
    
    /**
     * 로그아웃
     */
    LOGOUT("로그아웃"),
    
    /**
     * 토큰 갱신
     */
    TOKEN_REFRESH("토큰 갱신"),
    
    /**
     * 2FA 설정 (QR 코드 생성)
     */
    TWO_FACTOR_SETUP("2FA 설정"),
    
    /**
     * 2FA 활성화
     */
    TWO_FACTOR_ENABLE("2FA 활성화"),
    
    /**
     * 2FA 검증 (로그인 시)
     */
    TWO_FACTOR_VERIFY("2FA 검증"),
    
    /**
     * 2FA 비활성화
     */
    TWO_FACTOR_DISABLE("2FA 비활성화"),
    
    /**
     * 비밀번호 변경
     */
    PASSWORD_CHANGE("비밀번호 변경"),
    
    /**
     * 비밀번호 재설정
     */
    PASSWORD_RESET("비밀번호 재설정"),
    
    /**
     * 이메일 인증
     */
    EMAIL_VERIFY("이메일 인증"),
    
    /**
     * 계정 잠금
     */
    ACCOUNT_LOCK("계정 잠금"),
    
    /**
     * 계정 잠금 해제
     */
    ACCOUNT_UNLOCK("계정 잠금 해제");
    
    private final String description;
    
    AuthType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}

