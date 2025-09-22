package com.agenticcp.core.domain.platform.service;

import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.common.exception.ValidationException;
import com.agenticcp.core.domain.platform.util.LoggingUtils;
import com.agenticcp.core.domain.platform.entity.PlatformConfig;
import com.agenticcp.core.domain.platform.repository.PlatformConfigRepository;
import com.agenticcp.core.domain.platform.validation.ConfigValidators;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * PlatformConfig 도메인의 비즈니스 로직을 담당.
 * - 조회: 전체/타입별/시스템 설정 페이징 조회
 * - 단건: 키로 조회
 * - 생성/수정/삭제: 타입별 검증 및 정책(시스템 설정 삭제 금지) 적용
 * - 로깅: 민감한 값은 마스킹하여 로그 출력
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlatformConfigService {

    private final PlatformConfigRepository platformConfigRepository;

    /** 전체 설정 페이징 조회 */
    public Page<PlatformConfig> getAllConfigs(Pageable pageable) {
        return platformConfigRepository.findAll(pageable);
    }

    /** 키로 단건 조회 */
    public Optional<PlatformConfig> getConfigByKey(String configKey) {
        return platformConfigRepository.findByConfigKey(configKey);
    }

    /** 키로 단건 조회(없으면 404 예외) */
    public PlatformConfig getConfigByKeyOrThrow(String configKey) {
        return platformConfigRepository.findByConfigKey(configKey)
                .orElseThrow(() -> new ResourceNotFoundException("PlatformConfig", "configKey", configKey));
    }

    /** 타입별 페이징 조회 */
    public Page<PlatformConfig> getConfigsByType(PlatformConfig.ConfigType configType, Pageable pageable) {
        return platformConfigRepository.findByConfigType(configType, pageable);
    }

    /** 시스템 설정만 페이징 조회 */
    public Page<PlatformConfig> getSystemConfigs(Pageable pageable) {
        return platformConfigRepository.findByIsSystem(true, pageable);
    }

    /** 필터링된 설정 페이징 조회 */
    public Page<PlatformConfig> getFilteredConfigs(String configKeyPattern, PlatformConfig.ConfigType configType, Boolean isSystem, Pageable pageable) {
        return platformConfigRepository.findFilteredConfigs(configKeyPattern, configType, isSystem, pageable);
    }

    /** 생성: 키 정책 및 타입-값 일치 검증, 로깅 시 민감 값 마스킹 */
    @Transactional
    public PlatformConfig createConfig(PlatformConfig platformConfig) {
        ConfigValidators.validateKeyPolicy(platformConfig.getConfigKey());
        ConfigValidators.validateValueByType(platformConfig.getConfigType(), platformConfig.getConfigValue());
        
        // 로깅 시 민감 값 마스킹
        log.info("Creating platform config: {} = {}", 
            platformConfig.getConfigKey(), 
            LoggingUtils.maskSensitiveValue(platformConfig));
        
        PlatformConfig savedConfig = platformConfigRepository.save(platformConfig);
        
        // 저장 후에도 마스킹된 로그 출력
        log.info("Successfully created platform config: {}", 
            LoggingUtils.formatConfigForLogging(savedConfig));
        
        return savedConfig;
    }

    /** 수정: 타입-값 일치 검증 후 갱신, 로깅 시 민감 값 마스킹 */
    @Transactional
    public PlatformConfig updateConfig(String configKey, PlatformConfig updatedConfig) {
        PlatformConfig existingConfig = getConfigByKeyOrThrow(configKey);
        ConfigValidators.validateValueByType(updatedConfig.getConfigType(), updatedConfig.getConfigValue());
        
        // 기존 값 로깅 (마스킹)
        log.info("Updating platform config: {} from {} to {}", 
            configKey,
            LoggingUtils.maskSensitiveValue(existingConfig),
            LoggingUtils.maskSensitiveValue(updatedConfig));
        
        existingConfig.setConfigValue(updatedConfig.getConfigValue());
        existingConfig.setConfigType(updatedConfig.getConfigType());
        existingConfig.setDescription(updatedConfig.getDescription());
        existingConfig.setIsEncrypted(updatedConfig.getConfigType() == PlatformConfig.ConfigType.ENCRYPTED);
        
        PlatformConfig savedConfig = platformConfigRepository.save(existingConfig);
        
        // 수정 후 마스킹된 로그 출력
        log.info("Successfully updated platform config: {}", 
            LoggingUtils.formatConfigForLogging(savedConfig));
        
        return savedConfig;
    }

    /** 삭제: 시스템 설정은 삭제 금지(소프트 삭제만), 로깅 시 민감 값 마스킹 */
    @Transactional
    public void deleteConfig(String configKey) {
        PlatformConfig config = getConfigByKeyOrThrow(configKey);
        
        if (Boolean.TRUE.equals(config.getIsSystem())) {
            log.warn("Attempted to delete system config: {} = {}", 
                configKey, 
                LoggingUtils.maskSensitiveValue(config));
            throw new ValidationException("configKey", "system config cannot be deleted");
        }
        
        // 삭제 전 마스킹된 로그 출력
        log.info("Soft deleting platform config: {} = {}", 
            configKey, 
            LoggingUtils.maskSensitiveValue(config));
        
        config.setIsDeleted(true);
        platformConfigRepository.save(config);
        
        log.info("Successfully soft deleted platform config: {}", configKey);
    }

    @Transactional
    public void hardDeleteConfig(String configKey) {
        PlatformConfig config = getConfigByKeyOrThrow(configKey);
        
        // 하드 삭제 전 마스킹된 로그 출력
        log.info("Hard deleting platform config: {} = {}", 
            configKey, 
            LoggingUtils.maskSensitiveValue(config));
        
        platformConfigRepository.delete(config);
        
        log.info("Successfully hard deleted platform config: {}", configKey);
    }
}
