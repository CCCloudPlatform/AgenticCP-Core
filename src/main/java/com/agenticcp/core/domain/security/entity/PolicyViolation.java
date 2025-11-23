package com.agenticcp.core.domain.security.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 정책 위반 내역 엔티티
 * 
 * <p>보안 정책 위반이 발생했을 때의 모든 정보를 기록합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Entity
@Table(name = "policy_violations", indexes = {
    @Index(name = "idx_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_policy_id", columnList = "policy_id"),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_detected_at", columnList = "detected_at"),
    @Index(name = "idx_severity", columnList = "severity"),
    @Index(name = "idx_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@lombok.EqualsAndHashCode(callSuper = false)
public class PolicyViolation extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "policy_id", nullable = false)
    private Long policyId;

    @Column(name = "policy_name", nullable = false)
    private String policyName;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username")
    private String username;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Enumerated(EnumType.STRING)
    @Column(name = "violation_type", nullable = false)
    private ViolationType violationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private SecurityPolicy.Severity severity;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;

    @Column(name = "resource_type")
    private String resourceType;

    @Column(name = "resource_id")
    private String resourceId;

    @Column(name = "action_attempted")
    private String actionAttempted;

    @Column(name = "violation_details", columnDefinition = "TEXT")
    private String violationDetails; // JSON

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ViolationStatus status;

    @Column(name = "auto_response_executed")
    @Builder.Default
    private Boolean autoResponseExecuted = false;

    @Column(name = "response_action", columnDefinition = "TEXT")
    private String responseAction; // JSON array of executed actions

    @Column(name = "response_executed_at")
    private LocalDateTime responseExecutedAt;

    @Column(name = "notification_sent")
    @Builder.Default
    private Boolean notificationSent = false;

    @Column(name = "notification_sent_at")
    private LocalDateTime notificationSentAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by")
    private String resolvedBy;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "false_positive")
    @Builder.Default
    private Boolean falsePositive = false;

    /**
     * 위반 타입
     */
    public enum ViolationType {
        ACCESS_DENIED,              // 접근 거부
        AUTHENTICATION_FAILURE,      // 인증 실패
        AUTHORIZATION_FAILURE,       // 권한 부족
        BRUTE_FORCE_ATTACK,         // 무차별 대입 공격
        SUSPICIOUS_IP,              // 의심스러운 IP
        SUSPICIOUS_LOCATION,        // 의심스러운 위치
        TIME_RESTRICTION_VIOLATION, // 시간 제한 위반
        DATA_LEAK_ATTEMPT,          // 데이터 유출 시도
        MALICIOUS_REQUEST,          // 악의적인 요청
        POLICY_RULE_VIOLATION,      // 정책 규칙 위반
        RATE_LIMIT_EXCEEDED,        // 요청 한도 초과
        INVALID_INPUT,              // 잘못된 입력
        ENCRYPTION_VIOLATION,       // 암호화 위반
        COMPLIANCE_VIOLATION,       // 규정 준수 위반
        OTHER                       // 기타
    }

    /**
     * 위반 처리 상태
     */
    public enum ViolationStatus {
        DETECTED,       // 감지됨
        PROCESSING,     // 처리 중
        RESPONDED,      // 대응 완료
        RESOLVED,       // 해결됨
        IGNORED,        // 무시됨
        FALSE_POSITIVE  // 오탐지
    }
}

