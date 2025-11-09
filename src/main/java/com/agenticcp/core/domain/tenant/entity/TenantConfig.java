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
 * 테넌트별 설정 엔티티
 * 
 * 개별 테넌트의 설정값을 저장하고 관리합니다.
 * 플랫폼 설정과 테넌트 타입 설정을 상속받아 오버라이드할 수 있습니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-23
 */
@Entity
@Table(name = "tenant_configs", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "config_key"}))
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantConfig extends BaseEntity {

    /**
     * 설정이 속한 테넌트
     */
    @NotNull(message = "테넌트는 필수입니다")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    /**
     * 설정 키 (예: max_users, storage_limit)
     */
    @NotBlank(message = "설정 키는 필수입니다")
    @Size(min = 1, max = 100, message = "설정 키는 1-100자 사이여야 합니다")
    @Column(name = "config_key", nullable = false, length = 100)
    private String configKey;

    /**
     * 설정 값 (JSON, 문자열, 숫자 등 다양한 형식 지원)
     */
    @Column(name = "config_value", columnDefinition = "TEXT")
    private String configValue;

    /**
     * 설정 값의 타입
     */
    @NotNull(message = "설정 타입은 필수입니다")
    @Column(name = "config_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ConfigType configType;

    /**
     * 설정에 대한 설명
     */
    @Size(max = 500, message = "설명은 500자를 초과할 수 없습니다")
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 값이 암호화되어 저장되었는지 여부
     */
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
}