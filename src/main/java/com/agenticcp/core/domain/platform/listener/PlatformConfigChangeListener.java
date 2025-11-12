package com.agenticcp.core.domain.platform.listener;

import com.agenticcp.core.domain.platform.event.ConfigChangeEvent;
import com.agenticcp.core.domain.platform.service.PlatformConfigRuntimeService;
import com.agenticcp.core.common.util.LogMaskingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 플랫폼 설정 변경 이벤트 리스너
 * 
 * ConfigChangeEvent를 구독하여 설정 변경을 실시간으로 감지하고
 * 핵심 설정 변경 시 런타임에 즉시 적용하는 역할을 담당합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlatformConfigChangeListener {

    private final PlatformConfigRuntimeService platformConfigRuntimeService;

    /**
     * 설정 변경 이벤트 처리
     * 
     * @param event 설정 변경 이벤트
     */
    @EventListener
    public void handleConfigChange(ConfigChangeEvent event) {
        log.info("[PlatformConfigChangeListener] handleConfigChange - configKey={}, changeType={}", 
                LogMaskingUtils.mask(event.getConfigKey(), 2, 2), event.getChangeType());

        try {
            // 핵심 설정 키별 처리
            switch (event.getConfigKey()) {
                case "system.maintenance_mode":
                    handleMaintenanceModeChange(event);
                    break;
                case "system.cache_ttl":
                case "system.cache_max_size":
                    handleCacheConfigChange(event);
                    break;
                case "system.security_session_timeout":
                case "system.security_max_login_attempts":
                    handleSecurityConfigChange(event);
                    break;
                case "system.logging_level":
                    handleLoggingConfigChange(event);
                    break;
                default:
                    log.debug("[PlatformConfigChangeListener] No special handling for configKey: {}", 
                            LogMaskingUtils.mask(event.getConfigKey(), 2, 2));
                    break;
            }
        } catch (Exception e) {
            log.error("[PlatformConfigChangeListener] Failed to handle config change: configKey={}, error={}", 
                    LogMaskingUtils.mask(event.getConfigKey(), 2, 2), e.getMessage(), e);
        }
    }

    /**
     * 유지보수 모드 설정 변경 처리
     * 
     * @param event 설정 변경 이벤트
     */
    private void handleMaintenanceModeChange(ConfigChangeEvent event) {
        log.info("[PlatformConfigChangeListener] handleMaintenanceModeChange - newValue={}", 
                event.getNewValueMasked());

        try {
            // 실제 값을 사용하여 유지보수 모드 상태 결정
            String newValue = event.getNewValueRaw();
            if ("true".equals(newValue) || "1".equals(newValue)) {
                platformConfigRuntimeService.enableMaintenanceMode();
                log.info("[PlatformConfigChangeListener] Maintenance mode enabled");
            } else if ("false".equals(newValue) || "0".equals(newValue)) {
                platformConfigRuntimeService.disableMaintenanceMode();
                log.info("[PlatformConfigChangeListener] Maintenance mode disabled");
            } else {
                log.warn("[PlatformConfigChangeListener] Invalid maintenance_mode value: {}", newValue);
            }
        } catch (Exception e) {
            log.error("[PlatformConfigChangeListener] Failed to apply maintenance mode change", e);
        }
    }

    /**
     * 캐시 설정 변경 처리
     * 
     * @param event 설정 변경 이벤트
     */
    private void handleCacheConfigChange(ConfigChangeEvent event) {
        log.info("[PlatformConfigChangeListener] handleCacheConfigChange - configKey={}, newValue={}", 
                LogMaskingUtils.mask(event.getConfigKey(), 2, 2), event.getNewValueMasked());

        try {
            platformConfigRuntimeService.updateCacheSettings(event.getConfigKey(), event.getNewValueMasked());
            log.info("[PlatformConfigChangeListener] Cache settings updated for key: {}", 
                    LogMaskingUtils.mask(event.getConfigKey(), 2, 2));
        } catch (Exception e) {
            log.error("[PlatformConfigChangeListener] Failed to update cache settings", e);
        }
    }

    /**
     * 보안 설정 변경 처리
     * 
     * @param event 설정 변경 이벤트
     */
    private void handleSecurityConfigChange(ConfigChangeEvent event) {
        log.info("[PlatformConfigChangeListener] handleSecurityConfigChange - configKey={}, newValue={}", 
                LogMaskingUtils.mask(event.getConfigKey(), 2, 2), event.getNewValueMasked());

        try {
            platformConfigRuntimeService.updateSecuritySettings(event.getConfigKey(), event.getNewValueMasked());
            log.info("[PlatformConfigChangeListener] Security settings updated for key: {}", 
                    LogMaskingUtils.mask(event.getConfigKey(), 2, 2));
        } catch (Exception e) {
            log.error("[PlatformConfigChangeListener] Failed to update security settings", e);
        }
    }

    /**
     * 로깅 설정 변경 처리
     * 
     * @param event 설정 변경 이벤트
     */
    private void handleLoggingConfigChange(ConfigChangeEvent event) {
        log.info("[PlatformConfigChangeListener] handleLoggingConfigChange - configKey={}, newValue={}", 
                LogMaskingUtils.mask(event.getConfigKey(), 2, 2), event.getNewValueMasked());

        try {
            platformConfigRuntimeService.updateLoggingSettings(event.getConfigKey(), event.getNewValueMasked());
            log.info("[PlatformConfigChangeListener] Logging settings updated for key: {}", 
                    LogMaskingUtils.mask(event.getConfigKey(), 2, 2));
        } catch (Exception e) {
            log.error("[PlatformConfigChangeListener] Failed to update logging settings", e);
        }
    }
}
