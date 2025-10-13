package com.agenticcp.core.domain.monitoring.enums;

import lombok.Getter;

/**
 * 할당량 타입 열거형
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-02
 */
@Getter
public enum QuotaType {
    
    /**
     * 일일 메트릭 수집량
     */
    DAILY_METRIC_LIMIT("일일 메트릭 수집량", "하루 동안 수집할 수 있는 메트릭의 최대 개수"),
    
    /**
     * 저장 공간 할당량
     */
    STORAGE_QUOTA("저장 공간 할당량", "메트릭 데이터를 저장할 수 있는 최대 공간 (MB)"),
    
    /**
     * 수집 빈도
     */
    COLLECTION_FREQUENCY("수집 빈도", "메트릭을 수집하는 최소 간격 (초)");
    
    private final String displayName;
    private final String description;
    
    QuotaType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    /**
     * 할당량 타입이 메트릭 수집량인지 확인
     */
    public boolean isMetricLimit() {
        return this == DAILY_METRIC_LIMIT;
    }
    
    /**
     * 할당량 타입이 저장 공간인지 확인
     */
    public boolean isStorage() {
        return this == STORAGE_QUOTA;
    }
    
    /**
     * 할당량 타입이 수집 빈도인지 확인
     */
    public boolean isFrequency() {
        return this == COLLECTION_FREQUENCY;
    }
}
