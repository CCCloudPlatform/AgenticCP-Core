package com.agenticcp.core.domain.monitoring.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 메트릭 메타데이터 엔티티
 * 
 * <p>메트릭에 대한 추가적인 메타데이터 정보를 저장합니다.
 * 예: 호스트명, 리전, 인스턴스 타입, 태그 등</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Entity
@Table(name = "metric_metadata", indexes = {
    @Index(name = "idx_metric_metadata_metric_id", columnList = "metric_id"),
    @Index(name = "idx_metric_metadata_key", columnList = "key"),
    @Index(name = "idx_metric_metadata_key_value", columnList = "key, value")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MetricMetadata extends BaseEntity {

    /**
     * 연결된 메트릭
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metric_id", nullable = false)
    private Metric metric;

    /**
     * 메타데이터 키 (예: hostname, region, instance_type)
     */
    @Column(name = "key", nullable = false, length = 100)
    private String key;

    /**
     * 메타데이터 값
     */
    @Column(name = "value", columnDefinition = "TEXT")
    private String value;

    /**
     * 데이터 타입 (string, number, boolean, json)
     */
    @Column(name = "data_type", length = 50)
    private String dataType = "string";

    /**
     * 메타데이터 설명
     */
    @Column(name = "description", length = 500)
    private String description;

    @Builder
    public MetricMetadata(Metric metric, String key, String value, String dataType, String description) {
        this.metric = metric;
        this.key = key;
        this.value = value;
        this.dataType = dataType;
        this.description = description;
    }

    /**
     * 메타데이터 값 업데이트
     * 
     * @param value 새로운 값
     * @param dataType 데이터 타입
     */
    public void updateValue(String value, String dataType) {
        this.value = value;
        this.dataType = dataType;
    }

    /**
     * 메타데이터 설명 업데이트
     * 
     * @param description 새로운 설명
     */
    public void updateDescription(String description) {
        this.description = description;
    }

    /**
     * 연결된 메트릭 설정
     * 
     * @param metric 메트릭
     */
    public void setMetric(Metric metric) {
        this.metric = metric;
    }
}
