package com.agenticcp.core.domain.monitoring.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 메트릭 데이터를 저장하는 엔티티
 * 
 * <p>시스템 리소스 메트릭(CPU, 메모리, 디스크)과 애플리케이션 메트릭을 저장합니다.
 * 메트릭은 메타데이터, 태그, 임계값과 연관관계를 가집니다.</p>
 * 
 */
@Entity
@Table(name = "metrics", indexes = {
    @Index(name = "idx_metrics_name", columnList = "metric_name"),
    @Index(name = "idx_metrics_type", columnList = "metric_type"),
    @Index(name = "idx_metrics_collected_at", columnList = "collected_at"),
    @Index(name = "idx_metrics_source", columnList = "source"),
    @Index(name = "idx_metrics_status", columnList = "status")
    // TODO: 테넌트 도메인 구현 후 활성화 예정
    // @Index(name = "idx_metrics_tenant_id", columnList = "tenant_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Metric extends BaseEntity {

    /**
     * 메트릭 이름 (예: cpu.usage, memory.used, disk.free)
     */
    @Column(name = "metric_name", nullable = false, length = 100)
    private String metricName;

    /**
     * 메트릭 값
     */
    @Column(name = "metric_value", nullable = false)
    private Double metricValue;

    /**
     * 메트릭 단위 (예: %, MB, GB, ms)
     */
    @Column(name = "unit", length = 20)
    private String unit;

    /**
     * 메트릭 타입 (SYSTEM, APPLICATION)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type", nullable = false)
    private MetricType metricType;

    /**
     * 메트릭 수집 시간
     */
    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    /**
     * 메트릭 소스 (예: system, application, custom)
     */
    @Column(name = "source", length = 50)
    private String source = "system";

    /**
     * 메트릭 상태 (ACTIVE, INACTIVE, ARCHIVED)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private Status status = Status.ACTIVE;

    /**
     * 추가 메타데이터 (JSON 형태로 저장)
     * 예: {"host": "server1", "region": "us-east-1", "instance_type": "t3.medium"}
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * 메트릭 메타데이터 목록
     */
    @OneToMany(mappedBy = "metric", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MetricMetadata> metadataList = new ArrayList<>();

    /**
     * 메트릭 태그 목록
     */
    @OneToMany(mappedBy = "metric", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MetricTag> tags = new ArrayList<>();

    /**
     * 테넌트 ID (멀티테넌트 지원)
     * TODO: 테넌트 도메인 구현 후 활성화 예정
     * - TenantAwareRepository 상속 시 자동 테넌트 필터링 지원
     * - 테넌트별 데이터 격리 및 권한 관리
     */
    // @Column(name = "tenant_id")
    // private String tenantId;

    @Builder
    public Metric(String metricName, Double metricValue, String unit, MetricType metricType, 
                  LocalDateTime collectedAt, String source, Status status, String metadata) {
        this.metricName = metricName;
        this.metricValue = metricValue;
        this.unit = unit;
        this.metricType = metricType;
        this.collectedAt = collectedAt;
        this.source = source;
        this.status = status;
        this.metadata = metadata;
    }

    /**
     * 메트릭 값 업데이트
     * 
     * @param metricValue 새로운 메트릭 값
     * @param collectedAt 새로운 수집 시간
     */
    public void updateValue(Double metricValue, LocalDateTime collectedAt) {
        this.metricValue = metricValue;
        this.collectedAt = collectedAt;
    }

    /**
     * 메트릭 상태 업데이트
     * 
     * @param status 새로운 상태
     */
    public void updateStatus(Status status) {
        this.status = status;
    }

    /**
     * 메타데이터 추가
     * 
     * @param metadata 메타데이터
     */
    public void addMetadata(MetricMetadata metadata) {
        this.metadataList.add(metadata);
        metadata.setMetric(this);
    }

    /**
     * 태그 추가
     * 
     * @param tag 태그
     */
    public void addTag(MetricTag tag) {
        this.tags.add(tag);
        tag.setMetric(this);
    }

    /**
     * 메트릭 타입 열거형
     */
    public enum MetricType {
        SYSTEM,        // 시스템 리소스 메트릭 (CPU, 메모리, 디스크)
        APPLICATION    // 애플리케이션 메트릭 (응답시간, 처리량, 에러율)
    }

    /**
     * 메트릭 상태 열거형
     */
    public enum Status {
        ACTIVE,        // 활성
        INACTIVE,      // 비활성
        ARCHIVED       // 아카이브
    }
}
