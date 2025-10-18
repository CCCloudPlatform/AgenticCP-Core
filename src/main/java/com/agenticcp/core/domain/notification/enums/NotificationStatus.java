package com.agenticcp.core.domain.notification.enums;

/**
 * 알림 상태 열거형
 */
public enum NotificationStatus {
    PENDING("대기"),
    SENDING("발송중"),
    SENT("발송완료"),
    DELIVERED("전달완료"),
    READ("읽음"),
    FAILED("실패"),
    CANCELLED("취소");

    private final String description;

    NotificationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
