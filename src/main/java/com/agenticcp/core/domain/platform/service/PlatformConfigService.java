package com.agenticcp.core.domain.platform.service;

import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.crypto.EncryptionService;
import com.agenticcp.core.domain.platform.entity.PlatformConfig;
import com.agenticcp.core.domain.platform.enums.PlatformConfigErrorCode;
import com.agenticcp.core.domain.platform.exception.ConfigValidationException;
import com.agenticcp.core.domain.platform.repository.PlatformConfigRepository;
import com.agenticcp.core.domain.platform.validation.ConfigValidator;
import com.agenticcp.core.common.util.LogMaskingUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final EncryptionService encryptionService;
    private final ConfigAuditService configAuditService;

    public List<PlatformConfig> getAllConfigs() {
        log.info("[PlatformConfigService] getAllConfigs");
        List<PlatformConfig> result = platformConfigRepository.findAllActive()
                .stream()
                .map(pc -> toResponse(pc, false))
                .toList();
        log.info("[PlatformConfigService] getAllConfigs - success count={}", result.size());
        return result;
    }

    public List<PlatformConfig> getAllConfigs(boolean showSecret) {
        log.info("[PlatformConfigService] getAllConfigs - showSecret={}", showSecret);
        List<PlatformConfig> result = platformConfigRepository.findAllActive()
                .stream()
                .map(pc -> toResponse(pc, showSecret))
                .toList();
        log.info("[PlatformConfigService] getAllConfigs - success count={}", result.size());
        return result;
    }

    public List<PlatformConfig> getAllConfigs(boolean showSecret, Boolean isSystem) {
        log.info("[PlatformConfigService] getAllConfigs - showSecret={}, isSystem={}", showSecret, isSystem);
        
        List<PlatformConfig> sourceConfigs;
        if (isSystem != null) {
            sourceConfigs = platformConfigRepository.findByIsSystem(isSystem);
        } else {
            sourceConfigs = platformConfigRepository.findAllActive();
        }
        
        List<PlatformConfig> result = sourceConfigs.stream()
                .map(pc -> toResponse(pc, showSecret))
                .toList();
        
        log.info("[PlatformConfigService] getAllConfigs - success count={}", result.size());
        return result;
    }

    public Optional<PlatformConfig> getConfigByKey(String configKey) {
        log.info("[PlatformConfigService] getConfigByKey - configKey={}", LogMaskingUtils.mask(configKey, 2, 2));
        Optional<PlatformConfig> result = platformConfigRepository.findByConfigKey(configKey)
                .map(pc -> toResponse(pc, false));
        log.info("[PlatformConfigService] getConfigByKey - found={} configKey={}", result.isPresent(), LogMaskingUtils.mask(configKey, 2, 2));
        return result;
    }

    public Optional<PlatformConfig> getConfigByKey(String configKey, boolean showSecret) {
        log.info("[PlatformConfigService] getConfigByKey - showSecret={} configKey={}", showSecret, LogMaskingUtils.mask(configKey, 2, 2));
        Optional<PlatformConfig> result = platformConfigRepository.findByConfigKey(configKey)
                .map(pc -> toResponse(pc, showSecret));
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
        List<PlatformConfig> result = platformConfigRepository.findByConfigType(configType)
                .stream()
                .map(pc -> toResponse(pc, false))
                .toList();
        log.info("[PlatformConfigService] getConfigsByType - success count={} type={}", result.size(), configType);
        return result;
    }

    public List<PlatformConfig> getConfigsByType(PlatformConfig.ConfigType configType, boolean showSecret) {
        log.info("[PlatformConfigService] getConfigsByType - showSecret={} type={}", showSecret, configType);
        List<PlatformConfig> result = platformConfigRepository.findByConfigType(configType)
                .stream()
                .map(pc -> toResponse(pc, showSecret))
                .toList();
        log.info("[PlatformConfigService] getConfigsByType - success count={} type={}", result.size(), configType);
        return result;
    }

    public List<PlatformConfig> getSystemConfigs() {
        log.info("[PlatformConfigService] getSystemConfigs");
        List<PlatformConfig> result = platformConfigRepository.findByIsSystem(true)
                .stream()
                .map(pc -> toResponse(pc, false))
                .toList();
        log.info("[PlatformConfigService] getSystemConfigs - success count={}", result.size());
        return result;
    }

    public List<PlatformConfig> getSystemConfigs(boolean showSecret) {
        log.info("[PlatformConfigService] getSystemConfigs - showSecret={}", showSecret);
        List<PlatformConfig> result = platformConfigRepository.findByIsSystem(true)
                .stream()
                .map(pc -> toResponse(pc, showSecret))
                .toList();
        log.info("[PlatformConfigService] getSystemConfigs - success count={}", result.size());
        return result;
    }

    @Transactional
    public PlatformConfig createConfig(PlatformConfig platformConfig) {
        log.info("[PlatformConfigService] createConfig - configKey={} isEncrypted={} type={}",
                LogMaskingUtils.mask(platformConfig.getConfigKey(), 2, 2),
                platformConfig.getIsEncrypted(),
                platformConfig.getConfigType());
        
        // 네임스페이스 기반 자동 isSystem 설정
        if (platformConfig.getIsSystem() == null) {
            boolean isSystemKey = platformConfig.getConfigKey().matches("^system\\..*");
            platformConfig.setIsSystem(isSystemKey);
            log.info("[PlatformConfigService] createConfig - auto-set isSystem={} for configKey={}", 
                    isSystemKey, LogMaskingUtils.mask(platformConfig.getConfigKey(), 2, 2));
        }
        
        // 설정 검증 수행
        validateConfig(platformConfig);
        
        // 중복 키 검증
        if (platformConfigRepository.findByConfigKey(platformConfig.getConfigKey()).isPresent()) {
            throw new ConfigValidationException(PlatformConfigErrorCode.CONFIG_ALREADY_EXISTS);
        }

        // ENCRYPTED 타입 저장 시 암호화 적용
        String rawNewValue = platformConfig.getConfigValue();
        if (platformConfig.getConfigType() == PlatformConfig.ConfigType.ENCRYPTED) {
            if (rawNewValue != null && !rawNewValue.isEmpty()) {
                if (!isProbablyEncrypted(rawNewValue)) {
                    String encrypted = encryptionService.encrypt(rawNewValue);
                    platformConfig.setConfigValue(encrypted);
                }
            }
            platformConfig.setIsEncrypted(true);
        }

        PlatformConfig saved = platformConfigRepository.save(platformConfig);
        log.info("[PlatformConfigService] createConfig - success configKey={}", LogMaskingUtils.mask(saved.getConfigKey(), 2, 2));

        // 감사 로그
        try {
            if (configAuditService == null) return saved;
            String userId = getCurrentUserId();
            String reason = saved.getDescription();
            String valueType = saved.getConfigType() != null ? saved.getConfigType().name() : null;
            configAuditService.logCreate(saved.getConfigKey(), rawNewValue, userId, reason, valueType);
        } catch (Exception ex) {
            log.warn("[PlatformConfigService] createConfig - audit logging failed: {}", ex.getMessage());
        }
        return saved;
    }

    @Transactional
    public PlatformConfig updateConfig(String configKey, PlatformConfig updatedConfig) {
        log.info("[PlatformConfigService] updateConfig - configKey={}", LogMaskingUtils.mask(configKey, 2, 2));
        PlatformConfig existingConfig = getConfigByKeyOrThrow(configKey);
        
        // 시스템 설정 타입 변경 방지
        if (Boolean.TRUE.equals(existingConfig.getIsSystem()) && 
            !existingConfig.getConfigType().equals(updatedConfig.getConfigType())) {
            throw new ConfigValidationException(PlatformConfigErrorCode.SYSTEM_CONFIG_TYPE_CHANGE_FORBIDDEN);
        }
        

        // 업데이트할 설정에 키 설정 (검증을 위해)
        updatedConfig.setConfigKey(configKey);
        
        // 설정 검증 수행
        validateConfig(updatedConfig);

        // ENCRYPTED 타입 업데이트 시 암호화 적용
        String rawOldValue = existingConfig.getConfigValue();
        String rawNewValue = updatedConfig.getConfigValue();
        if (updatedConfig.getConfigType() == PlatformConfig.ConfigType.ENCRYPTED) {
            String newValue = rawNewValue;
            if (newValue != null && !newValue.isEmpty()) {
                if (!isProbablyEncrypted(newValue)) {
                    newValue = encryptionService.encrypt(newValue);
                }
            }
            existingConfig.setConfigValue(newValue);
            existingConfig.setIsEncrypted(true);
        } else {
            existingConfig.setConfigValue(rawNewValue);
            existingConfig.setIsEncrypted(updatedConfig.getIsEncrypted() != null ? updatedConfig.getIsEncrypted() : false);
        }
        existingConfig.setConfigType(updatedConfig.getConfigType());
        existingConfig.setDescription(updatedConfig.getDescription());
        
        PlatformConfig saved = platformConfigRepository.save(existingConfig);
        log.info("[PlatformConfigService] updateConfig - success configKey={}", LogMaskingUtils.mask(configKey, 2, 2));

        // 감사 로그
        try {
            if (configAuditService == null) return saved;
            String userId = getCurrentUserId();
            String reason = saved.getDescription();
            String valueType = saved.getConfigType() != null ? saved.getConfigType().name() : null;
            configAuditService.logUpdate(configKey, rawOldValue, rawNewValue, userId, reason, valueType);
        } catch (Exception ex) {
            log.warn("[PlatformConfigService] updateConfig - audit logging failed: {}", ex.getMessage());
        }
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
        

        String rawOldValue = config.getConfigValue();
        config.setIsDeleted(true);
        platformConfigRepository.save(config);
        log.info("[PlatformConfigService] deleteConfig - success configKey={}", LogMaskingUtils.mask(configKey, 2, 2));

        // 감사 로그
        try {
            if (configAuditService == null) return;
            String userId = getCurrentUserId();
            String reason = config.getDescription();
            String valueType = config.getConfigType() != null ? config.getConfigType().name() : null;
            configAuditService.logDelete(configKey, rawOldValue, userId, reason, valueType);
        } catch (Exception ex) {
            log.warn("[PlatformConfigService] deleteConfig - audit logging failed: {}", ex.getMessage());
        }
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

    // 매우 보수적인 이중 암호화 방지 체크: Base64 디코딩 가능하며 IV(12바이트) 이상 길이면 이미 암호문일 가능성으로 간주
    private boolean isProbablyEncrypted(String value) {
        try {
            byte[] decoded = java.util.Base64.getDecoder().decode(value);
            return decoded != null && decoded.length > 12; // AES-GCM IV 12바이트
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    // 응답 변환: ENCRYPTED 타입은 기본 마스킹, showSecret=true 시 복호화하여 평문 반환
    private PlatformConfig toResponse(PlatformConfig source, boolean showSecret) {
        PlatformConfig.PlatformConfigBuilder builder = PlatformConfig.builder()
                .configKey(source.getConfigKey())
                .configType(source.getConfigType())
                .description(source.getDescription())
                .isEncrypted(source.getIsEncrypted())
                .isSystem(source.getIsSystem());

        boolean isEncryptedType = source.getConfigType() == PlatformConfig.ConfigType.ENCRYPTED
                || Boolean.TRUE.equals(source.getIsEncrypted());

        if (isEncryptedType) {
            if (showSecret) {
                try {
                    String decrypted = encryptionService.decrypt(source.getConfigValue());
                    builder.configValue(decrypted);
                } catch (IllegalArgumentException e) {
                    throw new BusinessException(PlatformConfigErrorCode.ENCRYPTED_PAYLOAD_INVALID, e.getMessage());
                } catch (RuntimeException e) {
                    throw new BusinessException(PlatformConfigErrorCode.DECRYPTION_FAILED, e.getMessage());
                }
            } else {
                builder.configValue("Encrypted");
            }
        } else {
            builder.configValue(source.getConfigValue());
        }

        return builder.build();
    }

    private String getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                return authentication.getName();
            }
        } catch (Exception ignored) {
        }
        return "system";
    }
}
