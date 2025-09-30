package com.agenticcp.core.domain.monitoring.storage;

import com.agenticcp.core.domain.monitoring.entity.Metric;
import com.agenticcp.core.domain.monitoring.enums.StorageType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 메트릭 저장소 인터페이스
 * 
 * <p>다양한 시계열 데이터베이스에 메트릭을 저장하고 조회하는 기능을 정의합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public interface MetricsStorage {
    
    /**
     * 메트릭 목록을 저장소에 저장합니다.
     * 
     * @param metrics 저장할 메트릭 목록
     * @throws com.agenticcp.core.common.exception.BusinessException 저장 실패 시
     */
    void saveMetrics(List<Metric> metrics);
    
    /**
     * 특정 메트릭명과 시간 범위로 메트릭을 조회합니다.
     * 
     * @param metricName 조회할 메트릭명
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return 조회된 메트릭 목록
     * @throws com.agenticcp.core.common.exception.BusinessException 조회 실패 시
     */
    List<Metric> getMetrics(String metricName, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 저장소 연결 상태를 확인합니다.
     * 
     * @return 연결 상태
     */
    boolean isConnected();
    
    /**
     * 저장소에 연결합니다.
     * 
     * @throws com.agenticcp.core.common.exception.BusinessException 연결 실패 시
     */
    void connect();
    
    /**
     * 저장소 연결을 해제합니다.
     */
    void disconnect();
    
    /**
     * 저장소 타입을 반환합니다.
     * 
     * @return 저장소 타입
     */
    StorageType getStorageType();
    
    /**
     * 저장소가 활성화되어 있는지 확인합니다.
     * 
     * @return 활성화 상태
     */
    boolean isEnabled();
    
    /**
     * 저장소 활성화 상태를 설정합니다.
     * 
     * @param enabled 활성화 여부
     */
    void setEnabled(boolean enabled);
}
