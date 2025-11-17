package com.agenticcp.core.domain.cloud.port.outbound.vpc;

/**
 * VPC 관련 상수 정의
 * 
 * VPC 관리에서 사용되는 공통 상수들을 정의합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
public final class VpcConstants {
    
    /**
     * VPC 서비스 키 (AWS EC2)
     */
    public static final String SERVICE_KEY = "EC2";
    
    /**
     * VPC 리소스 타입
     */
    public static final String RESOURCE_TYPE = "VPC";
    
    private VpcConstants() {
        // 인스턴스화 방지
    }
}

