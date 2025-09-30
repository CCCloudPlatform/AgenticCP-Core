package com.agenticcp.core.domain.monitoring.enums;

/**
 * 메트릭 수집기 타입 열거형
 * 
 * <p>다양한 메트릭 수집기를 구분하기 위한 타입 정의</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public enum CollectorType {
    
    /**
     * 시스템 리소스 메트릭 수집기
     * CPU, 메모리, 디스크 등 시스템 리소스 정보 수집
     */
    SYSTEM,
    
    /**
     * 애플리케이션 메트릭 수집기
     * JVM, 커스텀 메트릭 등 애플리케이션 관련 정보 수집
     */
    APPLICATION
}
