package com.agenticcp.core.domain.platform.util;

import com.agenticcp.core.domain.platform.entity.PlatformConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LoggingUtils 단위 테스트
 * - 민감한 값 마스킹 테스트
 * - 설정 키 민감도 판별 테스트
 * - 로깅 포맷 테스트
 */
class LoggingUtilsTest {

    @Test
    @DisplayName("ENCRYPTED 타입 설정값 마스킹 테스트")
    void maskSensitiveValue_EncryptedType() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.secret")
                .configValue("secret123")
                .configType(PlatformConfig.ConfigType.ENCRYPTED)
                .isEncrypted(true)
                .build();

        // When
        String maskedValue = LoggingUtils.maskSensitiveValue(config);

        // Then
        assertEquals("******", maskedValue);
    }

    @Test
    @DisplayName("isEncrypted=true인 설정값 마스킹 테스트")
    void maskSensitiveValue_IsEncryptedTrue() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.key")
                .configValue("encrypted_value")
                .configType(PlatformConfig.ConfigType.STRING)
                .isEncrypted(true)
                .build();

        // When
        String maskedValue = LoggingUtils.maskSensitiveValue(config);

        // Then
        assertEquals("******", maskedValue);
    }

    @Test
    @DisplayName("민감한 키를 가진 시스템 설정 마스킹 테스트")
    void maskSensitiveValue_SensitiveSystemKey() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("system.password")
                .configValue("admin123")
                .configType(PlatformConfig.ConfigType.STRING)
                .isSystem(true)
                .build();

        // When
        String maskedValue = LoggingUtils.maskSensitiveValue(config);

        // Then
        assertEquals("******", maskedValue);
    }

    @Test
    @DisplayName("일반 설정값은 마스킹하지 않음")
    void maskSensitiveValue_NormalConfig() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("app.name")
                .configValue("MyApp")
                .configType(PlatformConfig.ConfigType.STRING)
                .isSystem(false)
                .build();

        // When
        String maskedValue = LoggingUtils.maskSensitiveValue(config);

        // Then
        assertEquals("MyApp", maskedValue);
    }

    @Test
    @DisplayName("null 값 처리 테스트")
    void maskSensitiveValue_NullValues() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.key")
                .configValue(null)
                .build();

        // When
        String maskedValue = LoggingUtils.maskSensitiveValue(config);

        // Then
        assertNull(maskedValue);
    }

    @Test
    @DisplayName("로깅 포맷 테스트")
    void formatConfigForLogging() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.secret")
                .configValue("secret123")
                .configType(PlatformConfig.ConfigType.ENCRYPTED)
                .isEncrypted(true)
                .isSystem(false)
                .build();

        // When
        String formatted = LoggingUtils.formatConfigForLogging(config);

        // Then
        assertTrue(formatted.contains("key='test.secret'"));
        assertTrue(formatted.contains("type=ENCRYPTED"));
        assertTrue(formatted.contains("value='******'"));
        assertTrue(formatted.contains("isEncrypted=true"));
        assertTrue(formatted.contains("isSystem=false"));
    }

    @Test
    @DisplayName("다양한 민감한 키 패턴 테스트")
    void maskSensitiveValue_VariousSensitiveKeys() {
        // Given
        String[] sensitiveKeys = {
            "database.password",
            "api.secret",
            "jwt.token",
            "auth.credential",
            "system.key"
        };

        for (String key : sensitiveKeys) {
            PlatformConfig config = PlatformConfig.builder()
                    .configKey(key)
                    .configValue("sensitive_value")
                    .configType(PlatformConfig.ConfigType.STRING)
                    .isSystem(true)
                    .build();

            // When
            String maskedValue = LoggingUtils.maskSensitiveValue(config);

            // Then
            assertEquals("******", maskedValue, "Key '" + key + "' should be masked");
        }
    }

    @Test
    @DisplayName("민감하지 않은 키는 마스킹하지 않음")
    void maskSensitiveValue_NonSensitiveKeys() {
        // Given
        String[] nonSensitiveKeys = {
            "app.name",
            "server.port",
            "database.host",
            "cache.timeout",
            "feature.enabled"
        };

        for (String key : nonSensitiveKeys) {
            PlatformConfig config = PlatformConfig.builder()
                    .configKey(key)
                    .configValue("normal_value")
                    .configType(PlatformConfig.ConfigType.STRING)
                    .isSystem(true)
                    .build();

            // When
            String maskedValue = LoggingUtils.maskSensitiveValue(config);

            // Then
            assertEquals("normal_value", maskedValue, "Key '" + key + "' should not be masked");
        }
    }
}
