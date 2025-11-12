package com.agenticcp.core.domain.platform.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 플랫폼 설정 엔티티
 * <p>
 * 플랫폼 전역 설정을 저장하는 엔티티입니다. 설정 키는 고유해야 하며,
 * 네임스페이스 기반으로 시스템 설정(system.*)과 사용자 설정(user.*)을 구분합니다.
 * ENCRYPTED 타입의 설정은 자동으로 암호화되어 저장됩니다.
 * </p>
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Entity
@Table(name = "platform_configs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformConfig extends BaseEntity {

    /**
     * 설정 키
     * <p>
     * 플랫폼 설정을 식별하는 고유한 키입니다.
     * 네임스페이스 규칙: system.* 또는 user.*로 시작해야 합니다.
     * 형식: 영문자로 시작하고 영문자, 숫자, 점(.), 언더스코어(_), 하이픈(-)을 포함할 수 있습니다.
     * 길이: 3자 이상 255자 이하
     * </p>
     */
    @Column(name = "config_key", nullable = false, unique = true)
    private String configKey;

    /**
     * 설정 값
     * <p>
     * 설정의 실제 값입니다. configType에 따라 검증이 수행됩니다.
     * ENCRYPTED 타입인 경우 암호화되어 저장됩니다.
     * </p>
     */
    @Column(name = "config_value", columnDefinition = "TEXT")
    private String configValue;

    /**
     * 설정 타입
     * <p>
     * 설정 값의 타입을 나타냅니다. 타입에 따라 값 검증이 수행됩니다.
     * 시스템 설정의 타입은 변경할 수 없습니다.
     * </p>
     *
     * @see ConfigType
     */
    @Column(name = "config_type")
    @Enumerated(EnumType.STRING)
    private ConfigType configType;

    /**
     * 설정 설명
     * <p>
     * 설정의 용도나 설명을 기록하는 필드입니다.
     * 감사 로그에 변경 사유로 사용될 수 있습니다.
     * </p>
     */
    @Column(name = "description")
    private String description;

    /**
     * 암호화 여부
     * <p>
     * 설정 값이 암호화되어 저장되었는지 여부를 나타냅니다.
     * ENCRYPTED 타입인 경우 자동으로 true로 설정됩니다.
     * 기본값: false
     * </p>
     */
    @Column(name = "is_encrypted")
    private Boolean isEncrypted = false;

    /**
     * 시스템 설정 여부
     * <p>
     * 시스템 설정인지 사용자 설정인지를 나타냅니다.
     * configKey가 system.*로 시작하면 자동으로 true로 설정됩니다.
     * 시스템 설정은 타입 변경 및 삭제가 금지됩니다.
     * </p>
     */
    @Column(name = "is_system")
    private Boolean isSystem;

    /**
     * 설정 타입 열거형
     * <p>
     * 플랫폼 설정 값의 타입을 정의합니다. 각 타입에 따라 값 검증이 수행됩니다.
     * </p>
     */
    public enum ConfigType {
        /**
         * 문자열 타입
         * <p>
         * 일반 문자열 값을 저장합니다. 빈 문자열은 허용되지 않습니다.
         * </p>
         */
        STRING,

        /**
         * 숫자 타입
         * <p>
         * 정수 또는 실수 값을 저장합니다. BigInteger, BigDecimal을 포함한 모든 숫자 형식을 지원합니다.
         * </p>
         */
        NUMBER,

        /**
         * 불린 타입
         * <p>
         * true 또는 false 값을 저장합니다.
         * </p>
         */
        BOOLEAN,

        /**
         * JSON 타입
         * <p>
         * 유효한 JSON 형식의 문자열을 저장합니다. JSON 파싱 검증이 수행됩니다.
         * </p>
         */
        JSON,

        /**
         * 암호화 타입
         * <p>
         * 민감한 정보를 저장하는 타입입니다. 저장 시 자동으로 암호화되며,
         * 조회 시 showSecret=true인 경우에만 복호화되어 반환됩니다.
         * </p>
         */
        ENCRYPTED
    }
}
