package com.agenticcp.core.domain.monitoring.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import com.agenticcp.core.domain.monitoring.enums.AlertStatus;
import com.agenticcp.core.domain.monitoring.enums.AlertType;
import com.agenticcp.core.domain.monitoring.enums.Severity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 알림 엔티티 (모니터링 도메인)
 * 
 * <p>시스템 모니터링 중 발생하는 알림을 저장하고 관리합니다.
 * 알림은 조건, 심각도, 상태 등을 포함하며, 알림 발생 시 알림을 처리합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Entity
@Table(name = "alerts", indexes = {
    @Index(name = "idx_alerts_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_alerts_status", columnList = "status"),
    @Index(name = "idx_alerts_alert_type", columnList = "alert_type"),
    @Index(name = "idx_alerts_tenant_status", columnList = "tenant_id,status")
})
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alert extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    @NotBlank(message = "테넌트 ID는 필수입니다")
    @Size(max = 50, message = "테넌트 ID는 50자를 초과할 수 없습니다")
    private String tenantId;

    @Column(name = "alert_name", nullable = false)
    @NotBlank(message = "알림 이름은 필수입니다")
    @Size(max = 100, message = "알림 이름은 100자를 초과할 수 없습니다")
    private String alertName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false)
    @NotNull(message = "알림 타입은 필수입니다")
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    @NotNull(message = "심각도는 필수입니다")
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private AlertStatus status = AlertStatus.ACTIVE;

    @Column(name = "condition", columnDefinition = "JSON", nullable = false)
    @NotBlank(message = "알림 조건은 필수입니다")
    private String condition; // JSON 형태로 저장

    @Column(name = "notification_channels", columnDefinition = "JSON")
    private String notificationChannels; // JSON 형태로 저장

    @Column(name = "recipients", columnDefinition = "JSON")
    private String recipients; // JSON 형태로 저장

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = true;

    @Column(name = "check_interval")
    private Integer checkInterval; // 체크 간격 (초)

    @Column(name = "cooldown_period")
    private Integer cooldownPeriod; // 쿨다운 기간 (초)

    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata; // JSON 형태로 저장

    @Column(name = "last_triggered")
    private LocalDateTime lastTriggered;

    @Column(name = "trigger_count")
    @Builder.Default
    private Integer triggerCount = 0;

    // ===== 비즈니스 메서드 =====

    /**
     * 알림 활성화
     */
    public void activate() {
        this.status = AlertStatus.ACTIVE;
        this.isEnabled = true;
    }

    /**
     * 알림 비활성화
     */
    public void deactivate() {
        this.status = AlertStatus.DISABLED;
        this.isEnabled = false;
    }

    /**
     * 알림 활성 상태 확인
     * 
     * @return 활성 상태 여부
     */
    public boolean isActive() {
        return this.status == AlertStatus.ACTIVE && Boolean.TRUE.equals(this.isEnabled);
    }

    /**
     * 트리거 횟수 증가
     */
    public void incrementTriggerCount() {
        this.triggerCount = (this.triggerCount == null ? 0 : this.triggerCount) + 1;
        this.lastTriggered = LocalDateTime.now();
    }

    /**
     * 트리거 횟수 리셋
     */
    public void resetTriggerCount() {
        this.triggerCount = 0;
        this.lastTriggered = null;
    }
}
