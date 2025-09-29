package com.agenticcp.core.common.security;

/**
 * JWT 관련 상수 정의
 */
public final class JwtConstants {
    
    private JwtConstants() {
        // 유틸리티 클래스는 인스턴스화 방지
    }
    
    // JWT 클레임 키
    public static final String CLAIM_USERNAME = "username";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_TENANT_ID = "tenantId";
    public static final String CLAIM_TENANT_KEY = "tenantKey";
    public static final String CLAIM_PERMISSIONS = "permissions";
    public static final String CLAIM_TYPE = "type";
    
    // JWT 토큰 타입
    public static final String TOKEN_TYPE_REFRESH = "refresh";
    public static final String TOKEN_TYPE_ACCESS = "access";
    
    // JWT 서명 알고리즘
    public static final String SIGNATURE_ALGORITHM = "HS256";
    
    // HTTP 헤더
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    
    // 토큰 만료 시간 (밀리초)
    public static final long DEFAULT_ACCESS_TOKEN_EXPIRATION = 3600000L; // 1시간
    public static final long DEFAULT_REFRESH_TOKEN_EXPIRATION = 604800000L; // 7일
    
    // Redis 키 접두사
    public static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    public static final String BLACKLIST_PREFIX = "blacklist:";
    
    // 2FA 관련 상수
    public static final String TWO_FACTOR_TEMP_SECRET_PREFIX = "2fa_temp:";
    public static final String TWO_FACTOR_ATTEMPTS_PREFIX = "2fa_attempts:";
    public static final int TWO_FACTOR_MAX_ATTEMPTS = 3;
    public static final int TWO_FACTOR_ATTEMPT_LOCKOUT_MINUTES = 15;
    
    // 계정 잠금 관련 상수
    public static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
    public static final int ACCOUNT_LOCKOUT_MINUTES = 30;
}
