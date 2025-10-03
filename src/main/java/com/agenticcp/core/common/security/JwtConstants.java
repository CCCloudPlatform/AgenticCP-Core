package com.agenticcp.core.common.security;

/**
 * JWT 관련 상수 정의
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public final class JwtConstants {

    private JwtConstants() {}

    // JWT Claims
    public static final String CLAIM_USERNAME = "username";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_PERMISSIONS = "permissions";
    public static final String CLAIM_TENANT_ID = "tenantId";
    public static final String CLAIM_TENANT_KEY = "tenantKey";
    public static final String CLAIM_TYPE = "type";
    
    // Token Types
    public static final String TOKEN_TYPE_ACCESS = "ACCESS";
    public static final String TOKEN_TYPE_REFRESH = "REFRESH";
    
    // Header
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
}
