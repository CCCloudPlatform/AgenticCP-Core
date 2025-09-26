package com.agenticcp.core.domain.platform.validation.impl;

import com.agenticcp.core.domain.platform.entity.PlatformConfig;
import com.agenticcp.core.domain.platform.enums.PlatformConfigErrorCode;
import com.agenticcp.core.domain.platform.exception.ConfigValidationException;
import com.agenticcp.core.domain.platform.validation.ConfigValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * 플랫폼 설정 검증의 기본 구현체입니다.
 * <p>
 * 공통적인 검증 로직(키 검증, 기본값 검증 등)을 제공하며,
 * 타입별 특화 검증은 하위 클래스에서 구현합니다.
 * </p>
 *
 * @author AgenticCP Team
 * @since 2025-09-26
 */
@Slf4j
public abstract class BaseConfigValidator implements ConfigValidator {

    private static final int MIN_KEY_LENGTH = 3;
    private static final int MAX_KEY_LENGTH = 255;
    private static final String KEY_PATTERN = "^[a-zA-Z][a-zA-Z0-9._-]*$";

    @Override
    public void validateKey(String configKey) {
        log.debug("[BaseConfigValidator] validateKey - configKey={}", configKey);

        if (!StringUtils.hasText(configKey)) {
            throw new ConfigValidationException(PlatformConfigErrorCode.CONFIG_KEY_REQUIRED);
        }

        if (configKey.length() < MIN_KEY_LENGTH) {
            throw new ConfigValidationException(PlatformConfigErrorCode.CONFIG_KEY_TOO_SHORT);
        }

        if (configKey.length() > MAX_KEY_LENGTH) {
            throw new ConfigValidationException(PlatformConfigErrorCode.CONFIG_KEY_TOO_LONG);
        }

        if (!configKey.matches(KEY_PATTERN)) {
            throw new ConfigValidationException(PlatformConfigErrorCode.CONFIG_KEY_INVALID_FORMAT);
        }

        log.debug("[BaseConfigValidator] validateKey - success");
    }

    @Override
    public void validateValue(String configValue, PlatformConfig.ConfigType configType) {
        log.debug("[BaseConfigValidator] validateValue - configType={}", configType);

        if (!StringUtils.hasText(configValue)) {
            throw new ConfigValidationException(PlatformConfigErrorCode.CONFIG_VALUE_REQUIRED);
        }

        // 타입별 특화 검증은 하위 클래스에서 구현
        validateValueByType(configValue, configType);

        log.debug("[BaseConfigValidator] validateValue - success");
    }

    @Override
    public void validate(PlatformConfig platformConfig) {
        log.debug("[BaseConfigValidator] validate - configKey={}", platformConfig.getConfigKey());

        validateKey(platformConfig.getConfigKey());
        validateValue(platformConfig.getConfigValue(), platformConfig.getConfigType());

        log.debug("[BaseConfigValidator] validate - success");
    }

    /**
     * 설정 타입별 특화 검증을 수행합니다.
     * 하위 클래스에서 구현해야 합니다.
     *
     * @param configValue 검증할 설정 값
     * @param configType 설정 타입
     */
    protected abstract void validateValueByType(String configValue, PlatformConfig.ConfigType configType);
}
