package com.agenticcp.core.domain.tenant.entity;

import com.agenticcp.core.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tenant_configs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantConfig extends TenantAwareEntity {

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
    private Boolean isEncrypted = false;

    @Column(name = "is_required")
    private Boolean isRequired = false;

    @Column(name = "default_value", columnDefinition = "TEXT")
    private String defaultValue;

    public enum ConfigType {
        STRING,
        NUMBER,
        BOOLEAN,
        JSON,
        ENCRYPTED,
        URL,
        EMAIL
    }
}
