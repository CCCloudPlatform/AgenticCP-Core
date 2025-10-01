package com.agenticcp.core.common.logging;

public final class MdcKeys {
    
    // 공통 키
    public static final String REQUEST_ID = "requestId";
    public static final String TENANT_ID = "tenantId";
    
    // 사용자 관련 키
    public static final String USER_ID = "userId";
    public static final String SESSION_ID = "sessionId";
    
    // 네트워크 관련 키
    public static final String CLIENT_IP = "clientIp";
    public static final String USER_AGENT = "userAgent";
    
    // HTTP 헤더 상수
    public static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    public static final String HEADER_X_REAL_IP = "X-Real-IP";
    public static final String HEADER_USER_AGENT = "User-Agent";
    
    private MdcKeys() {
    }
}
