package com.agenticcp.core.domain.platform.enums;

/**
 * 멀티클라우드 환경 타입
 * 
 * 테넌트가 사용하는 클라우드 환경의 유형을 정의합니다.
 * CloudResource 분석을 통해 자동으로 감지됩니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-06
 */
public enum MultiCloudEnvironment {
    
    /**
     * 단일 클라우드: 하나의 클라우드 프로바이더만 사용
     * 
     * 예시:
     * - AWS만 사용
     * - Azure만 사용
     * - GCP만 사용
     */
    SINGLE_CLOUD,
    
    /**
     * 멀티 클라우드: 2개 이상의 클라우드 프로바이더 사용
     * 
     * 예시:
     * - AWS + Azure
     * - AWS + GCP
     * - Azure + GCP
     * - AWS + Azure + GCP
     */
    MULTI_CLOUD,
    
    /**
     * 하이브리드: 클라우드 프로바이더 + 온프레미스 혼용
     * 
     * 예시:
     * - AWS + ON_PREMISE
     * - Azure + ON_PREMISE
     * - GCP + ON_PREMISE
     * - (AWS + Azure) + ON_PREMISE
     */
    HYBRID,
    
    /**
     * 온프레미스: 온프레미스 환경만 사용하거나 클라우드 리소스가 없는 경우
     * 
     * 예시:
     * - 자체 데이터센터만 사용
     * - 클라우드 리소스가 등록되지 않은 테넌트
     */
    ON_PREMISE
}

