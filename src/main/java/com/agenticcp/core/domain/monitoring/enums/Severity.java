package com.agenticcp.core.domain.monitoring.enums;

/**
 * 심각도 열거형
 */
public enum Severity {
    INFO("정보"),
    WARNING("경고"),
    ERROR("에러"),
    CRITICAL("치명적");

    private final String description;

    Severity(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
