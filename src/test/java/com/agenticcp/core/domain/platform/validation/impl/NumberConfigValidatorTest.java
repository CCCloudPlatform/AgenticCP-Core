package com.agenticcp.core.domain.platform.validation.impl;

import com.agenticcp.core.domain.platform.entity.PlatformConfig;
import com.agenticcp.core.domain.platform.enums.PlatformConfigErrorCode;
import com.agenticcp.core.domain.platform.exception.ConfigValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NumberConfigValidator 단위 테스트
 *
 * @author AgenticCP Team
 * @since 2025-09-26
 */
@DisplayName("NumberConfigValidator 테스트")
class NumberConfigValidatorTest {

    private NumberConfigValidator validator;

    @BeforeEach
    void setUp() {
        validator = new NumberConfigValidator();
    }

    @Test
    @DisplayName("유효한 정수 값 검증 성공")
    void shouldValidateValidIntegerValue() {
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
    @DisplayName("유효한 실수 값 검증 성공")
    void shouldValidateValidDecimalValue() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.number.key")
                .configType(PlatformConfig.ConfigType.NUMBER)
                .configValue("123.45")
                .build();

        // When & Then
        assertDoesNotThrow(() -> validator.validate(config));
    }

    @Test
    @DisplayName("음수 값 검증 성공")
    void shouldValidateNegativeNumberValue() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.number.key")
                .configType(PlatformConfig.ConfigType.NUMBER)
                .configValue("-123.45")
                .build();

        // When & Then
        assertDoesNotThrow(() -> validator.validate(config));
    }

    @Test
    @DisplayName("0 값 검증 성공")
    void shouldValidateZeroValue() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.number.key")
                .configType(PlatformConfig.ConfigType.NUMBER)
                .configValue("0")
                .build();

        // When & Then
        assertDoesNotThrow(() -> validator.validate(config));
    }

    @Test
    @DisplayName("잘못된 숫자 형식 검증 실패")
    void shouldRejectInvalidNumberFormat() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.number.key")
                .configType(PlatformConfig.ConfigType.NUMBER)
                .configValue("not.a.number")
                .build();

        // When & Then
        ConfigValidationException exception = assertThrows(ConfigValidationException.class,
                () -> validator.validate(config));
        assertEquals(PlatformConfigErrorCode.NUMBER_VALUE_INVALID_FORMAT, exception.getErrorCode());
    }

    @Test
    @DisplayName("빈 문자열 값 검증 실패")
    void shouldRejectEmptyStringValue() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.number.key")
                .configType(PlatformConfig.ConfigType.NUMBER)
                .configValue("")
                .build();

        // When & Then
        ConfigValidationException exception = assertThrows(ConfigValidationException.class,
                () -> validator.validate(config));
        assertEquals(PlatformConfigErrorCode.CONFIG_VALUE_REQUIRED, exception.getErrorCode());
    }

    @Test
    @DisplayName("null 값 검증 실패")
    void shouldRejectNullValue() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.number.key")
                .configType(PlatformConfig.ConfigType.NUMBER)
                .configValue(null)
                .build();

        // When & Then
        ConfigValidationException exception = assertThrows(ConfigValidationException.class,
                () -> validator.validate(config));
        assertEquals(PlatformConfigErrorCode.CONFIG_VALUE_REQUIRED, exception.getErrorCode());
    }

    @Test
    @DisplayName("NUMBER 타입이 아닌 경우 검증 통과")
    void shouldPassValidationForNonNumberType() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.string.key")
                .configType(PlatformConfig.ConfigType.STRING)
                .configValue("not a number")
                .build();

        // When & Then
        assertDoesNotThrow(() -> validator.validate(config));
    }
}
