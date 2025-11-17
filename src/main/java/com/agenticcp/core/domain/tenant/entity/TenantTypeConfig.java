package com.agenticcp.core.domain.tenant.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 테넌트 타입별 기본 설정 엔티티
 * 
 * 테넌트 타입(ENTERPRISE, STANDARD, TRIAL)에 따른 기본 설정값을 저장합니다.
 * 이 설정은 해당 타입의 모든 테넌트에 적용되는 기본값으로 사용됩니다.
 * 개별 테넌트는 이 값을 오버라이드할 수 있습니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-23
 */
@Entity
@Table(name = "tenant_type_configs",
       uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_type", "config_key"}),
       indexes = {
           @Index(name = "idx_tenant_type_configs_type", columnList = "tenant_type"),
           @Index(name = "idx_tenant_type_configs_key", columnList = "config_key"),
           @Index(name = "idx_tenant_type_configs_config_type", columnList = "config_type")
       })
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantTypeConfig extends BaseEntity {

    @NotNull(message = "테넌트 타입은 필수입니다")
    @Column(name = "tenant_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Tenant.TenantType tenantType;

    @NotBlank(message = "설정 키는 필수입니다")
    @Size(min = 1, max = 100, message = "설정 키는 1-100자 사이여야 합니다")
    @Column(name = "config_key", nullable = false, length = 100)
    private String configKey;

    @Column(name = "config_value", columnDefinition = "TEXT")
    private String configValue;

    @NotNull(message = "설정 타입은 필수입니다")
    @Column(name = "config_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ConfigType configType;

    @Size(max = 500, message = "설명은 500자를 초과할 수 없습니다")
    @Column(name = "description", length = 500)
    private String description;

    @Builder.Default
    @Column(name = "is_encrypted", nullable = false)
    private Boolean isEncrypted = false;

    /**
     * 설정 값의 데이터 타입
     * 
     * @author AgenticCP Team
     * @version 1.0.0
     * @since 2025-10-23
     */
    public enum ConfigType {
        /**
         * 문자열 타입
         */
        STRING,
        
        /**
         * 숫자 타입 (정수, 실수)
         */
        NUMBER,
        
        /**
         * 불리언 타입 (true/false)
         */
        BOOLEAN,
        
        /**
         * JSON 객체 또는 배열
         */
        JSON,
        
        /**
         * 암호화된 값
         */
        ENCRYPTED
    }

    // === 비즈니스 메서드 ===

    /**
     * 설정 값이 암호화되어 있는지 확인
     * 
     * @return 암호화 여부
     */
    public boolean isEncrypted() {
        return Boolean.TRUE.equals(this.isEncrypted);
    }

    /**
     * 설정을 암호화 상태로 변경
     */
    public void markAsEncrypted() {
        this.isEncrypted = true;
    }

    /**
     * 설정을 복호화 상태로 변경
     */
    public void markAsDecrypted() {
        this.isEncrypted = false;
    }

    /**
     * 설정 값이 비어있는지 확인
     * 
     * @return 값이 null이거나 빈 문자열인 경우 true
     */
    public boolean hasValue() {
        return configValue != null && !configValue.trim().isEmpty();
    }

    /**
     * 특정 타입인지 확인
     * 
     * @param type 확인할 타입
     * @return 동일한 타입이면 true
     */
    public boolean isType(ConfigType type) {
        return this.configType == type;
    }

    /**
     * 특정 테넌트 타입에 대한 설정인지 확인
     * 
     * @param type 확인할 테넌트 타입
     * @return 동일한 테넌트 타입이면 true
     */
    public boolean isForTenantType(Tenant.TenantType type) {
        return this.tenantType == type;
    }
}
