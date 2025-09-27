package com.agenticcp.core.domain.platform.validation.impl;

import com.agenticcp.core.domain.platform.entity.PlatformConfig;
import com.agenticcp.core.domain.platform.enums.PlatformConfigErrorCode;
import com.agenticcp.core.domain.platform.exception.ConfigValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EncryptedConfigValidator 단위 테스트
 *
 * @author AgenticCP Team
 * @since 2025-09-26
 */
@DisplayName("EncryptedConfigValidator 테스트")
class EncryptedConfigValidatorTest {

    private EncryptedConfigValidator validator;

    @BeforeEach
    void setUp() {
        validator = new EncryptedConfigValidator();
    }

    @Test
    @DisplayName("유효한 암호화된 값 검증 성공")
    void shouldValidateValidEncryptedValue() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.encrypted.key")
                .configType(PlatformConfig.ConfigType.ENCRYPTED)
                .configValue("encrypted_value_123")
                .build();

        // When & Then
        assertDoesNotThrow(() -> validator.validate(config));
    }

    @Test
    @DisplayName("긴 암호화된 값 검증 성공")
    void shouldValidateLongEncryptedValue() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.encrypted.key")
                .configType(PlatformConfig.ConfigType.ENCRYPTED)
                .configValue("very_long_encrypted_value_with_special_characters_!@#$%^&*()")
                .build();

        // When & Then
        assertDoesNotThrow(() -> validator.validate(config));
    }

    @Test
    @DisplayName("빈 문자열 값 검증 실패")
    void shouldRejectEmptyStringValue() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.encrypted.key")
                .configType(PlatformConfig.ConfigType.ENCRYPTED)
                .configValue("")
                .build();

        // When & Then
        ConfigValidationException exception = assertThrows(ConfigValidationException.class,
                () -> validator.validate(config));
        assertEquals(PlatformConfigErrorCode.ENCRYPTED_VALUE_EMPTY, exception.getErrorCode());
    }

    @Test
    @DisplayName("공백만 있는 값 검증 실패")
    void shouldRejectWhitespaceOnlyValue() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.encrypted.key")
                .configType(PlatformConfig.ConfigType.ENCRYPTED)
                .configValue("   ")
                .build();

        // When & Then
        ConfigValidationException exception = assertThrows(ConfigValidationException.class,
                () -> validator.validate(config));
        assertEquals(PlatformConfigErrorCode.ENCRYPTED_VALUE_EMPTY, exception.getErrorCode());
    }

    @Test
    @DisplayName("null 값 검증 실패")
    void shouldRejectNullValue() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.encrypted.key")
                .configType(PlatformConfig.ConfigType.ENCRYPTED)
                .configValue(null)
                .build();

        // When & Then
        ConfigValidationException exception = assertThrows(ConfigValidationException.class,
                () -> validator.validate(config));
        assertEquals(PlatformConfigErrorCode.CONFIG_VALUE_REQUIRED, exception.getErrorCode());
    }

    @Test
    @DisplayName("ENCRYPTED 타입이 아닌 경우 검증 통과")
    void shouldPassValidationForNonEncryptedType() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.string.key")
                .configType(PlatformConfig.ConfigType.STRING)
                .configValue("not encrypted")
                .build();

        // When & Then
        assertDoesNotThrow(() -> validator.validate(config));
    }

    @Test
    @DisplayName("유효한 키 형식 검증 성공")
    void shouldValidateValidKey() {
        // Given
        String validKey = "test.encrypted.key";

        // When & Then
        assertDoesNotThrow(() -> validator.validateKey(validKey));
    }

    @Test
    @DisplayName("잘못된 키 형식 검증 실패")
    void shouldRejectInvalidKey() {
        // Given
        String invalidKey = "123invalid.key"; // 숫자로 시작

        // When & Then
        ConfigValidationException exception = assertThrows(ConfigValidationException.class,
                () -> validator.validateKey(invalidKey));
        assertEquals(PlatformConfigErrorCode.CONFIG_KEY_INVALID_FORMAT, exception.getErrorCode());
    }
}
