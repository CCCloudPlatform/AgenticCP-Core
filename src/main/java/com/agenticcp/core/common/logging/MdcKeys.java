package com.agenticcp.core.common.logging;

/**
 * 로깅 MDC에서 사용하는 키 및 관련 헤더 상수 정의입니다.
 * 인스턴스화를 방지하기 위한 유틸리티 클래스입니다.
 * 
 * @author AgenticCP Team
 * @since 2025-10-01
 */
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
