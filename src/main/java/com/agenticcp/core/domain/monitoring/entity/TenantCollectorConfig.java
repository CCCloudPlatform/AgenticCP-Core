package com.agenticcp.core.domain.monitoring.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import com.agenticcp.core.domain.monitoring.enums.CollectorType;
import com.agenticcp.core.domain.monitoring.enums.QuotaExceededAction;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 테넌트별 메트릭 수집기 설정 엔티티
 * 
 * 각 테넌트가 사용할 수집기와 설정을 관리합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Entity
@Table(name = "tenant_collector_configs", indexes = {
    @Index(name = "idx_tenant_collector_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_tenant_collector_type", columnList = "collector_type"),
    @Index(name = "idx_tenant_collector_enabled", columnList = "is_enabled")
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, exclude = {"id", "createdAt", "updatedAt", "metadataList"})
@ToString(callSuper = true)
public class TenantCollectorConfig extends BaseEntity {

    /**
     * 테넌트 ID
     */
    @NotBlank(message = "테넌트 ID는 필수입니다")
    @Size(max = 50, message = "테넌트 ID는 50자를 초과할 수 없습니다")
    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    /**
     * 수집기 타입
     */
    @NotNull(message = "수집기 타입은 필수입니다")
    @Enumerated(EnumType.STRING)
    @Column(name = "collector_type", nullable = false)
    private CollectorType collectorType;

    /**
     * 수집기 활성화 여부
     */
    @NotNull(message = "활성화 여부는 필수입니다")
    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = true;

    /**
     * 수집 주기 (밀리초) - 1분 이하로 제한
     */
    @Min(value = 1000, message = "수집 주기는 최소 1초(1000ms) 이상이어야 합니다")
    @Max(value = 60000, message = "메트릭 수집 주기는 1분(60000ms) 이하여야 합니다")
    @Column(name = "collection_interval")
    @Builder.Default
    private Long collectionInterval = 60000L; // 기본 1분

    /**
     * 재시도 횟수
     */
    @Min(value = 0, message = "재시도 횟수는 0 이상이어야 합니다")
    @Max(value = 10, message = "재시도 횟수는 10 이하여야 합니다")
    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 3;

    /**
     * 타임아웃 (밀리초)
     */
    @Min(value = 1000, message = "타임아웃은 최소 1초(1000ms) 이상이어야 합니다")
    @Max(value = 300000, message = "타임아웃은 최대 5분(300000ms) 이하여야 합니다")
    @Column(name = "timeout")
    @Builder.Default
    private Long timeout = 30000L; // 기본 30초

    /**
     * 수집할 메트릭 목록 (JSON)
     * 예: ["cpu.usage", "memory.usage", "disk.usage"]
     */
    @Column(name = "target_metrics", columnDefinition = "TEXT")
    private String targetMetrics;

    /**
     * 수집기별 추가 설정 (JSON)
     * 예: {"aws_region": "us-east-1", "prometheus_url": "http://prometheus:9090"}
     */
    @Column(name = "collector_settings", columnDefinition = "TEXT")
    private String collectorSettings;

    /**
     * 우선순위 (낮을수록 높은 우선순위)
     */
    @Min(value = 1, message = "우선순위는 1 이상이어야 합니다")
    @Max(value = 1000, message = "우선순위는 1000 이하여야 합니다")
    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 100;

    /**
     * 수집기 설정 메타데이터 목록
     */
    @OneToMany(mappedBy = "tenantCollectorConfig", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TenantCollectorMetadata> metadataList = new ArrayList<>();

    // ===== 할당량 관련 필드들 =====
    
    /**
     * 일일 메트릭 수집량 제한
     */
    @Min(value = 1, message = "일일 메트릭 수집량 제한은 1 이상이어야 합니다")
    @Max(value = 10000000, message = "일일 메트릭 수집량 제한은 10,000,000 이하여야 합니다")
    @Column(name = "daily_metric_limit")
    private Long dailyMetricLimit;
    
    /**
     * 메트릭 저장 공간 할당량 (MB)
     */
    @Min(value = 1, message = "저장 공간 할당량은 최소 1MB 이상이어야 합니다")
    @Max(value = 1048576, message = "저장 공간 할당량은 최대 1TB(1048576MB) 이하여야 합니다")
    @Column(name = "storage_quota_mb")
    private Long storageQuotaMb;
    
    /**
     * 현재 일일 사용량
     */
    @Min(value = 0, message = "현재 일일 사용량은 0 이상이어야 합니다")
    @Column(name = "current_daily_usage")
    @Builder.Default
    private Long currentDailyUsage = 0L;
    
    /**
     * 현재 저장 공간 사용량 (MB)
     */
    @Min(value = 0, message = "현재 저장 공간 사용량은 0 이상이어야 합니다")
    @Column(name = "current_storage_usage_mb")
    @Builder.Default
    private Long currentStorageUsageMb = 0L;
    
    /**
     * 할당량 초과 시 동작
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "quota_exceeded_action")
    @Builder.Default
    private QuotaExceededAction quotaExceededAction = QuotaExceededAction.WARN_ONLY;
    
    /**
     * 할당량 리셋 일시
     */
    @Column(name = "last_reset_at")
    private LocalDateTime lastResetAt;

    /**
     * 수집기 활성화/비활성화
     */
    public void setEnabled(Boolean enabled) {
        this.isEnabled = enabled;
    }
    
    /**
     * 수집기 활성화 여부 설정
     */
    public void setIsEnabled(Boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    /**
     * 수집 주기 업데이트
     */
    public void updateCollectionInterval(Long interval) {
        this.collectionInterval = interval;
    }
    
    /**
     * 수집 주기 설정
     */
    public void setCollectionInterval(Long collectionInterval) {
        this.collectionInterval = collectionInterval;
    }

    /**
     * 타겟 메트릭 업데이트
     */
    public void updateTargetMetrics(String metrics) {
        this.targetMetrics = metrics;
    }

    /**
     * 수집기 설정 업데이트
     */
    public void updateCollectorSettings(String settings) {
        this.collectorSettings = settings;
    }

    /**
     * 재시도 횟수 업데이트
     */
    public void updateRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    // ===== 할당량 관련 메서드들 =====
    
    /**
     * 할당량 초과 여부 확인
     */
    public boolean isQuotaExceeded() {
        if (dailyMetricLimit != null && currentDailyUsage != null) {
            return currentDailyUsage >= dailyMetricLimit;
        }
        return false;
    }
    
    /**
     * 일일 사용량 증가
     */
    public void incrementDailyUsage(Long amount) {
        if (currentDailyUsage == null) {
            currentDailyUsage = 0L;
        }
        this.currentDailyUsage += amount;
    }
    
    /**
     * 저장 공간 사용량 증가
     */
    public void incrementStorageUsage(Long amountMb) {
        if (currentStorageUsageMb == null) {
            currentStorageUsageMb = 0L;
        }
        this.currentStorageUsageMb += amountMb;
    }
    
    /**
     * 일일 사용량 리셋
     */
    public void resetDailyUsage() {
        this.currentDailyUsage = 0L;
        this.lastResetAt = LocalDateTime.now();
    }
    
    /**
     * 할당량 설정 업데이트
     */
    public void updateQuota(Long dailyMetricLimit, Long storageQuotaMb, QuotaExceededAction quotaExceededAction) {
        this.dailyMetricLimit = dailyMetricLimit;
        this.storageQuotaMb = storageQuotaMb;
        this.quotaExceededAction = quotaExceededAction;
    }
    
    /**
     * 할당량 초과 동작 설정
     */
    public void setQuotaExceededAction(QuotaExceededAction action) {
        this.quotaExceededAction = action;
    }
    
    /**
     * 일일 메트릭 수집량 제한 설정
     */
    public void setDailyMetricLimit(Long dailyMetricLimit) {
        this.dailyMetricLimit = dailyMetricLimit;
    }
    
    /**
     * 메트릭 저장 공간 할당량 설정
     */
    public void setStorageQuotaMb(Long storageQuotaMb) {
        this.storageQuotaMb = storageQuotaMb;
    }
    
    /**
     * 현재 일일 사용량 설정
     */
    public void setCurrentDailyUsage(Long currentDailyUsage) {
        this.currentDailyUsage = currentDailyUsage;
    }
    
    /**
     * 현재 저장 공간 사용량 설정
     */
    public void setCurrentStorageUsageMb(Long currentStorageUsageMb) {
        this.currentStorageUsageMb = currentStorageUsageMb;
    }
    
    /**
     * 할당량 리셋 일시 설정
     */
    public void setLastResetAt(LocalDateTime lastResetAt) {
        this.lastResetAt = lastResetAt;
    }

    /**
     * 타임아웃 업데이트
     */
    public void updateTimeout(Long timeout) {
        this.timeout = timeout;
    }

    /**
     * 우선순위 업데이트
     */
    public void updatePriority(Integer priority) {
        this.priority = priority;
    }

    /**
     * 메타데이터 추가
     */
    public void addMetadata(TenantCollectorMetadata metadata) {
        this.metadataList.add(metadata);
        metadata.setTenantCollectorConfig(this);
    }
}
