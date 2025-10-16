package com.agenticcp.core.domain.notification.enums;

/**
 * 알림 타입 열거형
 */
public enum NotificationType {
    SYSTEM("시스템"),
    SECURITY("보안"),
    BILLING("과금"),
    MAINTENANCE("유지보수"),
    ALERT("알림"),
    MARKETING("마케팅"),
    CUSTOM("사용자 정의");

    private final String description;

    NotificationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
