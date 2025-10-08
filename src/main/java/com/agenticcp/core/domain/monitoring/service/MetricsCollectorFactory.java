package com.agenticcp.core.domain.monitoring.service;

import com.agenticcp.core.domain.monitoring.enums.CollectorType;

import java.util.List;

/**
 * 메트릭 수집기 팩토리 인터페이스
 * 
 * <p>다양한 타입의 메트릭 수집기를 생성하고 관리하는 팩토리 인터페이스</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public interface MetricsCollectorFactory {
    
    /**
     * 수집기 타입에 따른 MetricsCollector 생성
     * 
     * @param type 수집기 타입
     * @return 생성된 메트릭 수집기
     */
    MetricsCollector createCollector(CollectorType type);
    
    /**
     * 활성화된 모든 수집기 생성
     * 
     * @return 활성화된 수집기 목록
     */
    List<MetricsCollector> createAllCollectors();
    
    /**
     * 특정 타입의 수집기 존재 여부 확인
     * 
     * @param type 수집기 타입
     * @return 존재 여부
     */
    boolean hasCollector(CollectorType type);
}
