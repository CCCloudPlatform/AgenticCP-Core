package com.agenticcp.core.domain.monitoring.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 메트릭 데이터를 저장하는 엔티티
 * 시스템 리소스 메트릭(CPU, 메모리, 디스크)과 애플리케이션 메트릭을 저장
 */
@Entity
@Table(name = "metrics", indexes = {
    @Index(name = "idx_metrics_name", columnList = "metric_name"),
    @Index(name = "idx_metrics_type", columnList = "metric_type"),
    @Index(name = "idx_metrics_collected_at", columnList = "collected_at")
    // TODO: 테넌트 도메인 구현 후 활성화 예정
    // @Index(name = "idx_metrics_tenant_id", columnList = "tenant_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Metric extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
     * 추가 메타데이터 (JSON 형태로 저장)
     * 예: {"host": "server1", "region": "us-east-1", "instance_type": "t3.medium"}
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

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
                  LocalDateTime collectedAt, String metadata) { // Removed tenantId from constructor
        this.metricName = metricName;
        this.metricValue = metricValue;
        this.unit = unit;
        this.metricType = metricType;
        this.collectedAt = collectedAt;
        this.metadata = metadata;
        // TODO: 테넌트 도메인 구현 후 활성화 예정
        // this.tenantId = tenantId;
    }

    /**
     * 메트릭 타입 열거형
     */
    public enum MetricType {
        SYSTEM,        // 시스템 리소스 메트릭 (CPU, 메모리, 디스크)
        APPLICATION    // 애플리케이션 메트릭 (응답시간, 처리량, 에러율)
    }
}
