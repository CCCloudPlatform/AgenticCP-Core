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
 * 
 * 캐시 전략:
 * - tenantConfigs: 테넌트의 전체 설정 캐시
 * - tenantConfig: 테넌트의 개별 설정 캐시
 * 
 * 무효화 시나리오:
 * 1. TenantConfig 변경 시 → 해당 테넌트 캐시만 무효화
 * 2. TenantTypeConfig 변경 시 → 해당 타입의 모든 테넌트 캐시 무효화
 * 3. PlatformConfig 변경 시 → 전체 캐시 무효화
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantConfigCacheService {

    private final TenantConfigInheritanceService tenantConfigInheritanceService;

    /**
     * 캐시된 테넌트 전체 설정 조회
     * 
     * 테넌트의 모든 유효 설정을 캐시에서 조회합니다.
     * 캐시 미스 시 TenantConfigInheritanceService를 통해 설정을 계산하고 캐시합니다.
     * 
     * @param tenantKey 조회할 테넌트 키
     * @return 유효 설정 응답 (설정값과 출처 정보 포함)
     */
    @Cacheable(value = "tenantConfigs", key = "#tenantKey")
    public EffectiveConfigResponse getCachedConfigurations(String tenantKey) {
        log.info("[TenantConfigCacheService] getCachedConfigurations - cache miss, loading tenantKey={}", 
                LogMaskingUtils.maskTenantKey(tenantKey));
        
        return tenantConfigInheritanceService.getEffectiveConfigurations(tenantKey);
    }

    /**
     * 캐시된 특정 설정 조회
     * 
     * 테넌트의 특정 설정 키에 대한 값을 캐시에서 조회합니다.
     * 캐시 미스 시 상속 계층을 통해 설정을 계산하고 캐시합니다.
     * 
     * @param tenantKey 조회할 테넌트 키
     * @param configKey 조회할 설정 키
     * @return 유효 설정값 (파싱된 객체), 없으면 null
     */
    @Cacheable(value = "tenantConfig", key = "#tenantKey + ':' + #configKey")
    public Object getCachedConfiguration(String tenantKey, String configKey) {
        log.info("[TenantConfigCacheService] getCachedConfiguration - cache miss, loading tenantKey={}, configKey={}", 
                LogMaskingUtils.maskTenantKey(tenantKey), configKey);
        
        return tenantConfigInheritanceService.getEffectiveConfiguration(tenantKey, configKey);
    }

    /**
     * 특정 테넌트의 설정 캐시 무효화
     * 
     * 테넌트의 개별 설정(TenantConfig)이 변경되었을 때 호출됩니다.
     * 해당 테넌트의 전체 설정 캐시와 개별 설정 캐시를 모두 무효화합니다.
     * 
     * @param tenantKey 캐시를 무효화할 테넌트 키
     */
    @CacheEvict(value = {"tenantConfigs", "tenantConfig"}, key = "#tenantKey")
    public void evictTenantConfigCache(String tenantKey) {
        log.info("[TenantConfigCacheService] evictTenantConfigCache - tenantKey={}", 
                LogMaskingUtils.maskTenantKey(tenantKey));
    }

    /**
     * 특정 테넌트의 특정 설정 캐시 무효화
     * 
     * 테넌트의 개별 설정 중 특정 키만 변경되었을 때 호출됩니다.
     * 해당 테넌트의 전체 설정 캐시와 해당 키의 개별 캐시를 무효화합니다.
     * 
     * @param tenantKey 캐시를 무효화할 테넌트 키
     * @param configKey 캐시를 무효화할 설정 키
     */
    @CacheEvict(value = {"tenantConfigs", "tenantConfig"}, key = "#tenantKey + ':' + #configKey")
    public void evictTenantConfigCache(String tenantKey, String configKey) {
        log.info("[TenantConfigCacheService] evictTenantConfigCache - tenantKey={}, configKey={}", 
                LogMaskingUtils.maskTenantKey(tenantKey), configKey);
    }

    /**
     * 모든 테넌트 설정 캐시 일괄 무효화
     * 
     * 모든 테넌트의 설정 캐시를 일괄 무효화합니다.
     * 시스템 전반에 영향을 주는 변경 시 사용됩니다.
     * (예: 대규모 설정 마이그레이션, 캐시 초기화)
     */
    @CacheEvict(value = {"tenantConfigs", "tenantConfig"}, allEntries = true)
    public void evictAllTenantConfigCache() {
        log.info("[TenantConfigCacheService] evictAllTenantConfigCache");
    }

    /**
     * 테넌트 타입별 캐시 무효화
     * 
     * TenantTypeConfig(타입별 기본 설정)가 변경되었을 때 호출됩니다.
     * 해당 타입을 사용하는 모든 테넌트의 캐시를 무효화합니다.
     * (예: ENTERPRISE 타입의 기본 설정 변경 시)
     * 
     * 참고: 현재는 allEntries=true로 전체 무효화하지만,
     * 향후 성능 최적화를 위해 특정 타입만 무효화하도록 개선 가능
     * 
     * @param tenantType 캐시를 무효화할 테넌트 타입
     */
    @CacheEvict(value = {"tenantConfigs", "tenantConfig"}, allEntries = true)
    public void evictCacheByTenantType(String tenantType) {
        log.info("[TenantConfigCacheService] evictCacheByTenantType - tenantType={}", tenantType);
    }

    /**
     * 플랫폼 설정 변경으로 인한 캐시 무효화
     * 
     * PlatformConfig(플랫폼 전역 설정)가 변경되었을 때 호출됩니다.
     * 플랫폼 설정은 모든 테넌트에 영향을 주므로 전체 캐시를 무효화합니다.
     * (예: 전역 기본값, 시스템 레벨 제한 변경 시)
     */
    @CacheEvict(value = {"tenantConfigs", "tenantConfig"}, allEntries = true)
    public void evictCacheByPlatformConfigChange() {
        log.info("[TenantConfigCacheService] evictCacheByPlatformConfigChange");
    }

    /**
     * 특정 설정 키 변경으로 인한 캐시 무효화
     * 
     * 특정 설정 키가 여러 레벨(Platform, TenantType, Tenant)에서 변경되었을 때 호출됩니다.
     * 해당 설정 키를 참조하는 모든 테넌트의 캐시를 무효화합니다.
     * (예: max_users 설정이 여러 레벨에서 변경될 때)
     * 
     * 참고: 현재는 allEntries=true로 전체 무효화하지만,
     * 향후 성능 최적화를 위해 특정 키만 무효화하도록 개선 가능
     * 
     * @param configKey 변경된 설정 키
     */
    @CacheEvict(value = {"tenantConfigs", "tenantConfig"}, allEntries = true)
    public void evictCacheByConfigKey(String configKey) {
        log.info("[TenantConfigCacheService] evictCacheByConfigKey - configKey={}", configKey);
    }
}
