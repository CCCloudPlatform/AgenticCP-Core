package com.agenticcp.core.domain.platform.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import com.agenticcp.core.domain.platform.enums.ApprovalStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 기능 플래그 승인 엔티티
 * 
 * 기능 플래그 변경에 대한 승인 워크플로우를 관리합니다.
 * HIGH 또는 CRITICAL 심각도의 플래그 변경 시 승인이 필요합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-15
 */
@Entity
@Table(name = "feature_flag_approvals", indexes = {
    @Index(name = "idx_feature_flag_approvals_flag_id", columnList = "feature_flag_id"),
    @Index(name = "idx_feature_flag_approvals_status", columnList = "status"),
    @Index(name = "idx_feature_flag_approvals_requested_by", columnList = "requested_by"),
    @Index(name = "idx_feature_flag_approvals_requested_at", columnList = "requested_at")
})
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureFlagApproval extends BaseEntity {

    /**
     * 연관된 기능 플래그
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feature_flag_id", nullable = false)
    private FeatureFlag featureFlag;

    /**
     * 승인 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ApprovalStatus status = ApprovalStatus.PENDING;

    /**
     * 요청자 ID
     */
    @Column(name = "requested_by", nullable = false)
    private String requestedBy;

    /**
     * 요청 사유
     */
    @Column(name = "request_reason", columnDefinition = "TEXT")
    private String requestReason;

    /**
     * 변경 전 값 (JSON 형태)
     * 플래그 변경 전 상태를 JSON으로 저장
     */
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    /**
     * 변경 후 값 (JSON 형태)
     * 플래그 변경 후 상태를 JSON으로 저장
     */
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    /**
     * 요청 일시
     */
    @Column(name = "requested_at", nullable = false)
    @Builder.Default
    private LocalDateTime requestedAt = LocalDateTime.now();

    /**
     * 승인자 ID
     */
    @Column(name = "approved_by")
    private String approvedBy;

    /**
     * 승인 사유
     */
    @Column(name = "approval_reason", columnDefinition = "TEXT")
    private String approvalReason;

    /**
     * 승인 일시
     */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /**
     * 거부자 ID
     */
    @Column(name = "rejected_by")
    private String rejectedBy;

    /**
     * 거부 사유
     */
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    /**
     * 거부 일시
     */
    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    /**
     * 승인 완료 여부 확인
     * 
     * @return 승인 완료 여부
     */
    public boolean isApproved() {
        return status == ApprovalStatus.APPROVED;
    }

    /**
     * 거부 여부 확인
     * 
     * @return 거부 여부
     */
    public boolean isRejected() {
        return status == ApprovalStatus.REJECTED;
    }

    /**
     * 대기 중 여부 확인
     * 
     * @return 대기 중 여부
     */
    public boolean isPending() {
        return status == ApprovalStatus.PENDING;
    }

    /**
     * 완료 여부 확인 (승인 또는 거부)
     * 
     * @return 완료 여부
     */
    public boolean isCompleted() {
        return status == ApprovalStatus.APPROVED || status == ApprovalStatus.REJECTED;
    }
}

