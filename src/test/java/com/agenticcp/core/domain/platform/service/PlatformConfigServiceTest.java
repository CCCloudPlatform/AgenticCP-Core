package com.agenticcp.core.domain.platform.service;

import com.agenticcp.core.domain.platform.entity.PlatformConfig;
import com.agenticcp.core.domain.platform.enums.PlatformConfigErrorCode;
import com.agenticcp.core.domain.platform.exception.ConfigValidationException;
import com.agenticcp.core.domain.platform.repository.PlatformConfigRepository;
import com.agenticcp.core.domain.platform.validation.ConfigValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * PlatformConfigService 단위 테스트
 *
 * @author AgenticCP Team
 * @since 2025-09-26
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PlatformConfigService 테스트")
class PlatformConfigServiceTest {

    @Mock
    private PlatformConfigRepository platformConfigRepository;

    @Mock
    private ConfigValidator configValidator;

    @InjectMocks
    private PlatformConfigService platformConfigService;

    private PlatformConfig validConfig;
    private PlatformConfig systemConfig;

    @BeforeEach
    void setUp() {
        // Mock validator가 아무것도 하지 않도록 설정
        doNothing().when(configValidator).validate(any(PlatformConfig.class));
        doNothing().when(configValidator).validateKey(anyString());
        doNothing().when(configValidator).validateValue(anyString(), any(PlatformConfig.ConfigType.class));

        validConfig = PlatformConfig.builder()
                .configKey("test.config.key")
                .configValue("test value")
                .configType(PlatformConfig.ConfigType.STRING)
                .description("Test configuration")
                .isEncrypted(false)
                .isSystem(false)
                .build();

        systemConfig = PlatformConfig.builder()
                .configKey("system.config.key")
                .configValue("system value")
                .configType(PlatformConfig.ConfigType.STRING)
                .description("System configuration")
                .isEncrypted(false)
                .isSystem(true)
                .build();
    }

    @Test
    @DisplayName("유효한 설정 생성 성공")
    void shouldCreateValidConfig() {
        // Given
        when(platformConfigRepository.findByConfigKey(anyString())).thenReturn(Optional.empty());
        when(platformConfigRepository.save(any(PlatformConfig.class))).thenReturn(validConfig);

        // When
        PlatformConfig result = platformConfigService.createConfig(validConfig);

        // Then
        assertNotNull(result);
        assertEquals(validConfig.getConfigKey(), result.getConfigKey());
        verify(platformConfigRepository).findByConfigKey(validConfig.getConfigKey());
        verify(platformConfigRepository).save(validConfig);
        verify(configValidator).validate(validConfig);
    }

    @Test
    @DisplayName("중복 키로 인한 설정 생성 실패")
    void shouldFailToCreateConfigWithDuplicateKey() {
        // Given
        when(platformConfigRepository.findByConfigKey(anyString())).thenReturn(Optional.of(validConfig));

        // When & Then
        ConfigValidationException exception = assertThrows(ConfigValidationException.class,
                () -> platformConfigService.createConfig(validConfig));
        assertEquals(PlatformConfigErrorCode.CONFIG_ALREADY_EXISTS, exception.getErrorCode());
        verify(platformConfigRepository).findByConfigKey(validConfig.getConfigKey());
        verify(platformConfigRepository, never()).save(any(PlatformConfig.class));
    }

    @Test
    @DisplayName("유효한 설정 수정 성공")
    void shouldUpdateValidConfig() {
        // Given
        PlatformConfig updatedConfig = PlatformConfig.builder()
                .configValue("updated value")
                .configType(PlatformConfig.ConfigType.STRING)
                .description("Updated description")
                .isEncrypted(false)
                .build();

        when(platformConfigRepository.findByConfigKey(anyString())).thenReturn(Optional.of(validConfig));
        when(platformConfigRepository.save(any(PlatformConfig.class))).thenReturn(validConfig);

        // When
        PlatformConfig result = platformConfigService.updateConfig(validConfig.getConfigKey(), updatedConfig);

        // Then
        assertNotNull(result);
        verify(platformConfigRepository).findByConfigKey(validConfig.getConfigKey());
        verify(platformConfigRepository).save(any(PlatformConfig.class));
        verify(configValidator).validate(any(PlatformConfig.class));
    }

    @Test
    @DisplayName("시스템 설정 수정 실패")
    void shouldFailToUpdateSystemConfig() {
        // Given
        PlatformConfig updatedConfig = PlatformConfig.builder()
                .configValue("updated value")
                .configType(PlatformConfig.ConfigType.STRING)
                .build();

        when(platformConfigRepository.findByConfigKey(anyString())).thenReturn(Optional.of(systemConfig));

        // When & Then
        ConfigValidationException exception = assertThrows(ConfigValidationException.class,
                () -> platformConfigService.updateConfig(systemConfig.getConfigKey(), updatedConfig));
        assertEquals(PlatformConfigErrorCode.SYSTEM_CONFIG_CANNOT_MODIFY, exception.getErrorCode());
        verify(platformConfigRepository).findByConfigKey(systemConfig.getConfigKey());
        verify(platformConfigRepository, never()).save(any(PlatformConfig.class));
    }

    @Test
    @DisplayName("유효한 설정 삭제 성공")
    void shouldDeleteValidConfig() {
        // Given
        when(platformConfigRepository.findByConfigKey(anyString())).thenReturn(Optional.of(validConfig));
        when(platformConfigRepository.save(any(PlatformConfig.class))).thenReturn(validConfig);

        // When
        platformConfigService.deleteConfig(validConfig.getConfigKey());

        // Then
        verify(platformConfigRepository).findByConfigKey(validConfig.getConfigKey());
        verify(platformConfigRepository).save(any(PlatformConfig.class));
        assertTrue(validConfig.getIsDeleted());
    }

    @Test
    @DisplayName("시스템 설정 삭제 실패")
    void shouldFailToDeleteSystemConfig() {
        // Given
        when(platformConfigRepository.findByConfigKey(anyString())).thenReturn(Optional.of(systemConfig));

        // When & Then
        ConfigValidationException exception = assertThrows(ConfigValidationException.class,
                () -> platformConfigService.deleteConfig(systemConfig.getConfigKey()));
        assertEquals(PlatformConfigErrorCode.SYSTEM_CONFIG_CANNOT_DELETE, exception.getErrorCode());
        verify(platformConfigRepository).findByConfigKey(systemConfig.getConfigKey());
        verify(platformConfigRepository, never()).save(any(PlatformConfig.class));
    }

    @Test
    @DisplayName("시스템 설정 하드 삭제 실패")
    void shouldFailToHardDeleteSystemConfig() {
        // Given
        when(platformConfigRepository.findByConfigKey(anyString())).thenReturn(Optional.of(systemConfig));

        // When & Then
        ConfigValidationException exception = assertThrows(ConfigValidationException.class,
                () -> platformConfigService.hardDeleteConfig(systemConfig.getConfigKey()));
        assertEquals(PlatformConfigErrorCode.SYSTEM_CONFIG_CANNOT_DELETE, exception.getErrorCode());
        verify(platformConfigRepository).findByConfigKey(systemConfig.getConfigKey());
        verify(platformConfigRepository, never()).delete(any(PlatformConfig.class));
    }

    @Test
    @DisplayName("설정 검증 실패 시 예외 발생")
    void shouldThrowExceptionWhenValidationFails() {
        // Given
        doThrow(new ConfigValidationException(PlatformConfigErrorCode.CONFIG_KEY_REQUIRED))
                .when(configValidator).validate(any(PlatformConfig.class));

        // When & Then
        ConfigValidationException exception = assertThrows(ConfigValidationException.class,
                () -> platformConfigService.createConfig(validConfig));
        assertEquals(PlatformConfigErrorCode.CONFIG_KEY_REQUIRED, exception.getErrorCode());
        verify(configValidator).validate(validConfig);
        verify(platformConfigRepository, never()).save(any(PlatformConfig.class));
    }
}
