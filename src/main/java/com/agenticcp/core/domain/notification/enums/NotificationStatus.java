package com.agenticcp.core.domain.notification.enums;

/**
 * 알림 상태 열거형
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
public enum NotificationStatus {
    /**
     * 대기
     */
    PENDING("대기"),
    
    /**
     * 발송중
     */
    SENDING("발송중"),
    
    /**
     * 발송완료
     */
    SENT("발송완료"),
    
    /**
     * 전달완료
     */
    DELIVERED("전달완료"),
    
    /**
     * 읽음
     */
    READ("읽음"),
    
    /**
     * 실패
     */
    FAILED("실패"),
    
    /**
     * 취소
     */
    CANCELLED("취소");

    private final String description;

    NotificationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
