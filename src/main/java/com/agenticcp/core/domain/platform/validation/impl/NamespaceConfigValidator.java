package com.agenticcp.core.domain.platform.validation.impl;

import com.agenticcp.core.domain.platform.entity.PlatformConfig;
import com.agenticcp.core.domain.platform.enums.PlatformConfigErrorCode;
import com.agenticcp.core.domain.platform.exception.ConfigValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 플랫폼 설정 네임스페이스 검증기입니다.
 * <p>
 * 설정 키의 네임스페이스(system.*, user.*)와 isSystem 플래그의 일치성을 검증합니다.
 * </p>
 *
 * @author AgenticCP Team
 * @since 2025-10-19
 */
@Slf4j
@Component
public class NamespaceConfigValidator extends BaseConfigValidator {

    private static final String SYSTEM_NAMESPACE_PATTERN = "^system\\..*";
    private static final String USER_NAMESPACE_PATTERN = "^user\\..*";

    @Override
    public void validate(PlatformConfig platformConfig) {
        log.debug("[NamespaceConfigValidator] validate - configKey={}", platformConfig.getConfigKey());

        // 기본 검증 수행
        validateKey(platformConfig.getConfigKey());
        validateValue(platformConfig.getConfigValue(), platformConfig.getConfigType());
        
        // 네임스페이스 검증 수행
        validateNamespace(platformConfig);

        log.debug("[NamespaceConfigValidator] validate - success");
    }

    /**
     * 설정 키의 네임스페이스와 isSystem 플래그의 일치성을 검증합니다.
     *
     * @param platformConfig 검증할 설정 객체
     * @throws ConfigValidationException 네임스페이스 불일치 시
     */
    private void validateNamespace(PlatformConfig platformConfig) {
        String configKey = platformConfig.getConfigKey();
        Boolean isSystem = platformConfig.getIsSystem();

        log.debug("[NamespaceConfigValidator] validateNamespace - configKey={}, isSystem={}", 
                configKey, isSystem);

        // 네임스페이스 패턴 매칭
        boolean isSystemKey = configKey.matches(SYSTEM_NAMESPACE_PATTERN);
        boolean isUserKey = configKey.matches(USER_NAMESPACE_PATTERN);

        // 유효하지 않은 네임스페이스 검증
        if (!isSystemKey && !isUserKey) {
            log.warn("[NamespaceConfigValidator] validateNamespace - invalid namespace: {}", configKey);
            throw new ConfigValidationException(PlatformConfigErrorCode.INVALID_CONFIG_NAMESPACE);
        }

        // isSystem 플래그가 null인 경우는 자동 설정 로직에서 처리하므로 여기서는 검증하지 않음
        if (isSystem == null) {
            log.debug("[NamespaceConfigValidator] validateNamespace - isSystem is null, skipping validation");
            return;
        }

        // 네임스페이스와 isSystem 플래그 일치성 검증
        if (Boolean.TRUE.equals(isSystem) && !isSystemKey) {
            log.warn("[NamespaceConfigValidator] validateNamespace - system config with non-system namespace: {}", configKey);
            throw new ConfigValidationException(PlatformConfigErrorCode.SYSTEM_CONFIG_NAMESPACE_MISMATCH);
        }

        if (Boolean.FALSE.equals(isSystem) && isSystemKey) {
            log.warn("[NamespaceConfigValidator] validateNamespace - user config with system namespace: {}", configKey);
            throw new ConfigValidationException(PlatformConfigErrorCode.USER_CONFIG_NAMESPACE_MISMATCH);
        }

        log.debug("[NamespaceConfigValidator] validateNamespace - success");
    }

    @Override
    protected void validateValueByType(String configValue, PlatformConfig.ConfigType configType) {
        // 네임스페이스 검증기는 값 검증을 BaseConfigValidator에 위임
        // 타입별 특화 검증은 다른 검증기에서 처리
        log.debug("[NamespaceConfigValidator] validateValueByType - delegating to base validator");
    }
}
