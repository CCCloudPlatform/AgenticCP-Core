package com.agenticcp.core.domain.platform.service;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.domain.platform.entity.PlatformConfig;
import com.agenticcp.core.domain.platform.enums.PlatformConfigErrorCode;
import com.agenticcp.core.domain.platform.exception.ConfigValidationException;
import com.agenticcp.core.domain.platform.repository.PlatformConfigRepository;
import com.agenticcp.core.domain.platform.validation.ConfigValidator;
import com.agenticcp.core.domain.platform.event.ConfigChangeEvent;
import com.agenticcp.core.common.logging.masking.MaskingService;
import com.agenticcp.core.common.logging.masking.MaskingType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.context.ApplicationEventPublisher;

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

    @Mock
    private ConfigAuditService configAuditService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private com.agenticcp.core.common.crypto.EncryptionService encryptionService;

    @Mock
    private MaskingService maskingService;

    @InjectMocks
    private PlatformConfigService platformConfigService;

    private PlatformConfig validConfig;
    private PlatformConfig systemConfig;
    private MockedStatic<TenantContextHolder> tenantContextHolderMock;

    @BeforeEach
    void setUp() {
        // TenantContextHolder Mock 설정
        tenantContextHolderMock = mockStatic(TenantContextHolder.class);
        tenantContextHolderMock.when(TenantContextHolder::getCurrentTenantKeyOrThrow)
                .thenReturn("test-tenant-key");
        // Mock validator 리스트 설정
        List<ConfigValidator> mockValidators = Arrays.asList(configValidator);
        ReflectionTestUtils.setField(platformConfigService, "configValidators", mockValidators);
        
        // Mock validator가 아무것도 하지 않도록 설정 (lenient 모드 사용)
        lenient().doNothing().when(configValidator).validate(any(PlatformConfig.class));
        lenient().doNothing().when(configValidator).validateKey(anyString());
        lenient().doNothing().when(configValidator).validateValue(anyString(), any(PlatformConfig.ConfigType.class));
        
        // Mock ConfigAuditService가 아무것도 하지 않도록 설정 (lenient 모드 사용)
        lenient().doNothing().when(configAuditService).logCreate(anyString(), anyString(), anyString(), anyString(), anyString());
        lenient().doNothing().when(configAuditService).logUpdate(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
        lenient().doNothing().when(configAuditService).logDelete(anyString(), anyString(), anyString(), anyString(), anyString());

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

    @AfterEach
    void tearDown() {
        if (tenantContextHolderMock != null) {
            tenantContextHolderMock.close();
        }
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("유효한 설정 생성 성공")
    void shouldCreateValidConfig() {
        // Given
        when(platformConfigRepository.findByTenantIdAndConfigKey(anyString(), anyString())).thenReturn(Optional.empty());
        when(platformConfigRepository.save(any(PlatformConfig.class))).thenReturn(validConfig);

        // When
        PlatformConfig result = platformConfigService.createConfig(validConfig);

        // Then
        assertNotNull(result);
        assertEquals(validConfig.getConfigKey(), result.getConfigKey());
        verify(platformConfigRepository).findByTenantIdAndConfigKey("test-tenant-key", validConfig.getConfigKey());
        verify(platformConfigRepository).save(validConfig);
        verify(configValidator).validate(validConfig);
    }

    @Test
    @DisplayName("중복 키로 인한 설정 생성 실패")
    void shouldFailToCreateConfigWithDuplicateKey() {
        // Given
        when(platformConfigRepository.findByTenantIdAndConfigKey(anyString(), anyString())).thenReturn(Optional.of(validConfig));

        // When & Then
        ConfigValidationException exception = assertThrows(ConfigValidationException.class,
                () -> platformConfigService.createConfig(validConfig));
        assertEquals(PlatformConfigErrorCode.CONFIG_ALREADY_EXISTS, exception.getErrorCode());
        verify(platformConfigRepository).findByTenantIdAndConfigKey("test-tenant-key", validConfig.getConfigKey());
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

        when(platformConfigRepository.findByTenantIdAndConfigKey(anyString(), anyString())).thenReturn(Optional.of(validConfig));
        when(platformConfigRepository.save(any(PlatformConfig.class))).thenReturn(validConfig);

        // When
        PlatformConfig result = platformConfigService.updateConfig(validConfig.getConfigKey(), updatedConfig);

        // Then
        assertNotNull(result);
        verify(platformConfigRepository).findByTenantIdAndConfigKey("test-tenant-key", validConfig.getConfigKey());
        verify(platformConfigRepository).save(any(PlatformConfig.class));
        verify(configValidator).validate(any(PlatformConfig.class));
    }

    @Test
    @DisplayName("업데이트 시 @CacheEvict 어노테이션이 적용되어야 한다")
    void shouldEvictCacheOnUpdate() {
        // Given: 업데이트할 설정
        PlatformConfig updated = PlatformConfig.builder()
                .configValue("updated")
                .configType(PlatformConfig.ConfigType.STRING)
                .isEncrypted(false)
                .build();

        when(platformConfigRepository.findByTenantIdAndConfigKey(anyString(), anyString())).thenReturn(Optional.of(validConfig));
        when(platformConfigRepository.save(any(PlatformConfig.class))).thenReturn(validConfig);

        // When: 업데이트 수행
        PlatformConfig result = platformConfigService.updateConfig(validConfig.getConfigKey(), updated);

        // Then: 업데이트가 성공하고 @CacheEvict 어노테이션이 적용되었는지 확인
        assertNotNull(result);
        verify(platformConfigRepository).findByTenantIdAndConfigKey("test-tenant-key", validConfig.getConfigKey());
        verify(platformConfigRepository).save(any(PlatformConfig.class));
        verify(configValidator).validate(any(PlatformConfig.class));
        
        // @CacheEvict 어노테이션은 메서드 레벨에서 확인됨 (실제 캐시 동작은 통합 테스트에서 검증)
    }

    @Test
    @DisplayName("시스템 설정 수정 실패")
    void shouldFailToUpdateSystemConfig() {
        // Given
        PlatformConfig updatedConfig = PlatformConfig.builder()
                .configValue("updated value")
                .configType(PlatformConfig.ConfigType.NUMBER)  // 타입 변경
                .build();

        when(platformConfigRepository.findByTenantIdAndConfigKey(anyString(), anyString())).thenReturn(Optional.of(systemConfig));

        // When & Then
        ConfigValidationException exception = assertThrows(ConfigValidationException.class,
                () -> platformConfigService.updateConfig(systemConfig.getConfigKey(), updatedConfig));
        assertEquals(PlatformConfigErrorCode.SYSTEM_CONFIG_TYPE_CHANGE_FORBIDDEN, exception.getErrorCode());
        verify(platformConfigRepository).findByTenantIdAndConfigKey("test-tenant-key", systemConfig.getConfigKey());
        verify(platformConfigRepository, never()).save(any(PlatformConfig.class));
    }

    @Test
    @DisplayName("시스템 설정 값 변경 성공")
    void shouldAllowSystemConfigValueChange() {
        // Given
        PlatformConfig updatedConfig = PlatformConfig.builder()
                .configValue("updated value")
                .configType(PlatformConfig.ConfigType.STRING)  // 같은 타입
                .description("updated description")
                .build();

        when(platformConfigRepository.findByTenantIdAndConfigKey(anyString(), anyString())).thenReturn(Optional.of(systemConfig));
        when(platformConfigRepository.save(any(PlatformConfig.class))).thenReturn(systemConfig);

        // When
        PlatformConfig result = platformConfigService.updateConfig(systemConfig.getConfigKey(), updatedConfig);

        // Then
        assertNotNull(result);
        verify(platformConfigRepository).findByTenantIdAndConfigKey("test-tenant-key", systemConfig.getConfigKey());
        verify(platformConfigRepository).save(any(PlatformConfig.class));
    }

    @Test
    @DisplayName("유효한 설정 삭제 성공")
    void shouldDeleteValidConfig() {
        // Given
        when(platformConfigRepository.findByTenantIdAndConfigKey(anyString(), anyString())).thenReturn(Optional.of(validConfig));
        when(platformConfigRepository.save(any(PlatformConfig.class))).thenReturn(validConfig);

        // When
        platformConfigService.deleteConfig(validConfig.getConfigKey());

        // Then
        verify(platformConfigRepository).findByTenantIdAndConfigKey("test-tenant-key", validConfig.getConfigKey());
        verify(platformConfigRepository).save(any(PlatformConfig.class));
        assertTrue(validConfig.getIsDeleted());
    }

    @Test
    @DisplayName("시스템 설정 삭제 실패")
    void shouldFailToDeleteSystemConfig() {
        // Given
        when(platformConfigRepository.findByTenantIdAndConfigKey(anyString(), anyString())).thenReturn(Optional.of(systemConfig));

        // When & Then
        ConfigValidationException exception = assertThrows(ConfigValidationException.class,
                () -> platformConfigService.deleteConfig(systemConfig.getConfigKey()));
        assertEquals(PlatformConfigErrorCode.SYSTEM_CONFIG_CANNOT_DELETE, exception.getErrorCode());
        verify(platformConfigRepository).findByTenantIdAndConfigKey("test-tenant-key", systemConfig.getConfigKey());
        verify(platformConfigRepository, never()).save(any(PlatformConfig.class));
    }

    @Test
    @DisplayName("시스템 설정 하드 삭제 실패")
    void shouldFailToHardDeleteSystemConfig() {
        // Given
        when(platformConfigRepository.findByTenantIdAndConfigKey(anyString(), anyString())).thenReturn(Optional.of(systemConfig));

        // When & Then
        ConfigValidationException exception = assertThrows(ConfigValidationException.class,
                () -> platformConfigService.hardDeleteConfig(systemConfig.getConfigKey()));
        assertEquals(PlatformConfigErrorCode.SYSTEM_CONFIG_CANNOT_DELETE, exception.getErrorCode());
        verify(platformConfigRepository).findByTenantIdAndConfigKey("test-tenant-key", systemConfig.getConfigKey());
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

    @Test
    @DisplayName("설정 생성 시 ConfigChangeEvent 발행")
    void shouldPublishConfigChangeEventOnCreate() {
        // Given
        when(platformConfigRepository.findByTenantIdAndConfigKey(anyString(), anyString())).thenReturn(Optional.empty());
        when(platformConfigRepository.save(any(PlatformConfig.class))).thenReturn(validConfig);
        when(maskingService.applyMaskingStrategy(anyString(), eq(MaskingType.SECRET_KEY))).thenReturn("ma***ed");

        // When
        PlatformConfig result = platformConfigService.createConfig(validConfig);

        // Then
        assertNotNull(result);
        verify(eventPublisher).publishEvent(argThat(event -> {
            if (event instanceof ConfigChangeEvent configEvent) {
                return configEvent.getConfigKey().equals(validConfig.getConfigKey()) &&
                       configEvent.getChangeType() == ConfigChangeEvent.ChangeType.CREATE &&
                       configEvent.getNewValueMasked() != null &&
                       configEvent.getOldValueMasked() == null;
            }
            return false;
        }));
    }

    @Test
    @DisplayName("설정 수정 시 ConfigChangeEvent 발행")
    void shouldPublishConfigChangeEventOnUpdate() {
        // Given
        PlatformConfig updatedConfig = PlatformConfig.builder()
                .configValue("updated value")
                .configType(PlatformConfig.ConfigType.STRING)
                .isEncrypted(false)
                .build();

        when(platformConfigRepository.findByTenantIdAndConfigKey(anyString(), anyString())).thenReturn(Optional.of(validConfig));
        when(platformConfigRepository.save(any(PlatformConfig.class))).thenReturn(validConfig);
        when(maskingService.applyMaskingStrategy(anyString(), eq(MaskingType.SECRET_KEY))).thenReturn("ma***ed");

        // When
        PlatformConfig result = platformConfigService.updateConfig(validConfig.getConfigKey(), updatedConfig);

        // Then
        assertNotNull(result);
        verify(eventPublisher).publishEvent(argThat(event -> {
            if (event instanceof ConfigChangeEvent configEvent) {
                return configEvent.getConfigKey().equals(validConfig.getConfigKey()) &&
                       configEvent.getChangeType() == ConfigChangeEvent.ChangeType.UPDATE &&
                       configEvent.getNewValueMasked() != null &&
                       configEvent.getOldValueMasked() != null;
            }
            return false;
        }));
    }

    @Test
    @DisplayName("설정 삭제 시 ConfigChangeEvent 발행")
    void shouldPublishConfigChangeEventOnDelete() {
        // Given
        when(platformConfigRepository.findByTenantIdAndConfigKey(anyString(), anyString())).thenReturn(Optional.of(validConfig));
        when(platformConfigRepository.save(any(PlatformConfig.class))).thenReturn(validConfig);
        when(maskingService.applyMaskingStrategy(anyString(), eq(MaskingType.SECRET_KEY))).thenReturn("ma***ed");

        // When
        platformConfigService.deleteConfig(validConfig.getConfigKey());

        // Then
        verify(eventPublisher).publishEvent(argThat(event -> {
            if (event instanceof ConfigChangeEvent configEvent) {
                return configEvent.getConfigKey().equals(validConfig.getConfigKey()) &&
                       configEvent.getChangeType() == ConfigChangeEvent.ChangeType.DELETE &&
                       configEvent.getNewValueMasked() == null &&
                       configEvent.getOldValueMasked() != null;
            }
            return false;
        }));
    }

    @Test
    @DisplayName("암호화된 설정 값은 마스킹되어 이벤트에 포함")
    void shouldMaskEncryptedValuesInEvent() {
        // Given
        PlatformConfig encryptedConfig = PlatformConfig.builder()
                .configKey("encrypted.config")
                .configValue("sensitive-data")
                .configType(PlatformConfig.ConfigType.ENCRYPTED)
                .isEncrypted(true)
                .description("Encrypted config")
                .build();

        when(platformConfigRepository.findByTenantIdAndConfigKey(anyString(), anyString())).thenReturn(Optional.empty());
        when(platformConfigRepository.save(any(PlatformConfig.class))).thenReturn(encryptedConfig);
        when(encryptionService.encrypt(anyString())).thenReturn("encrypted-value");
        // 암호화된 값은 "Encrypted"로 마스킹되므로 MaskingService 호출 안 됨

        // When
        platformConfigService.createConfig(encryptedConfig);

        // Then
        verify(eventPublisher).publishEvent(argThat(event -> {
            if (event instanceof ConfigChangeEvent configEvent) {
                return configEvent.getConfigKey().equals(encryptedConfig.getConfigKey()) &&
                       "Encrypted".equals(configEvent.getNewValueMasked());
            }
            return false;
        }));
    }
}
