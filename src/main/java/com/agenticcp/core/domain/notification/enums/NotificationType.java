package com.agenticcp.core.domain.notification.enums;

/**
 * 알림 타입 열거형
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
public enum NotificationType {
    /**
     * 시스템
     */
    SYSTEM("시스템"),
    
    /**
     * 보안
     */
    SECURITY("보안"),
    
    /**
     * 과금
     */
    BILLING("과금"),
    
    /**
     * 유지보수
     */
    MAINTENANCE("유지보수"),
    
    /**
     * 알림
     */
    ALERT("알림"),
    
    /**
     * 마케팅
     */
    MARKETING("마케팅"),
    
    /**
     * 사용자 정의
     */
    CUSTOM("사용자 정의");

    private final String description;

    NotificationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
