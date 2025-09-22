package com.agenticcp.core.domain.platform.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "platform_configs")
@Data
@Builder
@lombok.EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class PlatformConfig extends BaseEntity {

    @Column(name = "config_key", nullable = false, unique = true)
    private String configKey;

    @Column(name = "config_value", columnDefinition = "TEXT")
    private String configValue;

    @Column(name = "config_type")
    @Enumerated(EnumType.STRING)
    private ConfigType configType;

    @Column(name = "description")
    private String description;

    @Column(name = "is_encrypted")
    @lombok.Builder.Default
    private Boolean isEncrypted = false;

    @Column(name = "is_system")
    @lombok.Builder.Default
    private Boolean isSystem = false;

    // 버전 컬럼은 ERD에 없으므로 사용하지 않음

    public enum ConfigType {
        STRING,
        NUMBER,
        BOOLEAN,
        JSON,
        ENCRYPTED
    }
}
