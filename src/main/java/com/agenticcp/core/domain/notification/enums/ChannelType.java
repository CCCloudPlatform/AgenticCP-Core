package com.agenticcp.core.domain.notification.enums;

/**
 * 알림 채널 타입 열거형
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
public enum ChannelType {
    /**
     * 이메일
     */
    EMAIL("이메일"),
    
    /**
     * SMS
     */
    SMS("SMS"),
    
    /**
     * 푸시 알림
     */
    PUSH("푸시"),
    
    /**
     * 웹훅
     */
    WEBHOOK("웹훅"),
    
    /**
     * Slack
     */
    SLACK("Slack"),
    
    /**
     * Microsoft Teams
     */
    TEAMS("Microsoft Teams"),
    
    /**
     * Discord
     */
    DISCORD("Discord"),
    
    /**
     * 사용자 정의 채널
     */
    CUSTOM("사용자 정의");

    private final String description;

    ChannelType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
