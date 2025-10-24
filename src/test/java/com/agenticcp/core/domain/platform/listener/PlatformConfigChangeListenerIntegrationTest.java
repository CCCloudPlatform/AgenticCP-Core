package com.agenticcp.core.domain.platform.listener;

import com.agenticcp.core.domain.platform.entity.PlatformConfig;
import com.agenticcp.core.domain.platform.service.PlatformConfigService;
import com.agenticcp.core.domain.platform.service.PlatformConfigRuntimeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * PlatformConfigChangeListener 통합 테스트
 * 
 * 실제 이벤트 발행과 수신이 연동되는지 확인합니다.
 *
 * @author AgenticCP Team
 * @since 2025-10-19
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Disabled("Integration test disabled")
@DisplayName("PlatformConfigChangeListener 통합 테스트")
class PlatformConfigChangeListenerIntegrationTest {

    @Autowired
    private PlatformConfigService platformConfigService;

    @MockBean
    private PlatformConfigRuntimeService platformConfigRuntimeService;

    @Test
    @DisplayName("설정 생성 시 이벤트가 발행되고 리스너가 처리함")
    void shouldPublishEventAndHandleByListenerWhenConfigCreated() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("system.cache_ttl")
                .configValue("3600")
                .configType(PlatformConfig.ConfigType.STRING)
                .isEncrypted(false)
                .description("Test cache TTL")
                .build();

        // When
        platformConfigService.createConfig(config);

        // Then
        verify(platformConfigRuntimeService, timeout(1000)).updateCacheSettings(eq("system.cache_ttl"), anyString());
    }

    @Test
    @DisplayName("설정 수정 시 이벤트가 발행되고 리스너가 처리함")
    void shouldPublishEventAndHandleByListenerWhenConfigUpdated() {
        // Given
        PlatformConfig existingConfig = PlatformConfig.builder()
                .configKey("system.cache_ttl")
                .configValue("1800")
                .configType(PlatformConfig.ConfigType.STRING)
                .isEncrypted(false)
                .description("Cache TTL")
                .build();

        PlatformConfig updatedConfig = PlatformConfig.builder()
                .configValue("3600")
                .configType(PlatformConfig.ConfigType.STRING)
                .isEncrypted(false)
                .build();

        // 먼저 설정 생성
        platformConfigService.createConfig(existingConfig);
        reset(platformConfigRuntimeService); // Mock 초기화

        // When
        platformConfigService.updateConfig("system.cache_ttl", updatedConfig);

        // Then
        verify(platformConfigRuntimeService, timeout(1000)).updateCacheSettings(eq("system.cache_ttl"), anyString());
    }

    @Test
    @DisplayName("보안 설정 변경 시 이벤트가 발행되고 리스너가 처리함")
    void shouldPublishEventAndHandleByListenerWhenSecurityConfigChanged() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("system.security_session_timeout")
                .configValue("1800")
                .configType(PlatformConfig.ConfigType.STRING)
                .isEncrypted(false)
                .description("Session timeout")
                .build();

        // When
        platformConfigService.createConfig(config);

        // Then
        verify(platformConfigRuntimeService, timeout(1000)).updateSecuritySettings(eq("system.security_session_timeout"), anyString());
    }

    @Test
    @DisplayName("로깅 설정 변경 시 이벤트가 발행되고 리스너가 처리함")
    void shouldPublishEventAndHandleByListenerWhenLoggingConfigChanged() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("system.logging_level")
                .configValue("DEBUG")
                .configType(PlatformConfig.ConfigType.STRING)
                .isEncrypted(false)
                .description("Logging level")
                .build();

        // When
        platformConfigService.createConfig(config);

        // Then
        verify(platformConfigRuntimeService, timeout(1000)).updateLoggingSettings(eq("system.logging_level"), anyString());
    }

    @Test
    @DisplayName("알 수 없는 설정은 리스너에서 특별 처리하지 않음")
    void shouldNotHandleUnknownConfigByListener() {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("system.unknown_setting")
                .configValue("some_value")
                .configType(PlatformConfig.ConfigType.STRING)
                .isEncrypted(false)
                .description("Unknown setting")
                .build();

        // When
        platformConfigService.createConfig(config);

        // Then
        verify(platformConfigRuntimeService, never()).enableMaintenanceMode();
        verify(platformConfigRuntimeService, never()).disableMaintenanceMode();
        verify(platformConfigRuntimeService, never()).updateCacheSettings(anyString(), anyString());
        verify(platformConfigRuntimeService, never()).updateSecuritySettings(anyString(), anyString());
        verify(platformConfigRuntimeService, never()).updateLoggingSettings(anyString(), anyString());
    }
}
