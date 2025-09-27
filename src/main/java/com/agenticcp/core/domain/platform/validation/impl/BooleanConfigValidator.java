package com.agenticcp.core.domain.platform.validation.impl;

import com.agenticcp.core.domain.platform.entity.PlatformConfig;
import com.agenticcp.core.domain.platform.enums.PlatformConfigErrorCode;
import com.agenticcp.core.domain.platform.exception.ConfigValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * BOOLEAN 타입 설정 검증을 담당하는 구현체입니다.
 * <p>
 * 불린 값의 유효성을 검증하며, 'true' 또는 'false' 문자열만 허용합니다.
 * </p>
 *
 * @author AgenticCP Team
 * @since 2025-09-26
 */
@Slf4j
@Component
public class BooleanConfigValidator extends BaseConfigValidator {

    @Override
    protected void validateValueByType(String configValue, PlatformConfig.ConfigType configType) {
        if (configType != PlatformConfig.ConfigType.BOOLEAN) {
            return;
        }

        log.debug("[BooleanConfigValidator] validateValueByType - BOOLEAN type validation");

        if (configValue == null || configValue.trim().isEmpty()) {
            throw new ConfigValidationException(PlatformConfigErrorCode.CONFIG_VALUE_REQUIRED);
        }

        String trimmedValue = configValue.trim().toLowerCase();
        if (!"true".equals(trimmedValue) && !"false".equals(trimmedValue)) {
            log.warn("[BooleanConfigValidator] Invalid boolean value: {}", configValue);
            throw new ConfigValidationException(PlatformConfigErrorCode.BOOLEAN_VALUE_INVALID);
        }

        log.debug("[BooleanConfigValidator] validateValueByType - success");
    }
}
