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
 * 메트릭 태그 엔티티
 * 
 * <p>메트릭에 대한 태그 정보를 저장합니다.
 * 태그는 메트릭을 분류하고 필터링하는 데 사용됩니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Entity
@Table(name = "metric_tags", indexes = {
    @Index(name = "idx_metric_tags_metric_id", columnList = "metric_id"),
    @Index(name = "idx_metric_tags_name", columnList = "name"),
    @Index(name = "idx_metric_tags_name_value", columnList = "name,\"value\""),
    @Index(name = "idx_metric_tags_category", columnList = "category")
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricTag extends BaseEntity {

    /**
     * 연결된 메트릭
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metric_id", nullable = false)
    @NotNull(message = "메트릭은 필수입니다")
    private Metric metric;

    /**
     * 태그 이름 (예: environment, service, team)
     */
    @NotBlank(message = "태그 이름은 필수입니다")
    @Size(max = 100, message = "태그 이름은 100자를 초과할 수 없습니다")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * 태그 값
     */
    @NotBlank(message = "태그 값은 필수입니다")
    @Size(max = 255, message = "태그 값은 255자를 초과할 수 없습니다")
    @Column(name = "\"value\"", nullable = false, length = 255)
    private String value;

    /**
     * 태그 카테고리 (예: infrastructure, application, business)
     */
    @Column(name = "category", length = 50)
    private String category;

    /**
     * 태그 우선순위 (낮을수록 높은 우선순위)
     */
    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 0;

    /**
     * 태그 설명
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 태그 값 업데이트
     * 
     * @param value 새로운 값
     */
    public void updateValue(String value) {
        this.value = value;
    }

    /**
     * 태그 우선순위 업데이트
     * 
     * @param priority 새로운 우선순위
     */
    public void updatePriority(Integer priority) {
        this.priority = priority;
    }

    /**
     * 태그 설명 업데이트
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
