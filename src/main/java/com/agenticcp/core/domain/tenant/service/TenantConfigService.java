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
 * TenantConfig는 특정 테넌트만의 고유 설정으로, 
 * TenantTypeConfig와 PlatformConfig보다 높은 우선순위를 가집니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-23
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
     * 
     * 특정 테넌트에 설정된 모든 개별 설정을 조회합니다.
     * 삭제되지 않은(isDeleted=false) 설정만 반환합니다.
     * 
     * @param tenantKey 조회할 테넌트 키
     * @return 테넌트의 설정 목록
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
     * 
     * 테넌트의 특정 설정 키에 해당하는 설정을 조회합니다.
     * 설정이 없거나 삭제된 경우 Empty Optional을 반환합니다.
     * 
     * @param tenantKey 조회할 테넌트 키
     * @param configKey 조회할 설정 키
     * @return Optional로 감싼 테넌트 설정 (없으면 Empty)
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
     * 
     * 테넌트의 특정 설정을 생성하거나 수정합니다.
     * 같은 configKey가 이미 존재하면 수정, 없으면 신규 생성합니다.
     * 저장 후 해당 테넌트의 설정 캐시를 무효화합니다.
     * 
     * @param tenantKey 설정을 저장할 테넌트 키
     * @param request 설정 요청 정보 (키, 값, 타입, 설명, 암호화 여부)
     * @return 저장된 테넌트 설정
     */
    @Transactional
    @CacheEvict(value = "tenantConfigs", key = "#tenantKey")
    public TenantConfig saveTenantConfiguration(String tenantKey, TenantConfigRequest request) {
        log.info("[TenantConfigService] saveTenantConfiguration - tenantKey={}, configKey={}", 
                LogMaskingUtils.maskTenantKey(tenantKey), request.getConfigKey());

        Tenant tenant = tenantService.getTenantByKeyOrThrow(tenantKey);

        // Upsert 패턴: 기존 설정 존재 여부 확인
        Optional<TenantConfig> existingConfig = tenantConfigRepository
                .findByTenantAndConfigKeyAndIsDeletedFalse(tenant, request.getConfigKey());

        TenantConfig config;
        if (existingConfig.isPresent()) {
            // 기존 설정 수정 (모든 필드 업데이트)
            config = existingConfig.get();
            config.setConfigValue(request.getConfigValue());
            config.setConfigType(request.getConfigType());
            config.setDescription(request.getDescription());
            config.setIsEncrypted(request.getIsEncrypted());
            log.info("[TenantConfigService] saveTenantConfiguration - updated existing config");
        } else {
            // 신규 설정 생성
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
     * 테넌트 설정 삭제 (소프트 삭제)
     * 
     * 특정 테넌트의 설정을 삭제합니다.
     * 물리적 삭제가 아닌 isDeleted 플래그를 true로 변경하는 소프트 삭제입니다.
     * 삭제 후 해당 테넌트의 설정 캐시를 무효화합니다.
     * 
     * @param tenantKey 삭제할 설정이 속한 테넌트 키
     * @param configKey 삭제할 설정 키
     * @throws ResourceNotFoundException 설정을 찾을 수 없는 경우
     */
    @Transactional
    @CacheEvict(value = "tenantConfigs", key = "#tenantKey")
    public void deleteTenantConfiguration(String tenantKey, String configKey) {
        log.info("[TenantConfigService] deleteTenantConfiguration - tenantKey={}, configKey={}", 
                LogMaskingUtils.maskTenantKey(tenantKey), configKey);

        Tenant tenant = tenantService.getTenantByKeyOrThrow(tenantKey);
        
        // 설정 존재 확인 후 소프트 삭제
        TenantConfig config = tenantConfigRepository
                .findByTenantAndConfigKeyAndIsDeletedFalse(tenant, configKey)
                .orElseThrow(() -> new ResourceNotFoundException("TenantConfig", "configKey", configKey));

        // 소프트 삭제: isDeleted 플래그만 변경 (데이터 보존)
        config.setIsDeleted(true);
        tenantConfigRepository.save(config);
        
        log.info("[TenantConfigService] deleteTenantConfiguration - success tenantKey={}, configKey={}", 
                LogMaskingUtils.maskTenantKey(tenantKey), configKey);
    }

    /**
     * 테넌트의 모든 설정 일괄 삭제 (소프트 삭제)
     * 
     * 특정 테넌트의 모든 개별 설정을 일괄 삭제합니다.
     * 소프트 삭제 방식으로 데이터는 보존되며, 삭제 후 캐시를 무효화합니다.
     * 테넌트 탈퇴나 초기화 시 사용됩니다.
     * 
     * @param tenantKey 모든 설정을 삭제할 테넌트 키
     */
    @Transactional
    @CacheEvict(value = "tenantConfigs", key = "#tenantKey")
    public void deleteAllTenantConfigurations(String tenantKey) {
        log.info("[TenantConfigService] deleteAllTenantConfigurations - tenantKey={}", 
                LogMaskingUtils.maskTenantKey(tenantKey));

        Tenant tenant = tenantService.getTenantByKeyOrThrow(tenantKey);
        
        // 삭제되지 않은 모든 설정 조회
        List<TenantConfig> configs = tenantConfigRepository.findByTenantAndIsDeletedFalse(tenant);
        
        // 일괄 소프트 삭제 처리
        for (TenantConfig config : configs) {
            config.setIsDeleted(true);
        }
        
        // 배치 업데이트로 성능 최적화
        tenantConfigRepository.saveAll(configs);
        
        log.info("[TenantConfigService] deleteAllTenantConfigurations - success tenantKey={}, count={}", 
                LogMaskingUtils.maskTenantKey(tenantKey), configs.size());
    }
}



