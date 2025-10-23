package com.agenticcp.core.domain.tenant.service;

import com.agenticcp.core.domain.tenant.dto.EffectiveConfigResponse;
import com.agenticcp.core.common.util.LogMaskingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;


/**
 * 테넌트 설정 캐시 서비스
 * 
 * 테넌트별 유효 설정을 캐시하여 성능을 최적화하고,
 * 설정 변경 시 캐시 무효화를 관리합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantConfigCacheService {

    private final TenantConfigInheritanceService tenantConfigInheritanceService;

    /**
     * 캐시된 테넌트 설정 조회
     */
    @Cacheable(value = "tenantConfigs", key = "#tenantKey")
    public EffectiveConfigResponse getCachedConfigurations(String tenantKey) {
        log.info("[TenantConfigCacheService] getCachedConfigurations - cache miss, loading tenantKey={}", 
                LogMaskingUtils.maskTenantKey(tenantKey));
        
        return tenantConfigInheritanceService.getEffectiveConfigurations(tenantKey);
    }

    /**
     * 캐시된 특정 설정 조회
     */
    @Cacheable(value = "tenantConfig", key = "#tenantKey + ':' + #configKey")
    public Object getCachedConfiguration(String tenantKey, String configKey) {
        log.info("[TenantConfigCacheService] getCachedConfiguration - cache miss, loading tenantKey={}, configKey={}", 
                LogMaskingUtils.maskTenantKey(tenantKey), configKey);
        
        return tenantConfigInheritanceService.getEffectiveConfiguration(tenantKey, configKey);
    }

    /**
     * 특정 테넌트의 설정 캐시 무효화
     */
    @CacheEvict(value = {"tenantConfigs", "tenantConfig"}, key = "#tenantKey")
    public void evictTenantConfigCache(String tenantKey) {
        log.info("[TenantConfigCacheService] evictTenantConfigCache - tenantKey={}", 
                LogMaskingUtils.maskTenantKey(tenantKey));
    }

    /**
     * 특정 테넌트의 특정 설정 캐시 무효화
     */
    @CacheEvict(value = {"tenantConfigs", "tenantConfig"}, key = "#tenantKey + ':' + #configKey")
    public void evictTenantConfigCache(String tenantKey, String configKey) {
        log.info("[TenantConfigCacheService] evictTenantConfigCache - tenantKey={}, configKey={}", 
                LogMaskingUtils.maskTenantKey(tenantKey), configKey);
    }

    /**
     * 모든 테넌트 설정 캐시 무효화
     */
    @CacheEvict(value = {"tenantConfigs", "tenantConfig"}, allEntries = true)
    public void evictAllTenantConfigCache() {
        log.info("[TenantConfigCacheService] evictAllTenantConfigCache");
    }

    /**
     * 테넌트 타입별 캐시 무효화
     * 해당 타입을 사용하는 모든 테넌트의 캐시를 무효화합니다.
     */
    @CacheEvict(value = {"tenantConfigs", "tenantConfig"}, allEntries = true)
    public void evictCacheByTenantType(String tenantType) {
        log.info("[TenantConfigCacheService] evictCacheByTenantType - tenantType={}", tenantType);
    }

    /**
     * 플랫폼 설정 변경으로 인한 캐시 무효화
     */
    @CacheEvict(value = {"tenantConfigs", "tenantConfig"}, allEntries = true)
    public void evictCacheByPlatformConfigChange() {
        log.info("[TenantConfigCacheService] evictCacheByPlatformConfigChange");
    }

    /**
     * 특정 설정 키 변경으로 인한 캐시 무효화
     * 해당 설정 키를 사용하는 모든 테넌트의 캐시를 무효화합니다.
     */
    @CacheEvict(value = {"tenantConfigs", "tenantConfig"}, allEntries = true)
    public void evictCacheByConfigKey(String configKey) {
        log.info("[TenantConfigCacheService] evictCacheByConfigKey - configKey={}", configKey);
    }
}
