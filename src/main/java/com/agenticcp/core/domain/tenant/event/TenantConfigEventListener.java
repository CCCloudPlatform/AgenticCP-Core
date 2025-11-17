package com.agenticcp.core.domain.tenant.event;

import com.agenticcp.core.domain.tenant.service.TenantConfigCacheService;
import com.agenticcp.core.common.util.LogMaskingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 테넌트 설정 관련 이벤트 리스너
 * 
 * 테넌트 설정 변경, 테넌트 타입 변경 등의 이벤트를 감지하여
 * 관련 캐시를 자동으로 무효화합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantConfigEventListener {

    private final TenantConfigCacheService tenantConfigCacheService;

    /**
     * 테넌트 타입 변경 이벤트 처리
     */
    @EventListener
    public void handleTenantTypeChangeEvent(TenantTypeChangeEvent event) {
        log.info("[TenantConfigEventListener] handleTenantTypeChangeEvent - tenantKey={}, oldType={}, newType={}", 
                LogMaskingUtils.maskTenantKey(event.getTenantKey()), event.getOldType(), event.getNewType());

        // 해당 테넌트의 캐시 무효화
        tenantConfigCacheService.evictTenantConfigCache(event.getTenantKey());
        
        log.info("[TenantConfigEventListener] handleTenantTypeChangeEvent - cache evicted for tenantKey={}", 
                LogMaskingUtils.maskTenantKey(event.getTenantKey()));
    }

    /**
     * 테넌트 설정 변경 이벤트 처리
     */
    @EventListener
    public void handleTenantConfigChangeEvent(TenantConfigChangeEvent event) {
        log.info("[TenantConfigEventListener] handleTenantConfigChangeEvent - tenantKey={}, configKey={}", 
                LogMaskingUtils.maskTenantKey(event.getTenantKey()), event.getConfigKey());

        // 해당 테넌트의 캐시 무효화
        tenantConfigCacheService.evictTenantConfigCache(event.getTenantKey(), event.getConfigKey());
        
        log.info("[TenantConfigEventListener] handleTenantConfigChangeEvent - cache evicted for tenantKey={}, configKey={}", 
                LogMaskingUtils.maskTenantKey(event.getTenantKey()), event.getConfigKey());
    }

    /**
     * 플랫폼 설정 변경 이벤트 처리
     */
    @EventListener
    public void handlePlatformConfigChangeEvent(PlatformConfigChangeEvent event) {
        log.info("[TenantConfigEventListener] handlePlatformConfigChangeEvent - configKey={}", event.getConfigKey());

        // 플랫폼 설정 변경으로 인한 모든 테넌트 캐시 무효화
        tenantConfigCacheService.evictCacheByPlatformConfigChange();
        
        log.info("[TenantConfigEventListener] handlePlatformConfigChangeEvent - all tenant caches evicted");
    }

    /**
     * 테넌트 타입별 설정 변경 이벤트 처리
     */
    @EventListener
    public void handleTenantTypeConfigChangeEvent(TenantTypeConfigChangeEvent event) {
        log.info("[TenantConfigEventListener] handleTenantTypeConfigChangeEvent - tenantType={}, configKey={}", 
                event.getTenantType(), event.getConfigKey());

        // 해당 타입을 사용하는 모든 테넌트의 캐시 무효화
        tenantConfigCacheService.evictCacheByTenantType(event.getTenantType());
        
        log.info("[TenantConfigEventListener] handleTenantTypeConfigChangeEvent - cache evicted for tenantType={}", 
                event.getTenantType());
    }
}
