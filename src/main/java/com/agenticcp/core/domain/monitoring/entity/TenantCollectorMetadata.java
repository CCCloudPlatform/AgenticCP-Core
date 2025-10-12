package com.agenticcp.core.domain.monitoring.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 테넌트 수집기 설정 메타데이터 엔티티
 * 
 * 수집기별 추가 설정 정보를 저장합니다.
 */
@Entity
@Table(name = "tenant_collector_metadata", indexes = {
    @Index(name = "idx_tenant_collector_metadata_config", columnList = "tenant_collector_config_id"),
    @Index(name = "idx_tenant_collector_metadata_key", columnList = "metadata_key")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(callSuper = false, exclude = {"id", "createdAt", "updatedAt"})
@ToString(callSuper = true)
public class TenantCollectorMetadata extends BaseEntity {

    /**
     * 테넌트 수집기 설정
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_collector_config_id", nullable = false)
    private TenantCollectorConfig tenantCollectorConfig;

    /**
     * 메타데이터 키
     */
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
    private String metadataType = "STRING";

    /**
     * 설명
     */
    @Column(name = "description", length = 500)
    private String description;

    @Builder
    public TenantCollectorMetadata(TenantCollectorConfig tenantCollectorConfig, String metadataKey,
                                  String metadataValue, String metadataType, String description) {
        this.tenantCollectorConfig = tenantCollectorConfig;
        this.metadataKey = metadataKey;
        this.metadataValue = metadataValue;
        this.metadataType = metadataType;
        this.description = description;
    }

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
