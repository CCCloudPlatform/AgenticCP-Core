package com.agenticcp.core.domain.monitoring.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 테넌트 수집기 설정 메타데이터 엔티티
 * 
 * 수집기별 추가 설정 정보를 저장합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Entity
@Table(name = "tenant_collector_metadata", indexes = {
    @Index(name = "idx_tenant_collector_metadata_config", columnList = "tenant_collector_config_id"),
    @Index(name = "idx_tenant_collector_metadata_key", columnList = "metadata_key")
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, exclude = {"id", "createdAt", "updatedAt"})
@ToString(callSuper = true)
public class TenantCollectorMetadata extends BaseEntity {

    /**
     * 테넌트 수집기 설정
     */
    @NotNull(message = "테넌트 수집기 설정은 필수입니다")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_collector_config_id", nullable = false)
    private TenantCollectorConfig tenantCollectorConfig;

    /**
     * 메타데이터 키
     */
    @NotBlank(message = "메타데이터 키는 필수입니다")
    @Size(max = 100, message = "메타데이터 키는 100자를 초과할 수 없습니다")
    @Column(name = "metadata_key", nullable = false, length = 100)
    private String metadataKey;

    /**
     * 메타데이터 값
     */
    @Column(name = "metadata_value", columnDefinition = "TEXT")
    private String metadataValue;

    /**
     * 메타데이터 타입
     */
    @Column(name = "metadata_type", length = 20)
    @Builder.Default
    private String metadataType = "STRING";

    /**
     * 설명
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 메타데이터 값 업데이트
     */
    public void updateValue(String value) {
        this.metadataValue = value;
    }

    /**
     * 메타데이터 타입 업데이트
     */
    public void updateType(String type) {
        this.metadataType = type;
    }

    /**
     * 설명 업데이트
     */
    public void updateDescription(String description) {
        this.description = description;
    }

    /**
     * 테넌트 수집기 설정 설정
     */
    public void setTenantCollectorConfig(TenantCollectorConfig tenantCollectorConfig) {
        this.tenantCollectorConfig = tenantCollectorConfig;
    }
}
