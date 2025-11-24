package com.agenticcp.core.domain.security.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 정책 위반 자동 대응 설정 엔티티
 * 
 * <p>특정 위반 타입 또는 정책에 대한 자동 대응 액션을 설정합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Entity
@Table(name = "violation_response_config", indexes = {
    @Index(name = "idx_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_violation_type", columnList = "violation_type"),
    @Index(name = "idx_severity", columnList = "severity"),
    @Index(name = "idx_is_enabled", columnList = "is_enabled")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@lombok.EqualsAndHashCode(callSuper = false)
public class ViolationResponseConfig extends BaseEntity {

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "config_name", nullable = false)
    private String configName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "violation_type")
    private PolicyViolation.ViolationType violationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity")
    private SecurityPolicy.Severity severity;

    @Column(name = "policy_id")
    private Long policyId;

    @Column(name = "is_global")
    @Builder.Default
    private Boolean isGlobal = false;

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "response_action", nullable = false)
    private ResponseAction responseAction;

    @Column(name = "action_params", columnDefinition = "TEXT")
    private String actionParams; // JSON for action-specific parameters

    @Column(name = "auto_execute")
    @Builder.Default
    private Boolean autoExecute = true;

    @Column(name = "require_approval")
    @Builder.Default
    private Boolean requireApproval = false;

    @Column(name = "send_notification")
    @Builder.Default
    private Boolean sendNotification = true;

    @Column(name = "notification_recipients", columnDefinition = "TEXT")
    private String notificationRecipients; // JSON array of email addresses

    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 0;

    @Column(name = "threshold_count")
    private Integer thresholdCount; // 몇 번 위반 시 액션 실행

    @Column(name = "threshold_window_minutes")
    private Integer thresholdWindowMinutes; // 시간 윈도우 (분)

    @Column(name = "cooldown_minutes")
    private Integer cooldownMinutes; // 재실행 대기 시간

    /**
     * 자동 대응 액션 타입
     */
    public enum ResponseAction {
        BLOCK_USER,         // 사용자 계정 차단
        BLOCK_IP,          // IP 주소 차단
        REQUIRE_2FA,       // 2FA 강제 활성화
        SEND_NOTIFICATION, // 알림만 발송
        LOG_ONLY,          // 로그만 기록
        RESET_PASSWORD,    // 비밀번호 재설정 요구
        SUSPEND_SESSION,   // 세션 중단
        QUARANTINE_USER,   // 사용자 격리 (제한된 권한)
        ESCALATE,          // 상위 관리자에게 에스컬레이션
        AUTO_REMEDIATE     // 자동 복구 시도
    }
}

