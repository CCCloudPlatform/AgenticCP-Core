package com.agenticcp.core.domain.platform.service;

import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.platform.entity.PlatformConfig;
import com.agenticcp.core.domain.platform.repository.PlatformConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlatformConfigService {

    private final PlatformConfigRepository platformConfigRepository;

    public List<PlatformConfig> getAllConfigs() {
        return platformConfigRepository.findAllActive();
    }

    public Optional<PlatformConfig> getConfigByKey(String configKey) {
        return platformConfigRepository.findByConfigKey(configKey);
    }

    public PlatformConfig getConfigByKeyOrThrow(String configKey) {
        return platformConfigRepository.findByConfigKey(configKey)
                .orElseThrow(() -> new ResourceNotFoundException("PlatformConfig", "configKey", configKey));
    }

    public List<PlatformConfig> getConfigsByType(PlatformConfig.ConfigType configType) {
        return platformConfigRepository.findByConfigType(configType);
    }

    public List<PlatformConfig> getSystemConfigs() {
        return platformConfigRepository.findByIsSystem(true);
    }

    @Transactional
    public PlatformConfig createConfig(PlatformConfig platformConfig) {
        log.info("Creating platform config: {}", platformConfig.getConfigKey());
        return platformConfigRepository.save(platformConfig);
    }

    @Transactional
    public PlatformConfig updateConfig(String configKey, PlatformConfig updatedConfig) {
        PlatformConfig existingConfig = getConfigByKeyOrThrow(configKey);
        
        existingConfig.setConfigValue(updatedConfig.getConfigValue());
        existingConfig.setConfigType(updatedConfig.getConfigType());
        existingConfig.setDescription(updatedConfig.getDescription());
        existingConfig.setIsEncrypted(updatedConfig.getIsEncrypted());
        
        log.info("Updating platform config: {}", configKey);
        return platformConfigRepository.save(existingConfig);
    }

    @Transactional
    public void deleteConfig(String configKey) {
        PlatformConfig config = getConfigByKeyOrThrow(configKey);
        config.setIsDeleted(true);
        platformConfigRepository.save(config);
        log.info("Soft deleted platform config: {}", configKey);
    }

    @Transactional
    public void hardDeleteConfig(String configKey) {
        PlatformConfig config = getConfigByKeyOrThrow(configKey);
        platformConfigRepository.delete(config);
        log.info("Hard deleted platform config: {}", configKey);
    }
}
