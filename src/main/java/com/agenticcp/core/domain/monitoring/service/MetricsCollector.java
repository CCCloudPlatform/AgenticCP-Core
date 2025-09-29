package com.agenticcp.core.domain.monitoring.service;

import com.agenticcp.core.domain.monitoring.dto.SystemMetrics;
import com.agenticcp.core.domain.monitoring.entity.Metric;
import com.agenticcp.core.domain.monitoring.enums.CollectorType;

import java.util.List;

/**
 * 메트릭 수집기 공통 인터페이스
 * 
 * <p>다양한 타입의 메트릭 수집기를 위한 공통 인터페이스 정의</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public interface MetricsCollector {
    
    /**
     * 시스템 메트릭 수집
     * 
     * @return 수집된 시스템 메트릭 정보
     */
    SystemMetrics collectSystemMetrics();
    
    /**
     * 애플리케이션 메트릭 수집
     * 
     * @return 수집된 애플리케이션 메트릭 목록
     */
    List<Metric> collectApplicationMetrics();
    
    /**
     * 수집기 타입 반환
     * 
     * @return 수집기 타입
     */
    CollectorType getCollectorType();
    
    /**
     * 수집기 활성화 여부 확인
     * 
     * @return 활성화 여부
     */
    boolean isEnabled();
}
