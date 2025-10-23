package com.agenticcp.core.domain.tenant.service;

import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.tenant.dto.TenantConfigRequest;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.tenant.entity.TenantConfig;
import com.agenticcp.core.domain.tenant.repository.TenantConfigRepository;
import com.agenticcp.core.common.util.LogMaskingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 테넌트 설정 관리 서비스
 * 
 * 개별 테넌트의 설정을 생성/수정/삭제/조회하는 기능을 제공합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TenantConfigService {

    private final TenantConfigRepository tenantConfigRepository;
    private final TenantService tenantService;

    /**
     * 테넌트의 모든 설정 조회
     */
    public List<TenantConfig> getTenantConfigurations(String tenantKey) {
        log.info("[TenantConfigService] getTenantConfigurations - tenantKey={}", 
                LogMaskingUtils.maskTenantKey(tenantKey));

        List<TenantConfig> configs = tenantConfigRepository.findByTenantKey(tenantKey);
        
        log.info("[TenantConfigService] getTenantConfigurations - success tenantKey={}, count={}", 
                LogMaskingUtils.maskTenantKey(tenantKey), configs.size());
        
        return configs;
    }

    /**
     * 테넌트의 특정 설정 조회
     */
    public Optional<TenantConfig> getTenantConfiguration(String tenantKey, String configKey) {
        log.info("[TenantConfigService] getTenantConfiguration - tenantKey={}, configKey={}", 
                LogMaskingUtils.maskTenantKey(tenantKey), configKey);

        Optional<TenantConfig> config = tenantConfigRepository.findByTenantKeyAndConfigKey(tenantKey, configKey);
        
        log.info("[TenantConfigService] getTenantConfiguration - found={}, tenantKey={}, configKey={}", 
                config.isPresent(), LogMaskingUtils.maskTenantKey(tenantKey), configKey);
        
        return config;
    }

    /**
     * 테넌트 설정 생성/수정
     */
    @Transactional
    @CacheEvict(value = "tenantConfigs", key = "#tenantKey")
    public TenantConfig saveTenantConfiguration(String tenantKey, TenantConfigRequest request) {
        log.info("[TenantConfigService] saveTenantConfiguration - tenantKey={}, configKey={}", 
                LogMaskingUtils.maskTenantKey(tenantKey), request.getConfigKey());

        Tenant tenant = tenantService.getTenantByKeyOrThrow(tenantKey);

        // 기존 설정이 있는지 확인
        Optional<TenantConfig> existingConfig = tenantConfigRepository
                .findByTenantAndConfigKeyAndIsDeletedFalse(tenant, request.getConfigKey());

        TenantConfig config;
        if (existingConfig.isPresent()) {
            // 수정
            config = existingConfig.get();
            config.setConfigValue(request.getConfigValue());
            config.setConfigType(request.getConfigType());
            config.setDescription(request.getDescription());
            config.setIsEncrypted(request.getIsEncrypted());
            log.info("[TenantConfigService] saveTenantConfiguration - updated existing config");
        } else {
            // 생성
            config = TenantConfig.builder()
                    .tenant(tenant)
                    .configKey(request.getConfigKey())
                    .configValue(request.getConfigValue())
                    .configType(request.getConfigType())
                    .description(request.getDescription())
                    .isEncrypted(request.getIsEncrypted())
                    .build();
            log.info("[TenantConfigService] saveTenantConfiguration - created new config");
        }

        TenantConfig savedConfig = tenantConfigRepository.save(config);
        
        log.info("[TenantConfigService] saveTenantConfiguration - success tenantKey={}, configKey={}", 
                LogMaskingUtils.maskTenantKey(tenantKey), request.getConfigKey());
        
        return savedConfig;
    }

    /**
     * 테넌트 설정 삭제
     */
    @Transactional
    @CacheEvict(value = "tenantConfigs", key = "#tenantKey")
    public void deleteTenantConfiguration(String tenantKey, String configKey) {
        log.info("[TenantConfigService] deleteTenantConfiguration - tenantKey={}, configKey={}", 
                LogMaskingUtils.maskTenantKey(tenantKey), configKey);

        Tenant tenant = tenantService.getTenantByKeyOrThrow(tenantKey);
        TenantConfig config = tenantConfigRepository
                .findByTenantAndConfigKeyAndIsDeletedFalse(tenant, configKey)
                .orElseThrow(() -> new ResourceNotFoundException("TenantConfig", "configKey", configKey));

        config.setIsDeleted(true);
        tenantConfigRepository.save(config);
        
        log.info("[TenantConfigService] deleteTenantConfiguration - success tenantKey={}, configKey={}", 
                LogMaskingUtils.maskTenantKey(tenantKey), configKey);
    }

    /**
     * 테넌트의 모든 설정 삭제
     */
    @Transactional
    @CacheEvict(value = "tenantConfigs", key = "#tenantKey")
    public void deleteAllTenantConfigurations(String tenantKey) {
        log.info("[TenantConfigService] deleteAllTenantConfigurations - tenantKey={}", 
                LogMaskingUtils.maskTenantKey(tenantKey));

        Tenant tenant = tenantService.getTenantByKeyOrThrow(tenantKey);
        List<TenantConfig> configs = tenantConfigRepository.findByTenantAndIsDeletedFalse(tenant);
        
        for (TenantConfig config : configs) {
            config.setIsDeleted(true);
        }
        
        tenantConfigRepository.saveAll(configs);
        
        log.info("[TenantConfigService] deleteAllTenantConfigurations - success tenantKey={}, count={}", 
                LogMaskingUtils.maskTenantKey(tenantKey), configs.size());
    }
}



