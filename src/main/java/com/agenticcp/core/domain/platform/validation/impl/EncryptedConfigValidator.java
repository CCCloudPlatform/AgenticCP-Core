package com.agenticcp.core.domain.platform.validation.impl;

import com.agenticcp.core.domain.platform.entity.PlatformConfig;
import com.agenticcp.core.domain.platform.enums.PlatformConfigErrorCode;
import com.agenticcp.core.domain.platform.exception.ConfigValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * ENCRYPTED 타입 설정 검증을 담당하는 구현체입니다.
 * <p>
 * 암호화된 값의 유효성을 검증하며, 빈 값이나 공백만 있는 값을 허용하지 않습니다.
 * 실제 암호화 검증은 별도의 암호화 서비스에서 수행됩니다.
 * </p>
 *
 * @author AgenticCP Team
 * @since 2025-09-26
 */
@Slf4j
@Component
public class EncryptedConfigValidator extends BaseConfigValidator {

    /**
     * ENCRYPTED 타입 설정 값 검증
     * <p>
     * 설정 값이 null이 아니고 빈 문자열이 아닌지 검증합니다.
     * 실제 암호화 형식 검증은 별도의 암호화 서비스에서 수행되며,
     * 이 검증기는 기본적인 값 존재 여부만 확인합니다.
     * </p>
     *
     * @param configValue 검증할 설정 값
     * @param configType 설정 타입 (ENCRYPTED가 아니면 검증하지 않음)
     * @throws ConfigValidationException 설정 값이 null이거나 빈 문자열(공백만 포함)인 경우
     */
    @Override
    protected void validateValueByType(String configValue, PlatformConfig.ConfigType configType) {
        if (configType != PlatformConfig.ConfigType.ENCRYPTED) {
            return;
        }

        log.debug("[EncryptedConfigValidator] validateValueByType - ENCRYPTED type validation");

        if (configValue == null || configValue.trim().isEmpty()) {
            throw new ConfigValidationException(PlatformConfigErrorCode.ENCRYPTED_VALUE_EMPTY);
        }

        // TODO: 향후 실제 암호화 형식 검증 로직 추가 가능
        // 예: Base64 인코딩 검증, 특정 암호화 알고리즘 형식 검증 등

        log.debug("[EncryptedConfigValidator] validateValueByType - success");
    }
}
