package com.agenticcp.core.domain.platform.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.platform.enums.PlatformErrorCode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 기능 플래그 타겟팅 규칙 엔티티
 * 
 * 고급 타겟팅 시스템을 위한 타겟팅 규칙을 정의합니다.
 * 클라우드 프로바이더, 리전, 테넌트, 사용자 등 다양한 기준으로 기능을 타겟팅할 수 있습니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Entity
@Table(name = "feature_flag_target_rules", indexes = {
    @Index(name = "idx_target_rules_flag_key", columnList = "flag_key"),
    @Index(name = "idx_target_rules_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_target_rules_status", columnList = "status"),
    @Index(name = "idx_target_rules_priority", columnList = "priority")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id", callSuper = false)
@ToString(exclude = "featureFlag")
public class FeatureFlagTargetRule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flag_key", nullable = false, length = 100)
    private String flagKey;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "rule_name", nullable = false, length = 200)
    private String ruleName;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @Column(name = "priority", nullable = false)
    @Builder.Default
    private Integer priority = 0;

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = true;

    // 타겟팅 기준
    @Column(name = "cloud_provider", length = 50)
    private String cloudProvider;

    @Column(name = "region", length = 100)
    private String region;

    @Column(name = "tenant_type", length = 50)
    private String tenantType;

    @Column(name = "tenant_grade", length = 50)
    private String tenantGrade;

    @Column(name = "user_role", length = 50)
    private String userRole;

    @Column(name = "user_attributes", columnDefinition = "TEXT")
    private String userAttributes; // JSON for user-specific attributes

    @Column(name = "custom_attributes", columnDefinition = "TEXT")
    private String customAttributes; // JSON for custom targeting attributes

    // 롤아웃 설정
    @Column(name = "rollout_percentage")
    @Builder.Default
    private Integer rolloutPercentage = 100;

    @Column(name = "rollout_strategy", length = 50)
    @Builder.Default
    private String rolloutStrategy = "PERCENTAGE"; // PERCENTAGE, USER_ID_HASH, CUSTOM

    // 시간 기반 타겟팅
    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    // 추가 설정
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON for additional configuration

    // 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flag_key", referencedColumnName = "flag_key", insertable = false, updatable = false)
    private FeatureFlag featureFlag;

    // 비즈니스 메서드
    public void activate() {
        this.status = Status.ACTIVE;
        this.isEnabled = true;
    }

    public void deactivate() {
        this.status = Status.INACTIVE;
        this.isEnabled = false;
    }

    public boolean isActive() {
        return Status.ACTIVE.equals(this.status) && Boolean.TRUE.equals(this.isEnabled);
    }

    public boolean isTimeBased() {
        return startDate != null || endDate != null;
    }

    public boolean isCurrentlyActive() {
        LocalDateTime now = LocalDateTime.now();
        boolean timeActive = true;
        
        if (startDate != null && now.isBefore(startDate)) {
            timeActive = false;
        }
        if (endDate != null && now.isAfter(endDate)) {
            timeActive = false;
        }
        
        return isActive() && timeActive;
    }

    public void updatePriority(Integer newPriority) {
        this.priority = newPriority;
    }

    public void updateRolloutPercentage(Integer percentage) {
        if (percentage < 0 || percentage > 100) {
            throw new BusinessException(PlatformErrorCode.TARGET_RULE_INVALID_ROLLOUT_PERCENTAGE);
        }
        this.rolloutPercentage = percentage;
    }
}
