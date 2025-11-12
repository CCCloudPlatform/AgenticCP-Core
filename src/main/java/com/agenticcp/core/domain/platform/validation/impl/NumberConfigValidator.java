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

    /**
     * NUMBER 타입 설정 값 검증
     * <p>
     * 설정 값이 유효한 숫자 형식인지 검증합니다.
     * 먼저 정수(BigInteger)로 파싱을 시도하고, 실패하면 실수(BigDecimal)로 파싱을 시도합니다.
     * 둘 다 실패하면 ConfigValidationException을 발생시킵니다.
     * </p>
     *
     * @param configValue 검증할 설정 값
     * @param configType 설정 타입 (NUMBER가 아니면 검증하지 않음)
     * @throws ConfigValidationException 설정 값이 null이거나 빈 문자열이거나 유효한 숫자 형식이 아닌 경우
     */
    @Override
    protected void validateValueByType(String configValue, PlatformConfig.ConfigType configType) {
        if (configType != PlatformConfig.ConfigType.NUMBER) {
            return;
        }

        log.debug("[NumberConfigValidator] validateValueByType - NUMBER type validation");

        // BaseConfigValidator에서 이미 null 체크를 수행하므로, 여기서는 빈 문자열만 체크
        if (configValue.trim().isEmpty()) {
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
