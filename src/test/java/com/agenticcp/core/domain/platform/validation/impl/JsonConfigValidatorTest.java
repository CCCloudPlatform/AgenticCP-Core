package com.agenticcp.core.domain.platform.validation.impl;

import com.agenticcp.core.domain.platform.entity.PlatformConfig;
import com.agenticcp.core.domain.platform.enums.PlatformConfigErrorCode;
import com.agenticcp.core.domain.platform.exception.ConfigValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JsonConfigValidator 단위 테스트
 *
 * @author AgenticCP Team
 * @since 2025-09-26
 */
@DisplayName("JsonConfigValidator 테스트")
class JsonConfigValidatorTest {

    private JsonConfigValidator validator;

    @BeforeEach
    void setUp() {
        validator = new JsonConfigValidator();
    }

    @Test
    @DisplayName("유효한 JSON 객체 검증 성공")
    void shouldValidateValidJsonObject() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.json.key")
                .configType(PlatformConfig.ConfigType.JSON)
                .configValue("{\"name\": \"test\", \"value\": 123}")
                .build();

        // When & Then
        assertDoesNotThrow(() -> validator.validate(config));
    }

    @Test
    @DisplayName("유효한 JSON 배열 검증 성공")
    void shouldValidateValidJsonArray() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.json.key")
                .configType(PlatformConfig.ConfigType.JSON)
                .configValue("[1, 2, 3, \"test\"]")
                .build();

        // When & Then
        assertDoesNotThrow(() -> validator.validate(config));
    }

    @Test
    @DisplayName("유효한 JSON 문자열 검증 성공")
    void shouldValidateValidJsonString() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.json.key")
                .configType(PlatformConfig.ConfigType.JSON)
                .configValue("\"simple string\"")
                .build();

        // When & Then
        assertDoesNotThrow(() -> validator.validate(config));
    }

    @Test
    @DisplayName("유효한 JSON 숫자 검증 성공")
    void shouldValidateValidJsonNumber() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.json.key")
                .configType(PlatformConfig.ConfigType.JSON)
                .configValue("123.45")
                .build();

        // When & Then
        assertDoesNotThrow(() -> validator.validate(config));
    }

    @Test
    @DisplayName("유효한 JSON 불린 검증 성공")
    void shouldValidateValidJsonBoolean() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.json.key")
                .configType(PlatformConfig.ConfigType.JSON)
                .configValue("true")
                .build();

        // When & Then
        assertDoesNotThrow(() -> validator.validate(config));
    }

    @Test
    @DisplayName("유효한 JSON null 검증 성공")
    void shouldValidateValidJsonNull() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.json.key")
                .configType(PlatformConfig.ConfigType.JSON)
                .configValue("null")
                .build();

        // When & Then
        assertDoesNotThrow(() -> validator.validate(config));
    }

    @Test
    @DisplayName("잘못된 JSON 형식 검증 실패")
    void shouldRejectInvalidJsonFormat() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.json.key")
                .configType(PlatformConfig.ConfigType.JSON)
                .configValue("{ invalid json }")
                .build();

        // When & Then
        ConfigValidationException exception = assertThrows(ConfigValidationException.class,
                () -> validator.validate(config));
        assertEquals(PlatformConfigErrorCode.JSON_VALUE_INVALID_FORMAT, exception.getErrorCode());
    }

    @Test
    @DisplayName("불완전한 JSON 객체 검증 실패")
    void shouldRejectIncompleteJsonObject() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.json.key")
                .configType(PlatformConfig.ConfigType.JSON)
                .configValue("{\"name\": \"test\"")
                .build();

        // When & Then
        ConfigValidationException exception = assertThrows(ConfigValidationException.class,
                () -> validator.validate(config));
        assertEquals(PlatformConfigErrorCode.JSON_VALUE_INVALID_FORMAT, exception.getErrorCode());
    }

    @Test
    @DisplayName("빈 문자열 값 검증 실패")
    void shouldRejectEmptyStringValue() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.json.key")
                .configType(PlatformConfig.ConfigType.JSON)
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
                .configKey("test.json.key")
                .configType(PlatformConfig.ConfigType.JSON)
                .configValue(null)
                .build();

        // When & Then
        ConfigValidationException exception = assertThrows(ConfigValidationException.class,
                () -> validator.validate(config));
        assertEquals(PlatformConfigErrorCode.CONFIG_VALUE_REQUIRED, exception.getErrorCode());
    }

    @Test
    @DisplayName("JSON 타입이 아닌 경우 검증 통과")
    void shouldPassValidationForNonJsonType() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.string.key")
                .configType(PlatformConfig.ConfigType.STRING)
                .configValue("not a json")
                .build();

        // When & Then
        assertDoesNotThrow(() -> validator.validate(config));
    }
}
