package com.agenticcp.core.domain.monitoring.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 테넌트별 데이터 보관 정책 엔티티
 * 
 * 테넌트별로 메트릭 데이터의 보관 기간과 정책을 관리합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Entity
@Table(name = "tenant_data_retention_policies", indexes = {
    @Index(name = "idx_retention_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_retention_data_type", columnList = "data_type"),
    @Index(name = "idx_retention_enabled", columnList = "is_enabled")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, exclude = {"id", "createdAt", "updatedAt"})
@ToString(callSuper = true)
public class TenantDataRetentionPolicy extends BaseEntity {

    /**
     * 테넌트 ID
     */
    @NotBlank(message = "테넌트 ID는 필수입니다")
    @Size(max = 50, message = "테넌트 ID는 50자를 초과할 수 없습니다")
    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    /**
     * 데이터 타입 (METRIC, ALERT, DASHBOARD, LOG 등)
     */
    @NotBlank(message = "데이터 타입은 필수입니다")
    @Size(max = 50, message = "데이터 타입은 50자를 초과할 수 없습니다")
    @Column(name = "data_type", nullable = false, length = 50)
    private String dataType;

    /**
     * 보관 기간 (일 단위) - 기본 30일
     */
    @Min(value = 1, message = "보관 기간은 최소 1일 이상이어야 합니다")
    @Max(value = 365, message = "보관 기간은 최대 365일 이하여야 합니다")
    @Column(name = "retention_days", nullable = false)
    @Builder.Default
    private Integer retentionDays = 30;

    /**
     * 정책 활성화 여부
     */
    @NotNull(message = "정책 활성화 여부는 필수입니다")
    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = true;

    /**
     * 데이터 삭제 방식 (DELETE, ARCHIVE, COMPRESS)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "deletion_strategy")
    @Builder.Default
    private DeletionStrategy deletionStrategy = DeletionStrategy.DELETE;

    /**
     * 아카이브 저장소 경로 (ARCHIVE 방식 사용 시)
     */
    @Size(max = 500, message = "아카이브 경로는 500자를 초과할 수 없습니다")
    @Column(name = "archive_path", length = 500)
    private String archivePath;

    /**
     * 마지막 정리 실행 일시
     */
    @Column(name = "last_cleanup_at")
    private LocalDateTime lastCleanupAt;

    /**
     * 마지막 정리 시 삭제된 레코드 수
     */
    @Min(value = 0, message = "삭제된 레코드 수는 0 이상이어야 합니다")
    @Column(name = "last_deleted_count")
    @Builder.Default
    private Long lastDeletedCount = 0L;

    /**
     * 정책 설명
     */
    @Size(max = 1000, message = "정책 설명은 1000자를 초과할 수 없습니다")
    @Column(name = "description", length = 1000)
    private String description;

    /**
     * 정책 우선순위 (낮은 숫자가 높은 우선순위)
     */
    @Min(value = 1, message = "우선순위는 1 이상이어야 합니다")
    @Max(value = 100, message = "우선순위는 100 이하여야 합니다")
    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 50;

    /**
     * 다음 정리 실행 예정 일시 계산
     */
    public LocalDateTime getNextCleanupAt() {
        if (lastCleanupAt == null) {
            return LocalDateTime.now();
        }
        // 매일 정리 (기본값)
        return lastCleanupAt.plusDays(1);
    }

    /**
     * 정리 실행 필요 여부 확인
     */
    public boolean isCleanupNeeded() {
        if (!isEnabled) {
            return false;
        }
        return LocalDateTime.now().isAfter(getNextCleanupAt());
    }

    /**
     * 정리 실행 후 상태 업데이트
     */
    public void updateCleanupStatus(Long deletedCount) {
        this.lastCleanupAt = LocalDateTime.now();
        this.lastDeletedCount = deletedCount;
    }

    /**
     * 보관 기간 설정
     */
    public void setRetentionDays(Integer retentionDays) {
        this.retentionDays = retentionDays;
    }

    /**
     * 삭제 방식 설정
     */
    public void setDeletionStrategy(DeletionStrategy deletionStrategy) {
        this.deletionStrategy = deletionStrategy;
    }

    /**
     * 설명 설정
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 활성화 여부 설정
     */
    public void setIsEnabled(Boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    /**
     * 데이터 삭제 방식 열거형
     */
    public enum DeletionStrategy {
        /**
         * 완전 삭제
         */
        DELETE("완전 삭제"),
        
        /**
         * 아카이브 후 삭제
         */
        ARCHIVE("아카이브 후 삭제"),
        
        /**
         * 압축 후 보관
         */
        COMPRESS("압축 후 보관");

        private final String description;

        DeletionStrategy(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
