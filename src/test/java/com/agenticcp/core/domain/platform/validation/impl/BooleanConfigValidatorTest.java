package com.agenticcp.core.domain.platform.validation.impl;

import com.agenticcp.core.domain.platform.entity.PlatformConfig;
import com.agenticcp.core.domain.platform.enums.PlatformConfigErrorCode;
import com.agenticcp.core.domain.platform.exception.ConfigValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BooleanConfigValidator 단위 테스트
 *
 * @author AgenticCP Team
 * @since 2025-09-26
 */
@DisplayName("BooleanConfigValidator 테스트")
class BooleanConfigValidatorTest {

    private BooleanConfigValidator validator;

    @BeforeEach
    void setUp() {
        validator = new BooleanConfigValidator();
    }

    @Test
    @DisplayName("유효한 'true' 값 검증 성공")
    void shouldValidateTrueValue() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.boolean.key")
                .configType(PlatformConfig.ConfigType.BOOLEAN)
                .configValue("true")
                .build();

        // When & Then
        assertDoesNotThrow(() -> validator.validate(config));
    }

    @Test
    @DisplayName("유효한 'false' 값 검증 성공")
    void shouldValidateFalseValue() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.boolean.key")
                .configType(PlatformConfig.ConfigType.BOOLEAN)
                .configValue("false")
                .build();

        // When & Then
        assertDoesNotThrow(() -> validator.validate(config));
    }

    @Test
    @DisplayName("대문자 'TRUE' 값 검증 성공")
    void shouldValidateUpperCaseTrueValue() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.boolean.key")
                .configType(PlatformConfig.ConfigType.BOOLEAN)
                .configValue("TRUE")
                .build();

        // When & Then
        assertDoesNotThrow(() -> validator.validate(config));
    }

    @Test
    @DisplayName("대문자 'FALSE' 값 검증 성공")
    void shouldValidateUpperCaseFalseValue() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.boolean.key")
                .configType(PlatformConfig.ConfigType.BOOLEAN)
                .configValue("FALSE")
                .build();

        // When & Then
        assertDoesNotThrow(() -> validator.validate(config));
    }

    @Test
    @DisplayName("혼합 대소문자 'True' 값 검증 성공")
    void shouldValidateMixedCaseTrueValue() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.boolean.key")
                .configType(PlatformConfig.ConfigType.BOOLEAN)
                .configValue("True")
                .build();

        // When & Then
        assertDoesNotThrow(() -> validator.validate(config));
    }

    @Test
    @DisplayName("잘못된 불린 값 검증 실패")
    void shouldRejectInvalidBooleanValue() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.boolean.key")
                .configType(PlatformConfig.ConfigType.BOOLEAN)
                .configValue("yes")
                .build();

        // When & Then
        ConfigValidationException exception = assertThrows(ConfigValidationException.class,
                () -> validator.validate(config));
        assertEquals(PlatformConfigErrorCode.BOOLEAN_VALUE_INVALID, exception.getErrorCode());
    }

    @Test
    @DisplayName("숫자 값 검증 실패")
    void shouldRejectNumericValue() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.boolean.key")
                .configType(PlatformConfig.ConfigType.BOOLEAN)
                .configValue("1")
                .build();

        // When & Then
        ConfigValidationException exception = assertThrows(ConfigValidationException.class,
                () -> validator.validate(config));
        assertEquals(PlatformConfigErrorCode.BOOLEAN_VALUE_INVALID, exception.getErrorCode());
    }

    @Test
    @DisplayName("빈 문자열 값 검증 실패")
    void shouldRejectEmptyStringValue() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.boolean.key")
                .configType(PlatformConfig.ConfigType.BOOLEAN)
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
                .configKey("test.boolean.key")
                .configType(PlatformConfig.ConfigType.BOOLEAN)
                .configValue(null)
                .build();

        // When & Then
        ConfigValidationException exception = assertThrows(ConfigValidationException.class,
                () -> validator.validate(config));
        assertEquals(PlatformConfigErrorCode.CONFIG_VALUE_REQUIRED, exception.getErrorCode());
    }

    @Test
    @DisplayName("BOOLEAN 타입이 아닌 경우 검증 통과")
    void shouldPassValidationForNonBooleanType() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.string.key")
                .configType(PlatformConfig.ConfigType.STRING)
                .configValue("not a boolean")
                .build();

        // When & Then
        assertDoesNotThrow(() -> validator.validate(config));
    }
}
