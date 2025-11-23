package com.agenticcp.core.domain.monitoring.enums;

/**
 * 알림 타입 열거형
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
public enum AlertType {
    THRESHOLD("임계값"),
    ANOMALY("이상치"),
    TREND("트렌드"),
    AVAILABILITY("가용성"),
    PERFORMANCE("성능"),
    CUSTOM("사용자 정의");

    private final String description;

    AlertType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
