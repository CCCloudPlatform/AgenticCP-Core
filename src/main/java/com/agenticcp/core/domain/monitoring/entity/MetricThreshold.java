package com.agenticcp.core.domain.monitoring.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 메트릭 임계값 엔티티
 * 
 * <p>메트릭에 대한 임계값 설정을 저장합니다.
 * 임계값은 알림 및 경고를 발생시키는 기준이 됩니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Entity
@Table(name = "metric_thresholds", indexes = {
    @Index(name = "idx_metric_thresholds_metric_name", columnList = "metric_name"),
    @Index(name = "idx_metric_thresholds_threshold_type", columnList = "threshold_type"),
    @Index(name = "idx_metric_thresholds_is_active", columnList = "is_active"),
    @Index(name = "idx_metric_thresholds_created_at", columnList = "created_at")
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, exclude = {"id", "createdAt", "updatedAt"})
@ToString(callSuper = true)
public class MetricThreshold extends BaseEntity {

    /**
     * 메트릭 이름 (예: cpu.usage, memory.used)
     */
    @NotBlank(message = "메트릭 이름은 필수입니다")
    @Size(max = 100, message = "메트릭 이름은 100자를 초과할 수 없습니다")
    @Column(name = "metric_name", nullable = false, length = 100)
    private String metricName;

    /**
     * 임계값 타입
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "threshold_type", nullable = false, length = 20)
    @NotNull(message = "임계값 타입은 필수입니다")
    private ThresholdType thresholdType;

    /**
     * 임계값
     */
    @NotNull(message = "임계값은 필수입니다")
    @Column(name = "threshold_value", nullable = false)
    private Double thresholdValue;

    /**
     * 비교 연산자 (>, >=, <, <=, ==, !=)
     */
    @NotBlank(message = "연산자는 필수입니다")
    @Size(max = 10, message = "연산자는 10자를 초과할 수 없습니다")
    @Pattern(regexp = "^(>|>=|<|<=|==|!=)$", message = "연산자는 >, >=, <, <=, ==, != 중 하나여야 합니다")
    @Column(name = "operator", nullable = false, length = 10)
    private String operator;

    /**
     * 활성화 여부
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * 임계값 설명
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 알림 활성화 여부
     */
    @Column(name = "alert_enabled", nullable = false)
    @Builder.Default
    private Boolean alertEnabled = true;

    /**
     * 알림 지속 시간 (초)
     */
    @Column(name = "alert_duration")
    @Builder.Default
    private Integer alertDuration = 300; // 5분

    /**
     * 알림 심각도
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 20)
    @Builder.Default
    private Severity severity = Severity.MEDIUM;

    /**
     * 마지막 알림 시간
     */
    @Column(name = "last_alert_at")
    private LocalDateTime lastAlertAt;

    /**
     * 임계값 타입 열거형
     */
    public enum ThresholdType {
        WARNING,    // 경고
        CRITICAL,   // 위험
        INFO        // 정보
    }

    /**
     * 심각도 열거형
     */
    public enum Severity {
        LOW,        // 낮음
        MEDIUM,     // 보통
        HIGH,       // 높음
        CRITICAL    // 위험
    }

    /**
     * 임계값 활성화/비활성화
     * 
     * @param isActive 활성화 여부
     */
    public void setActive(Boolean isActive) {
        this.isActive = isActive;
    }

    /**
     * 알림 활성화/비활성화
     * 
     * @param alertEnabled 알림 활성화 여부
     */
    public void setAlertEnabled(Boolean alertEnabled) {
        this.alertEnabled = alertEnabled;
    }

    /**
     * 임계값 업데이트
     * 
     * @param thresholdValue 새로운 임계값
     * @param operator 새로운 연산자
     */
    public void updateThreshold(Double thresholdValue, String operator) {
        this.thresholdValue = thresholdValue;
        this.operator = operator;
    }

    /**
     * 마지막 알림 시간 업데이트
     * 
     * @param lastAlertAt 마지막 알림 시간
     */
    public void updateLastAlertAt(LocalDateTime lastAlertAt) {
        this.lastAlertAt = lastAlertAt;
    }

    /**
     * 임계값 위반 여부 확인
     * 
     * @param metricValue 메트릭 값
     * @return 임계값 위반 여부
     */
    public boolean isThresholdViolated(Double metricValue) {
        if (!isActive || metricValue == null) {
            return false;
        }

        return switch (operator) {
            case ">" -> metricValue > thresholdValue;
            case ">=" -> metricValue >= thresholdValue;
            case "<" -> metricValue < thresholdValue;
            case "<=" -> metricValue <= thresholdValue;
            case "==" -> metricValue.equals(thresholdValue);
            case "!=" -> !metricValue.equals(thresholdValue);
            default -> false;
        };
    }
}
