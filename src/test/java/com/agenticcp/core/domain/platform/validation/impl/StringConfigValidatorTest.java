package com.agenticcp.core.domain.platform.validation.impl;

import com.agenticcp.core.domain.platform.entity.PlatformConfig;
import com.agenticcp.core.domain.platform.enums.PlatformConfigErrorCode;
import com.agenticcp.core.domain.platform.exception.ConfigValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StringConfigValidator 단위 테스트
 *
 * @author AgenticCP Team
 * @since 2025-09-26
 */
@DisplayName("StringConfigValidator 테스트")
class StringConfigValidatorTest {

    private StringConfigValidator validator;

    @BeforeEach
    void setUp() {
        validator = new StringConfigValidator();
    }

    @Test
    @DisplayName("유효한 STRING 타입 설정 검증 성공")
    void shouldValidateValidStringConfig() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.string.key")
                .configType(PlatformConfig.ConfigType.STRING)
                .configValue("valid string value")
                .build();

        // When & Then
        assertDoesNotThrow(() -> validator.validate(config));
    }

    @Test
    @DisplayName("빈 문자열 값 검증 실패")
    void shouldRejectEmptyStringValue() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.string.key")
                .configType(PlatformConfig.ConfigType.STRING)
                .configValue("")
                .build();

        // When & Then
        ConfigValidationException exception = assertThrows(ConfigValidationException.class,
                () -> validator.validate(config));
        assertEquals(PlatformConfigErrorCode.STRING_VALUE_EMPTY, exception.getErrorCode());
    }

    @Test
    @DisplayName("공백만 있는 문자열 값 검증 실패")
    void shouldRejectWhitespaceOnlyStringValue() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.string.key")
                .configType(PlatformConfig.ConfigType.STRING)
                .configValue("   ")
                .build();

        // When & Then
        ConfigValidationException exception = assertThrows(ConfigValidationException.class,
                () -> validator.validate(config));
        assertEquals(PlatformConfigErrorCode.STRING_VALUE_EMPTY, exception.getErrorCode());
    }

    @Test
    @DisplayName("null 값 검증 실패")
    void shouldRejectNullStringValue() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.string.key")
                .configType(PlatformConfig.ConfigType.STRING)
                .configValue(null)
                .build();

        // When & Then
        ConfigValidationException exception = assertThrows(ConfigValidationException.class,
                () -> validator.validate(config));
        assertEquals(PlatformConfigErrorCode.CONFIG_VALUE_REQUIRED, exception.getErrorCode());
    }

    @Test
    @DisplayName("STRING 타입이 아닌 경우 검증 통과")
    void shouldPassValidationForNonStringType() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.number.key")
                .configType(PlatformConfig.ConfigType.NUMBER)
                .configValue("123")
                .build();

        // When & Then
        assertDoesNotThrow(() -> validator.validate(config));
    }

    @Test
    @DisplayName("유효한 키 형식 검증 성공")
    void shouldValidateValidKey() {
        // Given
        String validKey = "test.string.key";

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
