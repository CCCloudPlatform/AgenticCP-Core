package com.agenticcp.core.domain.tenant.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 테넌트 타입별 기본 설정 엔티티
 * 테넌트 타입에 따른 기본 설정값을 저장합니다.
 */
@Entity
@Table(name = "tenant_type_configs",
       uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_type", "config_key"}))
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantTypeConfig extends BaseEntity {

    @Column(name = "tenant_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private Tenant.TenantType tenantType;

    @Column(name = "config_key", nullable = false)
    private String configKey;

    @Column(name = "config_value", columnDefinition = "TEXT")
    private String configValue;

    @Column(name = "config_type")
    @Enumerated(EnumType.STRING)
    private ConfigType configType;

    @Column(name = "description")
    private String description;

    @Column(name = "is_encrypted")
    @Builder.Default
    private Boolean isEncrypted = false;

    public enum ConfigType {
        STRING,
        NUMBER,
        BOOLEAN,
        JSON,
        ENCRYPTED
    }
}
