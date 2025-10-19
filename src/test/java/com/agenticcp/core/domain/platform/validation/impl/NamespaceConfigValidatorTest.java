package com.agenticcp.core.domain.platform.validation.impl;

import com.agenticcp.core.domain.platform.entity.PlatformConfig;
import com.agenticcp.core.domain.platform.enums.PlatformConfigErrorCode;
import com.agenticcp.core.domain.platform.exception.ConfigValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NamespaceConfigValidator 단위 테스트
 *
 * @author AgenticCP Team
 * @since 2025-01-16
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NamespaceConfigValidator 테스트")
class NamespaceConfigValidatorTest {

    private NamespaceConfigValidator validator;

    @BeforeEach
    void setUp() {
        validator = new NamespaceConfigValidator();
    }

    @Test
    @DisplayName("시스템 설정 네임스페이스 검증 성공")
    void shouldValidateSystemNamespace() {
        // Given
        PlatformConfig systemConfig = PlatformConfig.builder()
                .configKey("system.database.url")
                .isSystem(true)
                .configType(PlatformConfig.ConfigType.STRING)
                .configValue("jdbc:mysql://localhost:3306/db")
                .build();

        // When & Then
        assertDoesNotThrow(() -> validator.validate(systemConfig));
    }

    @Test
    @DisplayName("사용자 설정 네임스페이스 검증 성공")
    void shouldValidateUserNamespace() {
        // Given
        PlatformConfig userConfig = PlatformConfig.builder()
                .configKey("user.theme.color")
                .isSystem(false)
                .configType(PlatformConfig.ConfigType.STRING)
                .configValue("blue")
                .build();

        // When & Then
        assertDoesNotThrow(() -> validator.validate(userConfig));
    }

    @Test
    @DisplayName("isSystem이 null인 경우 검증 성공")
    void shouldValidateWhenIsSystemIsNull() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("system.database.url")
                .isSystem(null)  // null인 경우 자동 설정 로직에서 처리
                .configType(PlatformConfig.ConfigType.STRING)
                .configValue("jdbc:mysql://localhost:3306/db")
                .build();

        // When & Then
        assertDoesNotThrow(() -> validator.validate(config));
    }

    @Test
    @DisplayName("시스템 설정 네임스페이스 불일치 실패")
    void shouldFailSystemNamespaceMismatch() {
        // Given
        PlatformConfig invalidConfig = PlatformConfig.builder()
                .configKey("user.database.url")  // user 네임스페이스
                .isSystem(true)                  // 하지만 isSystem=true
                .configType(PlatformConfig.ConfigType.STRING)
                .configValue("jdbc:mysql://localhost:3306/db")
                .build();

        // When & Then
        ConfigValidationException exception = assertThrows(ConfigValidationException.class,
                () -> validator.validate(invalidConfig));
        assertEquals(PlatformConfigErrorCode.SYSTEM_CONFIG_NAMESPACE_MISMATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("사용자 설정 네임스페이스 불일치 실패")
    void shouldFailUserNamespaceMismatch() {
        // Given
        PlatformConfig invalidConfig = PlatformConfig.builder()
                .configKey("system.theme.color")  // system 네임스페이스
                .isSystem(false)                  // 하지만 isSystem=false
                .configType(PlatformConfig.ConfigType.STRING)
                .configValue("blue")
                .build();

        // When & Then
        ConfigValidationException exception = assertThrows(ConfigValidationException.class,
                () -> validator.validate(invalidConfig));
        assertEquals(PlatformConfigErrorCode.USER_CONFIG_NAMESPACE_MISMATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("잘못된 네임스페이스 형식 실패")
    void shouldFailInvalidNamespace() {
        // Given
        PlatformConfig invalidConfig = PlatformConfig.builder()
                .configKey("invalid.key")  // system. 또는 user.로 시작하지 않음
                .isSystem(false)
                .configType(PlatformConfig.ConfigType.STRING)
                .configValue("value")
                .build();

        // When & Then
        ConfigValidationException exception = assertThrows(ConfigValidationException.class,
                () -> validator.validate(invalidConfig));
        assertEquals(PlatformConfigErrorCode.INVALID_CONFIG_NAMESPACE, exception.getErrorCode());
    }

    @Test
    @DisplayName("빈 설정 키 실패")
    void shouldFailEmptyConfigKey() {
        // Given
        PlatformConfig invalidConfig = PlatformConfig.builder()
                .configKey("")  // 빈 키
                .isSystem(false)
                .configType(PlatformConfig.ConfigType.STRING)
                .configValue("value")
                .build();

        // When & Then
        ConfigValidationException exception = assertThrows(ConfigValidationException.class,
                () -> validator.validate(invalidConfig));
        assertEquals(PlatformConfigErrorCode.CONFIG_KEY_REQUIRED, exception.getErrorCode());
    }

    @Test
    @DisplayName("null 설정 키 실패")
    void shouldFailNullConfigKey() {
        // Given
        PlatformConfig invalidConfig = PlatformConfig.builder()
                .configKey(null)  // null 키
                .isSystem(false)
                .configType(PlatformConfig.ConfigType.STRING)
                .configValue("value")
                .build();

        // When & Then
        ConfigValidationException exception = assertThrows(ConfigValidationException.class,
                () -> validator.validate(invalidConfig));
        assertEquals(PlatformConfigErrorCode.CONFIG_KEY_REQUIRED, exception.getErrorCode());
    }

    @Test
    @DisplayName("시스템 설정 키 형식 검증")
    void shouldValidateSystemKeyFormats() {
        // Given
        String[] validSystemKeys = {
                "system.database.url",
                "system.security.key",
                "system.config.timeout",
                "system.app.version"
        };

        for (String key : validSystemKeys) {
            PlatformConfig config = PlatformConfig.builder()
                    .configKey(key)
                    .isSystem(true)
                    .configType(PlatformConfig.ConfigType.STRING)
                    .configValue("value")
                    .build();

            // When & Then
            assertDoesNotThrow(() -> validator.validate(config), 
                    "Key '" + key + "' should be valid");
        }
    }

    @Test
    @DisplayName("사용자 설정 키 형식 검증")
    void shouldValidateUserKeyFormats() {
        // Given
        String[] validUserKeys = {
                "user.theme.color",
                "user.preferences.language",
                "user.notification.email",
                "user.profile.name"
        };

        for (String key : validUserKeys) {
            PlatformConfig config = PlatformConfig.builder()
                    .configKey(key)
                    .isSystem(false)
                    .configType(PlatformConfig.ConfigType.STRING)
                    .configValue("value")
                    .build();

            // When & Then
            assertDoesNotThrow(() -> validator.validate(config), 
                    "Key '" + key + "' should be valid");
        }
    }
}
