package com.agenticcp.core.domain.platform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 플랫폼 설정 런타임 적용 서비스
 * 
 * 설정 변경 시 런타임에 즉시 적용되는 로직을 담당합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformConfigRuntimeService {

    private final CacheManagerService cacheManagerService;
    private final SecurityManagerService securityManagerService;
    private final LoggingManagerService loggingManagerService;
    private final MaintenanceModeService maintenanceModeService;

    /**
     * 유지보수 모드 활성화
     */
    public void enableMaintenanceMode() {
        log.info("[PlatformConfigRuntimeService] Enabling maintenance mode");
        maintenanceModeService.enable();
    }

    /**
     * 유지보수 모드 비활성화
     */
    public void disableMaintenanceMode() {
        log.info("[PlatformConfigRuntimeService] Disabling maintenance mode");
        maintenanceModeService.disable();
    }

    /**
     * 캐시 설정 업데이트
     * 
     * @param configKey 설정 키
     * @param configValue 설정 값
     */
    public void updateCacheSettings(String configKey, String configValue) {
        log.info("[PlatformConfigRuntimeService] Updating cache settings - key={}, value={}", 
                configKey, configValue);
        
        try {
            switch (configKey) {
                case "cache_ttl":
                    int ttl = Integer.parseInt(configValue);
                    cacheManagerService.updateCacheTtl(ttl);
                    break;
                case "cache_max_size":
                    int maxSize = Integer.parseInt(configValue);
                    cacheManagerService.updateCacheMaxSize(maxSize);
                    break;
                default:
                    log.warn("[PlatformConfigRuntimeService] Unknown cache config key: {}", configKey);
                    break;
            }
        } catch (NumberFormatException e) {
            log.error("[PlatformConfigRuntimeService] Invalid cache config value: key={}, value={}", 
                    configKey, configValue, e);
        }
    }

    /**
     * 보안 설정 업데이트
     * 
     * @param configKey 설정 키
     * @param configValue 설정 값
     */
    public void updateSecuritySettings(String configKey, String configValue) {
        log.info("[PlatformConfigRuntimeService] Updating security settings - key={}, value={}", 
                configKey, configValue);
        
        try {
            switch (configKey) {
                case "security_session_timeout":
                    int timeout = Integer.parseInt(configValue);
                    securityManagerService.updateSessionTimeout(timeout);
                    break;
                case "security_max_login_attempts":
                    int maxAttempts = Integer.parseInt(configValue);
                    securityManagerService.updateMaxLoginAttempts(maxAttempts);
                    break;
                default:
                    log.warn("[PlatformConfigRuntimeService] Unknown security config key: {}", configKey);
                    break;
            }
        } catch (NumberFormatException e) {
            log.error("[PlatformConfigRuntimeService] Invalid security config value: key={}, value={}", 
                    configKey, configValue, e);
        }
    }

    /**
     * 로깅 설정 업데이트
     * 
     * @param configKey 설정 키
     * @param configValue 설정 값
     */
    public void updateLoggingSettings(String configKey, String configValue) {
        log.info("[PlatformConfigRuntimeService] Updating logging settings - key={}, value={}", 
                configKey, configValue);
        
        switch (configKey) {
            case "logging_level":
                loggingManagerService.updateLoggingLevel(configValue);
                break;
            default:
                log.warn("[PlatformConfigRuntimeService] Unknown logging config key: {}", configKey);
                break;
        }
    }
}
