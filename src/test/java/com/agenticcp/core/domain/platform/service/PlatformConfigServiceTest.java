package com.agenticcp.core.domain.platform.service;

import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.common.exception.ValidationException;
import com.agenticcp.core.domain.platform.entity.PlatformConfig;
import com.agenticcp.core.domain.platform.repository.PlatformConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PlatformConfigService 단위 테스트
 * - 비즈니스 로직 검증
 * - 예외 처리 테스트
 * - Mock을 활용한 의존성 격리
 */
@ExtendWith(MockitoExtension.class)
class PlatformConfigServiceTest {

    @Mock
    private PlatformConfigRepository platformConfigRepository;

    @InjectMocks
    private PlatformConfigService platformConfigService;

    private PlatformConfig testConfig;
    private PlatformConfig systemConfig;

    @BeforeEach
    void setUp() {
        testConfig = PlatformConfig.builder()
                .configKey("test.key")
                .configValue("test value")
                .configType(PlatformConfig.ConfigType.STRING)
                .description("Test configuration")
                .isSystem(false)
                .isEncrypted(false)
                .build();

        systemConfig = PlatformConfig.builder()
                .configKey("system.key")
                .configValue("system value")
                .configType(PlatformConfig.ConfigType.STRING)
                .description("System configuration")
                .isSystem(true)
                .isEncrypted(false)
                .build();
    }

    @Test
    @DisplayName("전체 설정 페이징 조회")
    void getAllConfigs_Success() {
        // Given
        List<PlatformConfig> configs = Arrays.asList(testConfig, systemConfig);
        Page<PlatformConfig> page = new PageImpl<>(configs);
        Pageable pageable = PageRequest.of(0, 10);

        when(platformConfigRepository.findAll(pageable)).thenReturn(page);

        // When
        Page<PlatformConfig> result = platformConfigService.getAllConfigs(pageable);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        verify(platformConfigRepository).findAll(pageable);
    }

    @Test
    @DisplayName("키로 설정 조회 - 성공")
    void getConfigByKey_Success() {
        // Given
        when(platformConfigRepository.findByConfigKey("test.key")).thenReturn(Optional.of(testConfig));

        // When
        Optional<PlatformConfig> result = platformConfigService.getConfigByKey("test.key");

        // Then
        assertTrue(result.isPresent());
        assertEquals("test.key", result.get().getConfigKey());
        verify(platformConfigRepository).findByConfigKey("test.key");
    }

    @Test
    @DisplayName("키로 설정 조회 - 존재하지 않는 키")
    void getConfigByKey_NotFound() {
        // Given
        when(platformConfigRepository.findByConfigKey("nonexistent.key")).thenReturn(Optional.empty());

        // When
        Optional<PlatformConfig> result = platformConfigService.getConfigByKey("nonexistent.key");

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("키로 설정 조회(예외) - 존재하지 않는 키")
    void getConfigByKeyOrThrow_NotFound() {
        // Given
        when(platformConfigRepository.findByConfigKey("nonexistent.key")).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
            () -> platformConfigService.getConfigByKeyOrThrow("nonexistent.key"));
        
        assertTrue(exception.getMessage().contains("PlatformConfig"));
        assertTrue(exception.getMessage().contains("nonexistent.key"));
    }

    @Test
    @DisplayName("타입별 설정 조회")
    void getConfigsByType_Success() {
        // Given
        List<PlatformConfig> configs = Arrays.asList(testConfig);
        Page<PlatformConfig> page = new PageImpl<>(configs);
        Pageable pageable = PageRequest.of(0, 10);

        when(platformConfigRepository.findByConfigType(
            PlatformConfig.ConfigType.STRING, pageable)).thenReturn(page);

        // When
        Page<PlatformConfig> result = platformConfigService.getConfigsByType(
            PlatformConfig.ConfigType.STRING, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(platformConfigRepository).findByConfigType(
            PlatformConfig.ConfigType.STRING, pageable);
    }

    @Test
    @DisplayName("시스템 설정 조회")
    void getSystemConfigs_Success() {
        // Given
        List<PlatformConfig> configs = Arrays.asList(systemConfig);
        Page<PlatformConfig> page = new PageImpl<>(configs);
        Pageable pageable = PageRequest.of(0, 10);

        when(platformConfigRepository.findByIsSystem(true, pageable)).thenReturn(page);

        // When
        Page<PlatformConfig> result = platformConfigService.getSystemConfigs(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertTrue(result.getContent().get(0).getIsSystem());
        verify(platformConfigRepository).findByIsSystem(true, pageable);
    }

    @Test
    @DisplayName("설정 생성 - 성공")
    void createConfig_Success() {
        // Given
        when(platformConfigRepository.save(any(PlatformConfig.class))).thenReturn(testConfig);

        // When
        PlatformConfig result = platformConfigService.createConfig(testConfig);

        // Then
        assertNotNull(result);
        assertEquals("test.key", result.getConfigKey());
        verify(platformConfigRepository).save(testConfig);
    }

    @Test
    @DisplayName("설정 생성 - 잘못된 키")
    void createConfig_InvalidKey() {
        // Given
        PlatformConfig invalidConfig = PlatformConfig.builder()
                .configKey("ab") // 너무 짧은 키
                .configValue("test")
                .configType(PlatformConfig.ConfigType.STRING)
                .build();

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class,
            () -> platformConfigService.createConfig(invalidConfig));
        
        assertTrue(exception.getMessage().contains("length must be between 3 and 128"));
        verify(platformConfigRepository, never()).save(any());
    }

    @Test
    @DisplayName("설정 생성 - 잘못된 값 타입")
    void createConfig_InvalidValueType() {
        // Given
        PlatformConfig invalidConfig = PlatformConfig.builder()
                .configKey("test.key")
                .configValue("not-a-number")
                .configType(PlatformConfig.ConfigType.NUMBER)
                .build();

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class,
            () -> platformConfigService.createConfig(invalidConfig));
        
        assertTrue(exception.getMessage().contains("must be a valid number"));
        verify(platformConfigRepository, never()).save(any());
    }

    @Test
    @DisplayName("설정 생성 - ENCRYPTED 빈 값")
    void createConfig_EncryptedEmptyValue() {
        // Given
        PlatformConfig invalidConfig = PlatformConfig.builder()
                .configKey("test.key")
                .configValue("")
                .configType(PlatformConfig.ConfigType.ENCRYPTED)
                .build();

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class,
            () -> platformConfigService.createConfig(invalidConfig));
        
        assertTrue(exception.getMessage().contains("encrypted value must not be blank"));
        verify(platformConfigRepository, never()).save(any());
    }

    @Test
    @DisplayName("설정 수정 - 성공")
    void updateConfig_Success() {
        // Given
        PlatformConfig existingConfig = PlatformConfig.builder()
                .configKey("test.key")
                .configValue("old value")
                .configType(PlatformConfig.ConfigType.STRING)
                .build();

        PlatformConfig updatedConfig = PlatformConfig.builder()
                .configValue("new value")
                .configType(PlatformConfig.ConfigType.STRING)
                .description("Updated description")
                .isEncrypted(false)
                .build();

        when(platformConfigRepository.findByConfigKey("test.key")).thenReturn(Optional.of(existingConfig));
        when(platformConfigRepository.save(any(PlatformConfig.class))).thenReturn(existingConfig);

        // When
        PlatformConfig result = platformConfigService.updateConfig("test.key", updatedConfig);

        // Then
        assertNotNull(result);
        verify(platformConfigRepository).findByConfigKey("test.key");
        verify(platformConfigRepository).save(existingConfig);
    }

    @Test
    @DisplayName("설정 수정 - 존재하지 않는 키")
    void updateConfig_NotFound() {
        // Given
        PlatformConfig updatedConfig = PlatformConfig.builder()
                .configValue("new value")
                .configType(PlatformConfig.ConfigType.STRING)
                .build();

        when(platformConfigRepository.findByConfigKey("nonexistent.key")).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
            () -> platformConfigService.updateConfig("nonexistent.key", updatedConfig));
        
        assertTrue(exception.getMessage().contains("nonexistent.key"));
        verify(platformConfigRepository, never()).save(any());
    }

    @Test
    @DisplayName("설정 삭제 - 성공")
    void deleteConfig_Success() {
        // Given
        when(platformConfigRepository.findByConfigKey("test.key")).thenReturn(Optional.of(testConfig));
        when(platformConfigRepository.save(any(PlatformConfig.class))).thenReturn(testConfig);

        // When
        platformConfigService.deleteConfig("test.key");

        // Then
        verify(platformConfigRepository).findByConfigKey("test.key");
        verify(platformConfigRepository).save(testConfig);
        assertTrue(testConfig.getIsDeleted());
    }

    @Test
    @DisplayName("설정 삭제 - 시스템 설정 삭제 금지")
    void deleteConfig_SystemConfigNotAllowed() {
        // Given
        when(platformConfigRepository.findByConfigKey("system.key")).thenReturn(Optional.of(systemConfig));

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class,
            () -> platformConfigService.deleteConfig("system.key"));
        
        assertTrue(exception.getMessage().contains("system config cannot be deleted"));
        verify(platformConfigRepository, never()).save(any());
    }

    @Test
    @DisplayName("설정 삭제 - 존재하지 않는 키")
    void deleteConfig_NotFound() {
        // Given
        when(platformConfigRepository.findByConfigKey("nonexistent.key")).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
            () -> platformConfigService.deleteConfig("nonexistent.key"));
        
        assertTrue(exception.getMessage().contains("nonexistent.key"));
        verify(platformConfigRepository, never()).save(any());
    }

    @Test
    @DisplayName("하드 삭제 - 성공")
    void hardDeleteConfig_Success() {
        // Given
        when(platformConfigRepository.findByConfigKey("test.key")).thenReturn(Optional.of(testConfig));

        // When
        platformConfigService.hardDeleteConfig("test.key");

        // Then
        verify(platformConfigRepository).findByConfigKey("test.key");
        verify(platformConfigRepository).delete(testConfig);
    }
}
