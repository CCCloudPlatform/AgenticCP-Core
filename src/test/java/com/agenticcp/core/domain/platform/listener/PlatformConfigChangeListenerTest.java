package com.agenticcp.core.domain.platform.listener;

import com.agenticcp.core.domain.platform.event.ConfigChangeEvent;
import com.agenticcp.core.domain.platform.service.PlatformConfigRuntimeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * PlatformConfigChangeListener 단위 테스트
 *
 * @author AgenticCP Team
 * @since 2025-10-19
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PlatformConfigChangeListener 테스트")
class PlatformConfigChangeListenerTest {

    @Mock
    private PlatformConfigRuntimeService platformConfigRuntimeService;

    @InjectMocks
    private PlatformConfigChangeListener listener;

    @Test
    @DisplayName("maintenance_mode 설정 변경 시 유지보수 모드 활성화")
    void shouldEnableMaintenanceModeWhenConfigChanged() {
        // Given
        ConfigChangeEvent event = ConfigChangeEvent.create(
                "maintenance_mode", "true", "admin", "Emergency maintenance"
        );

        // When
        listener.handleConfigChange(event);

        // Then
        verify(platformConfigRuntimeService).enableMaintenanceMode();
        verify(platformConfigRuntimeService, never()).disableMaintenanceMode();
    }

    @Test
    @DisplayName("maintenance_mode 설정 변경 시 유지보수 모드 비활성화")
    void shouldDisableMaintenanceModeWhenConfigChanged() {
        // Given
        ConfigChangeEvent event = ConfigChangeEvent.create(
                "maintenance_mode", "false", "admin", "Maintenance completed"
        );

        // When
        listener.handleConfigChange(event);

        // Then
        verify(platformConfigRuntimeService).disableMaintenanceMode();
        verify(platformConfigRuntimeService, never()).enableMaintenanceMode();
    }

    @Test
    @DisplayName("maintenance_mode 설정 변경 시 숫자 값으로 활성화")
    void shouldEnableMaintenanceModeWithNumericValue() {
        // Given
        ConfigChangeEvent event = ConfigChangeEvent.create(
                "maintenance_mode", "1", "admin", "Maintenance mode on"
        );

        // When
        listener.handleConfigChange(event);

        // Then
        verify(platformConfigRuntimeService).enableMaintenanceMode();
    }

    @Test
    @DisplayName("maintenance_mode 설정 변경 시 숫자 값으로 비활성화")
    void shouldDisableMaintenanceModeWithNumericValue() {
        // Given
        ConfigChangeEvent event = ConfigChangeEvent.create(
                "maintenance_mode", "0", "admin", "Maintenance mode off"
        );

        // When
        listener.handleConfigChange(event);

        // Then
        verify(platformConfigRuntimeService).disableMaintenanceMode();
    }

    @Test
    @DisplayName("cache_ttl 설정 변경 시 캐시 설정 업데이트")
    void shouldUpdateCacheSettingsWhenCacheTtlChanged() {
        // Given
        ConfigChangeEvent event = ConfigChangeEvent.create(
                "cache_ttl", "3600", "admin", "Update cache TTL"
        );

        // When
        listener.handleConfigChange(event);

        // Then
        verify(platformConfigRuntimeService).updateCacheSettings("cache_ttl", "3600");
    }

    @Test
    @DisplayName("cache_max_size 설정 변경 시 캐시 설정 업데이트")
    void shouldUpdateCacheSettingsWhenCacheMaxSizeChanged() {
        // Given
        ConfigChangeEvent event = ConfigChangeEvent.create(
                "cache_max_size", "10000", "admin", "Update cache max size"
        );

        // When
        listener.handleConfigChange(event);

        // Then
        verify(platformConfigRuntimeService).updateCacheSettings("cache_max_size", "10000");
    }

    @Test
    @DisplayName("security_session_timeout 설정 변경 시 보안 설정 업데이트")
    void shouldUpdateSecuritySettingsWhenSessionTimeoutChanged() {
        // Given
        ConfigChangeEvent event = ConfigChangeEvent.create(
                "security_session_timeout", "1800", "admin", "Update session timeout"
        );

        // When
        listener.handleConfigChange(event);

        // Then
        verify(platformConfigRuntimeService).updateSecuritySettings("security_session_timeout", "1800");
    }

    @Test
    @DisplayName("security_max_login_attempts 설정 변경 시 보안 설정 업데이트")
    void shouldUpdateSecuritySettingsWhenMaxLoginAttemptsChanged() {
        // Given
        ConfigChangeEvent event = ConfigChangeEvent.create(
                "security_max_login_attempts", "5", "admin", "Update max login attempts"
        );

        // When
        listener.handleConfigChange(event);

        // Then
        verify(platformConfigRuntimeService).updateSecuritySettings("security_max_login_attempts", "5");
    }

    @Test
    @DisplayName("logging_level 설정 변경 시 로깅 설정 업데이트")
    void shouldUpdateLoggingSettingsWhenLoggingLevelChanged() {
        // Given
        ConfigChangeEvent event = ConfigChangeEvent.create(
                "logging_level", "DEBUG", "admin", "Update logging level"
        );

        // When
        listener.handleConfigChange(event);

        // Then
        verify(platformConfigRuntimeService).updateLoggingSettings("logging_level", "DEBUG");
    }

    @Test
    @DisplayName("알 수 없는 설정 키는 특별 처리하지 않음")
    void shouldNotHandleUnknownConfigKey() {
        // Given
        ConfigChangeEvent event = ConfigChangeEvent.create(
                "unknown_config", "some_value", "admin", "Unknown config"
        );

        // When
        listener.handleConfigChange(event);

        // Then
        verify(platformConfigRuntimeService, never()).enableMaintenanceMode();
        verify(platformConfigRuntimeService, never()).disableMaintenanceMode();
        verify(platformConfigRuntimeService, never()).updateCacheSettings(anyString(), anyString());
        verify(platformConfigRuntimeService, never()).updateSecuritySettings(anyString(), anyString());
        verify(platformConfigRuntimeService, never()).updateLoggingSettings(anyString(), anyString());
    }

    @Test
    @DisplayName("maintenance_mode 잘못된 값은 처리하지 않음")
    void shouldNotHandleInvalidMaintenanceModeValue() {
        // Given
        ConfigChangeEvent event = ConfigChangeEvent.create(
                "maintenance_mode", "invalid", "admin", "Invalid value"
        );

        // When
        listener.handleConfigChange(event);

        // Then
        verify(platformConfigRuntimeService, never()).enableMaintenanceMode();
        verify(platformConfigRuntimeService, never()).disableMaintenanceMode();
    }

    @Test
    @DisplayName("설정 변경 이벤트 수신 시 런타임 적용 호출 검증")
    void shouldCallRuntimeServiceWhenConfigChangeEventReceived() {
        // Given
        ConfigChangeEvent event = ConfigChangeEvent.create(
                "maintenance_mode", "true", "admin", "Test maintenance mode"
        );

        // When
        listener.handleConfigChange(event);

        // Then
        verify(platformConfigRuntimeService, times(1)).enableMaintenanceMode();
    }
}
