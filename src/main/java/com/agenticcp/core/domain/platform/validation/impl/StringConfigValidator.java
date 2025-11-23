package com.agenticcp.core.domain.platform.validation.impl;

import com.agenticcp.core.domain.platform.entity.PlatformConfig;
import com.agenticcp.core.domain.platform.enums.PlatformConfigErrorCode;
import com.agenticcp.core.domain.platform.exception.ConfigValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * STRING 타입 설정 검증을 담당하는 구현체입니다.
 * <p>
 * 문자열 값의 유효성을 검증하며, 빈 문자열이나 공백만 있는 문자열을 허용하지 않습니다.
 * </p>
 *
 * @author AgenticCP Team
 * @since 2025-09-26
 */
@Slf4j
@Component
public class StringConfigValidator extends BaseConfigValidator {

    @Override
    protected void validateValueByType(String configValue, PlatformConfig.ConfigType configType) {
        if (configType != PlatformConfig.ConfigType.STRING) {
            return;
        }

        log.debug("[StringConfigValidator] validateValueByType - STRING type validation");

        // BaseConfigValidator에서 이미 null 체크를 수행하므로, 여기서는 빈 문자열만 체크
        if (configValue.trim().isEmpty()) {
            throw new ConfigValidationException(PlatformConfigErrorCode.STRING_VALUE_EMPTY);
        }

        log.debug("[StringConfigValidator] validateValueByType - success");
    }
}
