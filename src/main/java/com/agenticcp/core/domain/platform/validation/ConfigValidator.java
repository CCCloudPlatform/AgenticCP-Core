package com.agenticcp.core.domain.platform.validation;

import com.agenticcp.core.domain.platform.entity.PlatformConfig;

/**
 * 플랫폼 설정 검증을 위한 인터페이스입니다.
 * <p>
 * 각 설정 타입별로 특화된 검증 로직을 구현하며,
 * 설정 키와 값의 유효성을 검증합니다.
 * </p>
 *
 * @author AgenticCP Team
 * @since 2025-09-26
 */
public interface ConfigValidator {

    /**
     * 설정 키의 유효성을 검증합니다.
     *
     * @param configKey 검증할 설정 키
     * @throws com.agenticcp.core.domain.platform.exception.ConfigValidationException 검증 실패 시
     */
    void validateKey(String configKey);

    /**
     * 설정 값의 유효성을 검증합니다.
     *
     * @param configValue 검증할 설정 값
     * @param configType 설정 타입
     * @throws com.agenticcp.core.domain.platform.exception.ConfigValidationException 검증 실패 시
     */
    void validateValue(String configValue, PlatformConfig.ConfigType configType);

    /**
     * 전체 설정 객체의 유효성을 검증합니다.
     *
     * @param platformConfig 검증할 설정 객체
     * @throws com.agenticcp.core.domain.platform.exception.ConfigValidationException 검증 실패 시
     */
    void validate(PlatformConfig platformConfig);
}
