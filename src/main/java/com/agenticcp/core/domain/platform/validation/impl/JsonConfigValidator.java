package com.agenticcp.core.domain.platform.validation.impl;

import com.agenticcp.core.domain.platform.entity.PlatformConfig;
import com.agenticcp.core.domain.platform.enums.PlatformConfigErrorCode;
import com.agenticcp.core.domain.platform.exception.ConfigValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class JsonConfigValidator extends BaseConfigValidator {

    private final ObjectMapper objectMapper;

    public JsonConfigValidator() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    protected void validateValueByType(String configValue, PlatformConfig.ConfigType configType) {
        if (configType != PlatformConfig.ConfigType.JSON) {
            return;
        }

        log.debug("[JsonConfigValidator] validateValueByType - JSON type validation");

        try {
            objectMapper.readTree(configValue.trim());
        } catch (Exception e) {
            log.warn("[JsonConfigValidator] Invalid JSON format: {}", configValue, e);
            throw new ConfigValidationException(PlatformConfigErrorCode.JSON_VALUE_INVALID_FORMAT);
        }

        log.debug("[JsonConfigValidator] validateValueByType - success");
    }
}
