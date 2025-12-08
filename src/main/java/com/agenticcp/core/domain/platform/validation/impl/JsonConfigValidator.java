package com.agenticcp.core.domain.platform.validation.impl;

import com.agenticcp.core.domain.platform.entity.PlatformConfig;
import com.agenticcp.core.domain.platform.enums.PlatformConfigErrorCode;
import com.agenticcp.core.domain.platform.exception.ConfigValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * JSON 타입 설정 검증을 담당하는 구현체입니다.
 * <p>
 * JSON 값의 유효성을 검증하며, 유효한 JSON 형식인지 확인합니다.
 * </p>
 *
 * @author AgenticCP Team
 * @since 2025-09-26
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JsonConfigValidator extends BaseConfigValidator {

    private final ObjectMapper objectMapper;

    /**
     * JSON 타입 설정 값 검증
     * <p>
     * 설정 값이 유효한 JSON 형식인지 검증합니다.
     * ObjectMapper를 사용하여 JSON 파싱을 시도하고, 파싱에 실패하면 예외를 발생시킵니다.
     * 공백은 자동으로 제거된 후 검증됩니다.
     * </p>
     *
     * @param configValue 검증할 설정 값
     * @param configType 설정 타입 (JSON이 아니면 검증하지 않음)
     * @throws ConfigValidationException 설정 값이 null이거나 빈 문자열이거나 유효한 JSON 형식이 아닌 경우
     */
    @Override
    protected void validateValueByType(String configValue, PlatformConfig.ConfigType configType) {
        if (configType != PlatformConfig.ConfigType.JSON) {
            return;
        }

        log.debug("[JsonConfigValidator] validateValueByType - JSON type validation");

        // BaseConfigValidator에서 이미 null 체크를 수행하므로, 여기서는 빈 문자열만 체크
        if (configValue.trim().isEmpty()) {
            throw new ConfigValidationException(PlatformConfigErrorCode.CONFIG_VALUE_REQUIRED);
        }

        try {
            objectMapper.readTree(configValue.trim());
        } catch (Exception e) {
            log.warn("[JsonConfigValidator] Invalid JSON format: {}", configValue, e);
            throw new ConfigValidationException(PlatformConfigErrorCode.JSON_VALUE_INVALID_FORMAT);
        }

        log.debug("[JsonConfigValidator] validateValueByType - success");
    }
}
