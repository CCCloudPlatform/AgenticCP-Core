package com.agenticcp.core.domain.monitoring.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import com.agenticcp.core.domain.monitoring.enums.AlertStatus;
import com.agenticcp.core.domain.monitoring.enums.AlertType;
import com.agenticcp.core.domain.monitoring.enums.Severity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 알림 엔티티 (모니터링 도메인)
 */
@Entity
@Table(name = "alerts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alert extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "alert_name", nullable = false)
    private String alertName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AlertStatus status = AlertStatus.ACTIVE;

    @Column(name = "condition", columnDefinition = "JSON", nullable = false)
    private String condition; // JSON 형태로 저장

    @Column(name = "notification_channels", columnDefinition = "JSON")
    private String notificationChannels; // JSON 형태로 저장

    @Column(name = "recipients", columnDefinition = "JSON")
    private String recipients; // JSON 형태로 저장

    @Column(name = "is_enabled", nullable = false)
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
    private Integer triggerCount = 0;
}
