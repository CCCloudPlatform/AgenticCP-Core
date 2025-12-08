package com.agenticcp.core.domain.monitoring.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
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
 * @since 2025-11-13
 */
@Entity
@Table(name = "metric_metadata", indexes = {
    @Index(name = "idx_metric_metadata_metric_id", columnList = "metric_id"),
    @Index(name = "idx_metric_metadata_key", columnList = "`key`")
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricMetadata extends BaseEntity {

    /**
     * 연결된 메트릭
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metric_id", nullable = false)
    @NotNull(message = "메트릭은 필수입니다")
    private Metric metric;

    /**
     * 메타데이터 키 (예: hostname, region, instance_type)
     */
    @NotBlank(message = "메타데이터 키는 필수입니다")
    @Size(max = 100, message = "메타데이터 키는 100자를 초과할 수 없습니다")
    @Column(name = "`key`", nullable = false, length = 100)
    private String key;

    /**
     * 메타데이터 값
     */
    @Column(name = "`value`", columnDefinition = "TEXT")
    private String value;

    /**
     * 데이터 타입 (string, number, boolean, json)
     */
    @Column(name = "data_type", length = 50)
    @Builder.Default
    private String dataType = "string";

    /**
     * 메타데이터 설명
     */
    @Column(name = "description", length = 500)
    private String description;

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
