package com.agenticcp.core.domain.notification.enums;

/**
 * 알림 우선순위 열거형
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
public enum NotificationPriority {
    /**
     * 낮음
     */
    LOW("낮음"),
    
    /**
     * 보통
     */
    MEDIUM("보통"),
    
    /**
     * 높음
     */
    HIGH("높음"),
    
    /**
     * 긴급
     */
    URGENT("긴급");

    private final String description;

    NotificationPriority(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
