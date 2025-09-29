package com.agenticcp.core.domain.platform.service;

import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.platform.entity.PlatformConfig;
import com.agenticcp.core.domain.platform.enums.PlatformConfigErrorCode;
import com.agenticcp.core.domain.platform.exception.ConfigValidationException;
import com.agenticcp.core.domain.platform.repository.PlatformConfigRepository;
import com.agenticcp.core.domain.platform.validation.ConfigValidator;
import com.agenticcp.core.common.util.LogMaskingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 플랫폼 설정 관리 서비스
 *
 * 플랫폼 전역 설정의 조회/생성/수정/삭제 기능을 제공합니다. 일부 값은 민감할 수 있으므로 로깅 시 주의합니다.
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlatformConfigService {

    private final PlatformConfigRepository platformConfigRepository;
    private final List<ConfigValidator> configValidators;

    public List<PlatformConfig> getAllConfigs() {
        log.info("[PlatformConfigService] getAllConfigs");
        List<PlatformConfig> result = platformConfigRepository.findAllActive();
        log.info("[PlatformConfigService] getAllConfigs - success count={}", result.size());
        return result;
    }

    public Optional<PlatformConfig> getConfigByKey(String configKey) {
        log.info("[PlatformConfigService] getConfigByKey - configKey={}", LogMaskingUtils.mask(configKey, 2, 2));
        Optional<PlatformConfig> result = platformConfigRepository.findByConfigKey(configKey);
        log.info("[PlatformConfigService] getConfigByKey - found={} configKey={}", result.isPresent(), LogMaskingUtils.mask(configKey, 2, 2));
        return result;
    }

    public PlatformConfig getConfigByKeyOrThrow(String configKey) {
        log.info("[PlatformConfigService] getConfigByKeyOrThrow - configKey={}", LogMaskingUtils.mask(configKey, 2, 2));
        PlatformConfig config = platformConfigRepository.findByConfigKey(configKey)
                .orElseThrow(() -> new ResourceNotFoundException("PlatformConfig", "configKey", configKey));
        log.info("[PlatformConfigService] getConfigByKeyOrThrow - success configKey={}", LogMaskingUtils.mask(configKey, 2, 2));
        return config;
    }

    public List<PlatformConfig> getConfigsByType(PlatformConfig.ConfigType configType) {
        log.info("[PlatformConfigService] getConfigsByType - type={}", configType);
        List<PlatformConfig> result = platformConfigRepository.findByConfigType(configType);
        log.info("[PlatformConfigService] getConfigsByType - success count={} type={}", result.size(), configType);
        return result;
    }

    public List<PlatformConfig> getSystemConfigs() {
        log.info("[PlatformConfigService] getSystemConfigs");
        List<PlatformConfig> result = platformConfigRepository.findByIsSystem(true);
        log.info("[PlatformConfigService] getSystemConfigs - success count={}", result.size());
        return result;
    }

    @Transactional
    public PlatformConfig createConfig(PlatformConfig platformConfig) {
        log.info("[PlatformConfigService] createConfig - configKey={} isEncrypted={} type={}",
                LogMaskingUtils.mask(platformConfig.getConfigKey(), 2, 2),
                platformConfig.getIsEncrypted(),
                platformConfig.getConfigType());
        
        // 설정 검증 수행
        validateConfig(platformConfig);
        
        // 중복 키 검증
        if (platformConfigRepository.findByConfigKey(platformConfig.getConfigKey()).isPresent()) {
            throw new ConfigValidationException(PlatformConfigErrorCode.CONFIG_ALREADY_EXISTS);
        }
        
        PlatformConfig saved = platformConfigRepository.save(platformConfig);
        log.info("[PlatformConfigService] createConfig - success configKey={}", LogMaskingUtils.mask(saved.getConfigKey(), 2, 2));
        return saved;
    }

    @Transactional
    public PlatformConfig updateConfig(String configKey, PlatformConfig updatedConfig) {
        log.info("[PlatformConfigService] updateConfig - configKey={}", LogMaskingUtils.mask(configKey, 2, 2));
        PlatformConfig existingConfig = getConfigByKeyOrThrow(configKey);
        
        // 시스템 설정 수정 방지
        if (Boolean.TRUE.equals(existingConfig.getIsSystem())) {
            throw new ConfigValidationException(PlatformConfigErrorCode.SYSTEM_CONFIG_CANNOT_MODIFY);
        }
        
        // 업데이트할 설정에 키 설정 (검증을 위해)
        updatedConfig.setConfigKey(configKey);
        
        // 설정 검증 수행
        validateConfig(updatedConfig);
        
        existingConfig.setConfigValue(updatedConfig.getConfigValue());
        existingConfig.setConfigType(updatedConfig.getConfigType());
        existingConfig.setDescription(updatedConfig.getDescription());
        existingConfig.setIsEncrypted(updatedConfig.getIsEncrypted());
        
        PlatformConfig saved = platformConfigRepository.save(existingConfig);
        log.info("[PlatformConfigService] updateConfig - success configKey={}", LogMaskingUtils.mask(configKey, 2, 2));
        return saved;
    }

    @Transactional
    public void deleteConfig(String configKey) {
        log.info("[PlatformConfigService] deleteConfig - configKey={}", LogMaskingUtils.mask(configKey, 2, 2));
        PlatformConfig config = getConfigByKeyOrThrow(configKey);
        
        // 시스템 설정 삭제 방지
        if (Boolean.TRUE.equals(config.getIsSystem())) {
            throw new ConfigValidationException(PlatformConfigErrorCode.SYSTEM_CONFIG_CANNOT_DELETE);
        }
        
        config.setIsDeleted(true);
        platformConfigRepository.save(config);
        log.info("[PlatformConfigService] deleteConfig - success configKey={}", LogMaskingUtils.mask(configKey, 2, 2));
    }

    @Transactional
    public void hardDeleteConfig(String configKey) {
        log.info("[PlatformConfigService] hardDeleteConfig - configKey={}", LogMaskingUtils.mask(configKey, 2, 2));
        PlatformConfig config = getConfigByKeyOrThrow(configKey);
        
        // 시스템 설정 삭제 방지
        if (Boolean.TRUE.equals(config.getIsSystem())) {
            throw new ConfigValidationException(PlatformConfigErrorCode.SYSTEM_CONFIG_CANNOT_DELETE);
        }
        
        platformConfigRepository.delete(config);
        log.info("[PlatformConfigService] hardDeleteConfig - success configKey={}", LogMaskingUtils.mask(configKey, 2, 2));
    }

    /**
     * 플랫폼 설정의 유효성을 검증합니다.
     *
     * @param platformConfig 검증할 설정 객체
     * @throws ConfigValidationException 검증 실패 시
     */
    private void validateConfig(PlatformConfig platformConfig) {
        log.debug("[PlatformConfigService] validateConfig - configKey={}", 
                LogMaskingUtils.mask(platformConfig.getConfigKey(), 2, 2));

        try {
            // 모든 검증기를 사용하여 검증 수행
            for (ConfigValidator validator : configValidators) {
                validator.validate(platformConfig);
            }

            log.debug("[PlatformConfigService] validateConfig - success");
        } catch (Exception e) {
            log.warn("[PlatformConfigService] validateConfig - failed: {}", e.getMessage());
            throw e;
        }
    }
}
