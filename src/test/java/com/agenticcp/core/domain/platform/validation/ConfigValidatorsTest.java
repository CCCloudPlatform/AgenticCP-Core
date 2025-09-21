package com.agenticcp.core.domain.platform.validation;

import com.agenticcp.core.common.exception.ValidationException;
import com.agenticcp.core.domain.platform.entity.PlatformConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ConfigValidators 단위 테스트
 * - 키 정책 검증 테스트
 * - 타입별 값 검증 테스트
 * - 경계값 및 예외 케이스 테스트
 */
class ConfigValidatorsTest {

    @Test
    @DisplayName("키 정책 검증 - 정상 케이스")
    void validateKeyPolicy_ValidKeys() {
        // Given
        String[] validKeys = {
            "abc", "test123", "my-config", "app.setting", "user_name", 
            "valid_key_123", "test.config.value", "APP_SETTING"
        };

        // When & Then
        for (String key : validKeys) {
            assertDoesNotThrow(() -> ConfigValidators.validateKeyPolicy(key),
                "키 '%s'는 유효해야 합니다".formatted(key));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "ab", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
    @DisplayName("키 정책 검증 - 길이 제한 위반")
    void validateKeyPolicy_InvalidLength(String invalidKey) {
        // When & Then
        ValidationException exception = assertThrows(ValidationException.class,
            () -> ConfigValidators.validateKeyPolicy(invalidKey));
        
        assertTrue(exception.getMessage().contains("length must be between 3 and 128") || 
                   exception.getMessage().contains("must not be blank"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-invalid", "_invalid", ".invalid"})
    @DisplayName("키 정책 검증 - 시작 문자 제한 위반")
    void validateKeyPolicy_InvalidStartChar(String invalidKey) {
        // When & Then
        ValidationException exception = assertThrows(ValidationException.class,
            () -> ConfigValidators.validateKeyPolicy(invalidKey));
        
        assertTrue(exception.getMessage().contains("must start with alphanumeric"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid@key", "invalid#key", "invalid key", "invalid/key"})
    @DisplayName("키 정책 검증 - 허용되지 않은 문자")
    void validateKeyPolicy_InvalidCharacters(String invalidKey) {
        // When & Then
        ValidationException exception = assertThrows(ValidationException.class,
            () -> ConfigValidators.validateKeyPolicy(invalidKey));
        
        assertTrue(exception.getMessage().contains("allowed chars are [a-zA-Z0-9._-]"));
    }

    @Test
    @DisplayName("STRING 타입 값 검증 - 정상 케이스")
    void validateValueByType_String_Valid() {
        // Given
        String[] validValues = {"", "hello", "123", "special!@#$%", "한글"};

        // When & Then
        for (String value : validValues) {
            assertDoesNotThrow(() -> 
                ConfigValidators.validateValueByType(PlatformConfig.ConfigType.STRING, value),
                "STRING 값 '%s'는 유효해야 합니다".formatted(value));
        }
    }

    @Test
    @DisplayName("NUMBER 타입 값 검증 - 정상 케이스")
    void validateValueByType_Number_Valid() {
        // Given
        String[] validValues = {"0", "123", "-456", "3.14", "1.0", "-0.5"};

        // When & Then
        for (String value : validValues) {
            assertDoesNotThrow(() -> 
                ConfigValidators.validateValueByType(PlatformConfig.ConfigType.NUMBER, value),
                "NUMBER 값 '%s'는 유효해야 합니다".formatted(value));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "12.34.56", "not-a-number", ""})
    @DisplayName("NUMBER 타입 값 검증 - 잘못된 형식")
    void validateValueByType_Number_Invalid(String invalidValue) {
        // When & Then
        ValidationException exception = assertThrows(ValidationException.class,
            () -> ConfigValidators.validateValueByType(PlatformConfig.ConfigType.NUMBER, invalidValue));
        
        assertTrue(exception.getMessage().contains("must be a valid number"));
    }

    @Test
    @DisplayName("BOOLEAN 타입 값 검증 - 정상 케이스")
    void validateValueByType_Boolean_Valid() {
        // Given
        String[] validValues = {"true", "false"};

        // When & Then
        for (String value : validValues) {
            assertDoesNotThrow(() -> 
                ConfigValidators.validateValueByType(PlatformConfig.ConfigType.BOOLEAN, value),
                "BOOLEAN 값 '%s'는 유효해야 합니다".formatted(value));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"True", "FALSE", "1", "0", "yes", "no"})
    @DisplayName("BOOLEAN 타입 값 검증 - 잘못된 형식")
    void validateValueByType_Boolean_Invalid(String invalidValue) {
        // When & Then
        ValidationException exception = assertThrows(ValidationException.class,
            () -> ConfigValidators.validateValueByType(PlatformConfig.ConfigType.BOOLEAN, invalidValue));
        
        assertTrue(exception.getMessage().contains("must be 'true' or 'false' (lowercase)"));
    }

    @Test
    @DisplayName("JSON 타입 값 검증 - 정상 케이스")
    void validateValueByType_Json_Valid() {
        // Given
        String[] validValues = {
            "{}", "[]", "{\"key\": \"value\"}", 
            "{\"nested\": {\"array\": [1, 2, 3]}}",
            "{\"boolean\": true, \"number\": 123}"
        };

        // When & Then
        for (String value : validValues) {
            assertDoesNotThrow(() -> 
                ConfigValidators.validateValueByType(PlatformConfig.ConfigType.JSON, value),
                "JSON 값 '%s'는 유효해야 합니다".formatted(value));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "}", "{key: value}", "{'key': 'value'}", "not json"})
    @DisplayName("JSON 타입 값 검증 - 잘못된 형식")
    void validateValueByType_Json_Invalid(String invalidValue) {
        // When & Then
        ValidationException exception = assertThrows(ValidationException.class,
            () -> ConfigValidators.validateValueByType(PlatformConfig.ConfigType.JSON, invalidValue));
        
        assertTrue(exception.getMessage().contains("must be valid JSON"));
    }

    @Test
    @DisplayName("ENCRYPTED 타입 값 검증 - 정상 케이스")
    void validateValueByType_Encrypted_Valid() {
        // Given
        String[] validValues = {"encrypted_value", "secret123", "a"};

        // When & Then
        for (String value : validValues) {
            assertDoesNotThrow(() -> 
                ConfigValidators.validateValueByType(PlatformConfig.ConfigType.ENCRYPTED, value),
                "ENCRYPTED 값 '%s'는 유효해야 합니다".formatted(value));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t", "\n"})
    @DisplayName("ENCRYPTED 타입 값 검증 - 빈 값")
    void validateValueByType_Encrypted_Invalid(String invalidValue) {
        // When & Then
        ValidationException exception = assertThrows(ValidationException.class,
            () -> ConfigValidators.validateValueByType(PlatformConfig.ConfigType.ENCRYPTED, invalidValue));
        
        assertTrue(exception.getMessage().contains("encrypted value must not be blank"));
    }

    @Test
    @DisplayName("null 값 검증")
    void validateValueByType_NullValues() {
        // When & Then
        ValidationException typeException = assertThrows(ValidationException.class,
            () -> ConfigValidators.validateValueByType(null, "value"));
        assertTrue(typeException.getMessage().contains("must not be null"));

        ValidationException valueException = assertThrows(ValidationException.class,
            () -> ConfigValidators.validateValueByType(PlatformConfig.ConfigType.STRING, null));
        assertTrue(valueException.getMessage().contains("must not be null"));
    }
}
