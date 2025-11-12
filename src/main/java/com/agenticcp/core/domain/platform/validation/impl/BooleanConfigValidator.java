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

    /**
     * BOOLEAN 타입 설정 값 검증
     * <p>
     * 설정 값이 유효한 불린 형식('true' 또는 'false')인지 검증합니다.
     * 대소문자를 구분하지 않으며, 공백은 자동으로 제거됩니다.
     * </p>
     *
     * @param configValue 검증할 설정 값
     * @param configType 설정 타입 (BOOLEAN이 아니면 검증하지 않음)
     * @throws ConfigValidationException 설정 값이 null이거나 빈 문자열이거나 'true'/'false'가 아닌 경우
     */
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
