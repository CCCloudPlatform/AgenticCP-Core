package com.agenticcp.core.domain.notification.enums;

/**
 * 알림 채널 타입 열거형
 */
public enum ChannelType {
    EMAIL("이메일"),
    SMS("SMS"),
    PUSH("푸시"),
    WEBHOOK("웹훅"),
    SLACK("Slack"),
    TEAMS("Microsoft Teams"),
    DISCORD("Discord"),
    CUSTOM("사용자 정의");

    private final String description;

    ChannelType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
