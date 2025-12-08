package com.agenticcp.core.domain.monitoring.enums;

/**
 * 알림 상태 열거형
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
public enum AlertStatus {
    ACTIVE("활성"),
    TRIGGERED("트리거됨"),
    RESOLVED("해결됨"),
    DISABLED("비활성화");

    private final String description;

    AlertStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
