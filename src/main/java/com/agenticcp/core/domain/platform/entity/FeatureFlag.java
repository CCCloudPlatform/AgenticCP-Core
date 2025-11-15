package com.agenticcp.core.domain.platform.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.domain.platform.enums.FeatureFlagSeverity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 기능 플래그 엔티티
 * 
 * <p>시스템의 기능을 동적으로 활성화/비활성화하기 위한 기능 플래그를 관리합니다.</p>
 * <p>롤아웃 비율, 타겟 테넌트/사용자, 시작/종료 일시 등을 설정할 수 있습니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-15
 */
@Entity
@Table(name = "feature_flags", indexes = {
    @Index(name = "idx_feature_flags_flag_key", columnList = "flag_key"),
    @Index(name = "idx_feature_flags_status", columnList = "status"),
    @Index(name = "idx_feature_flags_is_enabled", columnList = "is_enabled"),
    @Index(name = "idx_feature_flags_severity", columnList = "severity"),
    @Index(name = "idx_feature_flags_status_enabled", columnList = "status, is_enabled")
})
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureFlag extends BaseEntity {

    /**
     * 기능 플래그 키 (고유 식별자)
     */
    @NotBlank(message = "기능 플래그 키는 필수입니다")
    @Size(min = 1, max = 100, message = "기능 플래그 키는 1-100자 사이여야 합니다")
    @Column(name = "flag_key", nullable = false, unique = true, length = 100)
    private String flagKey;

    /**
     * 기능 플래그 이름
     */
    @NotBlank(message = "기능 플래그 이름은 필수입니다")
    @Size(min = 1, max = 200, message = "기능 플래그 이름은 1-200자 사이여야 합니다")
    @Column(name = "flag_name", nullable = false, length = 200)
    private String flagName;

    /**
     * 기능 플래그 설명
     */
    @Size(max = 1000, message = "설명은 1000자를 초과할 수 없습니다")
    @Column(name = "description", length = 1000)
    private String description;

    /**
     * 활성화 여부
     */
    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = false;

    /**
     * 상태 (ACTIVE, INACTIVE, SUSPENDED 등)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.ACTIVE;

    /**
     * 심각도 레벨 (LOW, MEDIUM, HIGH, CRITICAL)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    @Builder.Default
    private FeatureFlagSeverity severity = FeatureFlagSeverity.LOW;

    /**
     * 대상 테넌트 목록 (JSON 배열 형태)
     */
    @Column(name = "target_tenants", columnDefinition = "TEXT")
    private String targetTenants;

    /**
     * 대상 사용자 목록 (JSON 배열 형태)
     */
    @Column(name = "target_users", columnDefinition = "TEXT")
    private String targetUsers;

    /**
     * 롤아웃 비율 (0-100)
     */
    @Column(name = "rollout_percentage")
    @Builder.Default
    private Integer rolloutPercentage = 0;

    /**
     * 시작 일시
     */
    @Column(name = "start_date")
    private LocalDateTime startDate;

    /**
     * 종료 일시
     */
    @Column(name = "end_date")
    private LocalDateTime endDate;

    /**
     * 메타데이터 (JSON 형태의 추가 설정)
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * 캐시 TTL (초 단위)
     * NULL인 경우 기본값(300초) 사용
     */
    @Column(name = "cache_ttl_seconds")
    private Integer cacheTtlSeconds;

    // 비즈니스 메서드

    /**
     * 기능 플래그를 활성화합니다.
     */
    public void enable() {
        this.isEnabled = true;
    }

    /**
     * 기능 플래그를 비활성화합니다.
     */
    public void disable() {
        this.isEnabled = false;
    }

    /**
     * 기능 플래그가 활성화되어 있는지 확인합니다.
     * 
     * @return 활성화 여부
     */
    public boolean isEnabled() {
        return Boolean.TRUE.equals(this.isEnabled);
    }

    /**
     * 기능 플래그를 활성 상태로 변경합니다.
     */
    public void activate() {
        this.status = Status.ACTIVE;
    }

    /**
     * 기능 플래그를 비활성 상태로 변경합니다.
     */
    public void deactivate() {
        this.status = Status.INACTIVE;
    }

    /**
     * 기능 플래그를 일시정지 상태로 변경합니다.
     */
    public void suspend() {
        this.status = Status.SUSPENDED;
    }

    /**
     * 기능 플래그가 활성 상태인지 확인합니다.
     * 
     * @return 활성 상태 여부
     */
    public boolean isActive() {
        return Status.ACTIVE.equals(this.status);
    }

    /**
     * 기능 플래그가 현재 유효한 기간 내에 있는지 확인합니다.
     * 
     * @return 유효 기간 내 여부
     */
    public boolean isWithinValidPeriod() {
        LocalDateTime now = LocalDateTime.now();
        if (startDate != null && now.isBefore(startDate)) {
            return false;
        }
        if (endDate != null && now.isAfter(endDate)) {
            return false;
        }
        return true;
    }

    /**
     * 기능 플래그가 사용 가능한지 확인합니다.
     * (활성화되어 있고, 활성 상태이며, 유효 기간 내에 있는지 확인)
     * 
     * @return 사용 가능 여부
     */
    public boolean isAvailable() {
        return isEnabled() && isActive() && isWithinValidPeriod();
    }
}
