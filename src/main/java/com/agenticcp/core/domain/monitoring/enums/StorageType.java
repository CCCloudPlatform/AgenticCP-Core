package com.agenticcp.core.domain.monitoring.enums;

/**
 * 메트릭 저장소 타입 열거형
 * 
 * <p>지원하는 시계열 데이터베이스 타입을 정의합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public enum StorageType {
    
    /**
     * InfluxDB - 고성능 시계열 데이터베이스
     */
    INFLUXDB("InfluxDB", "고성능 시계열 데이터베이스"),
    
    /**
     * TimescaleDB - PostgreSQL 기반 시계열 데이터베이스
     */
    TIMESCALEDB("TimescaleDB", "PostgreSQL 기반 시계열 데이터베이스"),
    
    /**
     * Prometheus - 메트릭 모니터링 시스템
     */
    PROMETHEUS("Prometheus", "메트릭 모니터링 시스템");
    
    private final String displayName;
    private final String description;
    
    StorageType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    /**
     * 저장소 타입의 표시명을 반환합니다.
     * 
     * @return 표시명
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 저장소 타입의 설명을 반환합니다.
     * 
     * @return 설명
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 저장소 타입이 지원되는지 확인합니다.
     * 
     * @return 지원 여부
     */
    public boolean isSupported() {
        return this != null;
    }
}
