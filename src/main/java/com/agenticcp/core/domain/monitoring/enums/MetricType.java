package com.agenticcp.core.domain.monitoring.enums;

/**
 * 메트릭 타입 열거형
 */
public enum MetricType {
    COUNTER("카운터"),
    GAUGE("게이지"),
    HISTOGRAM("히스토그램"),
    SUMMARY("요약"),
    CUSTOM("사용자 정의");

    private final String description;

    MetricType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
