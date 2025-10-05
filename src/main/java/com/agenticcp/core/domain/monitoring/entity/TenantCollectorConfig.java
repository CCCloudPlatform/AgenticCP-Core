package com.agenticcp.core.domain.monitoring.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import com.agenticcp.core.domain.monitoring.enums.CollectorType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * 테넌트별 메트릭 수집기 설정 엔티티
 * 
 * 각 테넌트가 사용할 수집기와 설정을 관리합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Entity
@Table(name = "tenant_collector_configs", indexes = {
    @Index(name = "idx_tenant_collector_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_tenant_collector_type", columnList = "collector_type"),
    @Index(name = "idx_tenant_collector_enabled", columnList = "is_enabled")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(callSuper = false, exclude = {"id", "createdAt", "updatedAt"})
@ToString(callSuper = true)
public class TenantCollectorConfig extends BaseEntity {

    /**
     * 테넌트 ID
     */
    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    /**
     * 수집기 타입
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "collector_type", nullable = false)
    private CollectorType collectorType;

    /**
     * 수집기 활성화 여부
     */
    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = true;

    /**
     * 수집 주기 (밀리초)
     */
    @Column(name = "collection_interval")
    private Long collectionInterval = 60000L; // 기본 1분

    /**
     * 재시도 횟수
     */
    @Column(name = "retry_count")
    private Integer retryCount = 3;

    /**
     * 타임아웃 (밀리초)
     */
    @Column(name = "timeout")
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
    @Column(name = "priority")
    private Integer priority = 100;

    /**
     * 수집기 설정 메타데이터 목록
     */
    @OneToMany(mappedBy = "tenantCollectorConfig", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TenantCollectorMetadata> metadataList = new ArrayList<>();

    @Builder
    public TenantCollectorConfig(String tenantId, CollectorType collectorType, Boolean isEnabled,
                                Long collectionInterval, Integer retryCount, Long timeout,
                                String targetMetrics, String collectorSettings, Integer priority) {
        this.tenantId = tenantId;
        this.collectorType = collectorType;
        this.isEnabled = isEnabled;
        this.collectionInterval = collectionInterval;
        this.retryCount = retryCount;
        this.timeout = timeout;
        this.targetMetrics = targetMetrics;
        this.collectorSettings = collectorSettings;
        this.priority = priority;
    }

    /**
     * 수집기 활성화/비활성화
     */
    public void setEnabled(Boolean enabled) {
        this.isEnabled = enabled;
    }

    /**
     * 수집 주기 업데이트
     */
    public void updateCollectionInterval(Long interval) {
        this.collectionInterval = interval;
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
