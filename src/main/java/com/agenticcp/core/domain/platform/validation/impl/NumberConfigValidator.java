package com.agenticcp.core.domain.platform.validation.impl;

import com.agenticcp.core.domain.platform.entity.PlatformConfig;
import com.agenticcp.core.domain.platform.enums.PlatformConfigErrorCode;
import com.agenticcp.core.domain.platform.exception.ConfigValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * NUMBER 타입 설정 검증을 담당하는 구현체입니다.
 * <p>
 * 숫자 값의 유효성을 검증하며, 정수, 실수, BigInteger, BigDecimal 등을 지원합니다.
 * </p>
 *
 * @author AgenticCP Team
 * @since 2025-09-26
 */
@Slf4j
@Component
public class NumberConfigValidator extends BaseConfigValidator {

    @Override
    protected void validateValueByType(String configValue, PlatformConfig.ConfigType configType) {
        if (configType != PlatformConfig.ConfigType.NUMBER) {
            return;
        }

        log.debug("[NumberConfigValidator] validateValueByType - NUMBER type validation");

        if (configValue == null || configValue.trim().isEmpty()) {
            throw new ConfigValidationException(PlatformConfigErrorCode.CONFIG_VALUE_REQUIRED);
        }

        try {
            // 정수 검증
            new BigInteger(configValue.trim());
        } catch (NumberFormatException e1) {
            try {
                // 실수 검증
                new BigDecimal(configValue.trim());
            } catch (NumberFormatException e2) {
                log.warn("[NumberConfigValidator] Invalid number format: {}", configValue);
                throw new ConfigValidationException(PlatformConfigErrorCode.NUMBER_VALUE_INVALID_FORMAT);
            }
        }

        log.debug("[NumberConfigValidator] validateValueByType - success");
    }
}
