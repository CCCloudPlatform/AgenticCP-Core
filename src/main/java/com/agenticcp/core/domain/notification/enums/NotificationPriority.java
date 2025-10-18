package com.agenticcp.core.domain.notification.enums;

/**
 * 알림 우선순위 열거형
 */
public enum NotificationPriority {
    LOW("낮음"),
    MEDIUM("보통"),
    HIGH("높음"),
    URGENT("긴급");

    private final String description;

    NotificationPriority(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
