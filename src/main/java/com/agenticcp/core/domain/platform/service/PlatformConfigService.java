package com.agenticcp.core.domain.platform.service;

import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.crypto.EncryptionService;
import com.agenticcp.core.domain.platform.entity.PlatformConfig;
import com.agenticcp.core.domain.platform.enums.PlatformConfigErrorCode;
import com.agenticcp.core.domain.platform.exception.ConfigValidationException;
import com.agenticcp.core.domain.platform.repository.PlatformConfigRepository;
import com.agenticcp.core.domain.platform.validation.ConfigValidator;
import com.agenticcp.core.domain.platform.event.ConfigChangeEvent;
import com.agenticcp.core.common.util.LogMaskingUtils;
import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.logging.masking.MaskingService;
import com.agenticcp.core.common.logging.masking.MaskingType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

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
    private final ApplicationEventPublisher eventPublisher;
    private final MaskingService maskingService;
    private final com.agenticcp.core.domain.tenant.service.TenantService tenantService;

    /**
     * 테넌트 키를 조회하고 유효성을 검증합니다.
     * 
     * @return 유효한 테넌트 키
     * @throws ConfigValidationException 테넌트가 존재하지 않는 경우
     */
    private String requireTenantKey() {
        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        
        // 테넌트 존재 여부 검증 (보안 및 데이터 무결성 보장)
        if (!tenantService.getTenantByKey(tenantKey).isPresent()) {
            throw new ConfigValidationException(PlatformConfigErrorCode.INVALID_TENANT_KEY);
        }
        
        return tenantKey;
    }

    /**
     * 전체 플랫폼 설정 조회 (민감 정보 마스킹)
     * <p>
     * 활성화된 모든 플랫폼 설정을 조회합니다. 민감 정보는 마스킹되어 반환됩니다.
     * </p>
     *
     * @return 플랫폼 설정 목록 (민감 정보 마스킹)
     */
    public List<PlatformConfig> getAllConfigs() {
        String tenantKey = requireTenantKey();
        log.info("[PlatformConfigService] getAllConfigs - tenantKey={}", tenantKey);
        List<PlatformConfig> result = platformConfigRepository.findAllActiveByTenantId(tenantKey)
                .stream()
                .map(pc -> toResponse(pc, false))
                .toList();
        log.info("[PlatformConfigService] getAllConfigs - success count={} tenantKey={}", result.size(), tenantKey);
        return result;
    }

    /**
     * 전체 플랫폼 설정 조회
     * <p>
     * 활성화된 모든 플랫폼 설정을 조회합니다.
     * </p>
     *
     * @param showSecret 민감 정보 복호화 여부 (true: 복호화, false: 마스킹)
     * @return 플랫폼 설정 목록
     */
    public List<PlatformConfig> getAllConfigs(boolean showSecret) {
        String tenantKey = requireTenantKey();
        log.info("[PlatformConfigService] getAllConfigs - showSecret={} tenantKey={}", showSecret, tenantKey);
        List<PlatformConfig> result = platformConfigRepository.findAllActiveByTenantId(tenantKey)
                .stream()
                .map(pc -> toResponse(pc, showSecret))
                .toList();
        log.info("[PlatformConfigService] getAllConfigs - success count={} tenantKey={}", result.size(), tenantKey);
        return result;
    }

    /**
     * 전체 플랫폼 설정 조회 (시스템/사용자 필터링)
     * <p>
     * 활성화된 플랫폼 설정을 조회하며, isSystem 파라미터로 시스템/사용자 설정을 필터링할 수 있습니다.
     * </p>
     *
     * @param showSecret 민감 정보 복호화 여부 (true: 복호화, false: 마스킹)
     * @param isSystem 시스템 설정 필터 (true: 시스템 설정만, false: 사용자 설정만, null: 전체)
     * @return 플랫폼 설정 목록
     */
    public List<PlatformConfig> getAllConfigs(boolean showSecret, Boolean isSystem) {
        String tenantKey = requireTenantKey();
        log.info("[PlatformConfigService] getAllConfigs - showSecret={}, isSystem={}, tenantKey={}", showSecret, isSystem, tenantKey);

        List<PlatformConfig> sourceConfigs =
                isSystem != null
                        ? platformConfigRepository.findByTenantIdAndIsSystem(tenantKey, isSystem)
                        : platformConfigRepository.findAllActiveByTenantId(tenantKey);

        List<PlatformConfig> result = sourceConfigs.stream()
                .map(pc -> toResponse(pc, showSecret))
                .toList();
        
        log.info("[PlatformConfigService] getAllConfigs - success count={} tenantKey={}", result.size(), tenantKey);
        return result;
    }

    /**
     * 설정 키로 플랫폼 설정 조회 (민감 정보 마스킹)
     * <p>
     * 지정된 설정 키로 플랫폼 설정을 조회합니다. 민감 정보는 마스킹되어 반환됩니다.
     * </p>
     *
     * @param configKey 조회할 설정 키
     * @return 플랫폼 설정 (존재하지 않으면 Optional.empty())
     */
    public Optional<PlatformConfig> getConfigByKey(String configKey) {
        String tenantKey = requireTenantKey();
        log.info("[PlatformConfigService] getConfigByKey - tenantKey={} configKey={}", tenantKey, LogMaskingUtils.mask(configKey, 2, 2));
        Optional<PlatformConfig> result = platformConfigRepository.findByTenantIdAndConfigKey(tenantKey, configKey)
                .map(pc -> toResponse(pc, false));
        log.info("[PlatformConfigService] getConfigByKey - found={} tenantKey={} configKey={}", result.isPresent(), tenantKey, LogMaskingUtils.mask(configKey, 2, 2));
        return result;
    }

    /**
     * 설정 키로 플랫폼 설정 조회
     * <p>
     * 지정된 설정 키로 플랫폼 설정을 조회합니다.
     * </p>
     *
     * @param configKey 조회할 설정 키
     * @param showSecret 민감 정보 복호화 여부 (true: 복호화, false: 마스킹)
     * @return 플랫폼 설정 (존재하지 않으면 Optional.empty())
     */
    public Optional<PlatformConfig> getConfigByKey(String configKey, boolean showSecret) {
        String tenantKey = requireTenantKey();
        log.info("[PlatformConfigService] getConfigByKey - showSecret={} tenantKey={} configKey={}", showSecret, tenantKey, LogMaskingUtils.mask(configKey, 2, 2));
        Optional<PlatformConfig> result = platformConfigRepository.findByTenantIdAndConfigKey(tenantKey, configKey)
                .map(pc -> toResponse(pc, showSecret));
        log.info("[PlatformConfigService] getConfigByKey - found={} tenantKey={} configKey={}", result.isPresent(), tenantKey, LogMaskingUtils.mask(configKey, 2, 2));
        return result;
    }

    /**
     * 캐시가 적용된 설정 키로 플랫폼 설정 조회
     * <p>
     * 캐시를 활용하여 설정을 조회합니다. 민감 정보는 마스킹되어 반환됩니다.
     * 민감 정보를 복호화하여 조회하려면 {@link #getConfigByKey(String, boolean)} 메서드를 사용하세요.
     * </p>
     *
     * @param configKey 조회할 설정 키
     * @return 플랫폼 설정 (존재하지 않으면 Optional.empty(), 민감 정보 마스킹)
     */
    @Cacheable(
            value = "platformConfigs",
            key = "T(java.lang.String).valueOf(T(com.agenticcp.core.common.context.TenantContextHolder).getCurrentTenantKey()) + ':' + #configKey"
    )
    public Optional<PlatformConfig> getCachedConfigByKey(String configKey) {
        String tenantKey = requireTenantKey();
        log.info("[PlatformConfigService] getCachedConfigByKey - tenantKey={} configKey={}", tenantKey, LogMaskingUtils.mask(configKey, 2, 2));
        Optional<PlatformConfig> result = platformConfigRepository.findByTenantIdAndConfigKey(tenantKey, configKey)
                .map(pc -> toResponse(pc, false));
        log.info("[PlatformConfigService] getCachedConfigByKey - found={} tenantKey={} configKey={}", result.isPresent(), tenantKey, LogMaskingUtils.mask(configKey, 2, 2));
        return result;
    }

    /**
     * 설정 키로 플랫폼 설정 조회 (예외 발생)
     * <p>
     * 지정된 설정 키로 플랫폼 설정을 조회합니다. 설정이 존재하지 않으면 ResourceNotFoundException을 발생시킵니다.
     * </p>
     *
     * @param configKey 조회할 설정 키
     * @return 플랫폼 설정
     * @throws ResourceNotFoundException 설정을 찾을 수 없는 경우
     */
    public PlatformConfig getConfigByKeyOrThrow(String configKey) {
        String tenantKey = requireTenantKey();
        log.info("[PlatformConfigService] getConfigByKeyOrThrow - tenantKey={} configKey={}", tenantKey, LogMaskingUtils.mask(configKey, 2, 2));
        PlatformConfig config = platformConfigRepository.findByTenantIdAndConfigKey(tenantKey, configKey)
                .orElseThrow(() -> new ResourceNotFoundException(PlatformConfigErrorCode.CONFIG_NOT_FOUND));
        log.info("[PlatformConfigService] getConfigByKeyOrThrow - success tenantKey={} configKey={}", tenantKey, LogMaskingUtils.mask(configKey, 2, 2));
        return config;
    }

    /**
     * 설정 타입별 플랫폼 설정 조회 (민감 정보 마스킹)
     * <p>
     * 지정된 타입의 플랫폼 설정을 조회합니다. 민감 정보는 마스킹되어 반환됩니다.
     * </p>
     *
     * @param configType 조회할 설정 타입
     * @return 해당 타입의 플랫폼 설정 목록 (민감 정보 마스킹)
     */
    public List<PlatformConfig> getConfigsByType(PlatformConfig.ConfigType configType) {
        String tenantKey = requireTenantKey();
        log.info("[PlatformConfigService] getConfigsByType - tenantKey={} type={}", tenantKey, configType);
        List<PlatformConfig> result = platformConfigRepository.findByTenantIdAndConfigType(tenantKey, configType)
                .stream()
                .map(pc -> toResponse(pc, false))
                .toList();
        log.info("[PlatformConfigService] getConfigsByType - success count={} tenantKey={} type={}", result.size(), tenantKey, configType);
        return result;
    }

    /**
     * 설정 타입별 플랫폼 설정 조회
     * <p>
     * 지정된 타입의 플랫폼 설정을 조회합니다.
     * </p>
     *
     * @param configType 조회할 설정 타입
     * @param showSecret 민감 정보 복호화 여부 (true: 복호화, false: 마스킹)
     * @return 해당 타입의 플랫폼 설정 목록
     */
    public List<PlatformConfig> getConfigsByType(PlatformConfig.ConfigType configType, boolean showSecret) {
        String tenantKey = requireTenantKey();
        log.info("[PlatformConfigService] getConfigsByType - showSecret={} tenantKey={} type={}", showSecret, tenantKey, configType);
        List<PlatformConfig> result = platformConfigRepository.findByTenantIdAndConfigType(tenantKey, configType)
                .stream()
                .map(pc -> toResponse(pc, showSecret))
                .toList();
        log.info("[PlatformConfigService] getConfigsByType - success count={} tenantKey={} type={}", result.size(), tenantKey, configType);
        return result;
    }

    /**
     * 시스템 설정 조회 (민감 정보 마스킹)
     * <p>
     * 시스템 설정(isSystem=true)만 조회합니다. 민감 정보는 마스킹되어 반환됩니다.
     * </p>
     *
     * @return 시스템 설정 목록 (민감 정보 마스킹)
     */
    public List<PlatformConfig> getSystemConfigs() {
        String tenantKey = requireTenantKey();
        log.info("[PlatformConfigService] getSystemConfigs - tenantKey={}", tenantKey);
        List<PlatformConfig> result = platformConfigRepository.findByTenantIdAndIsSystem(tenantKey, true)
                .stream()
                .map(pc -> toResponse(pc, false))
                .toList();
        log.info("[PlatformConfigService] getSystemConfigs - success count={} tenantKey={}", result.size(), tenantKey);
        return result;
    }

    /**
     * 시스템 설정 조회
     * <p>
     * 시스템 설정(isSystem=true)만 조회합니다.
     * </p>
     *
     * @param showSecret 민감 정보 복호화 여부 (true: 복호화, false: 마스킹)
     * @return 시스템 설정 목록
     */
    public List<PlatformConfig> getSystemConfigs(boolean showSecret) {
        String tenantKey = requireTenantKey();
        log.info("[PlatformConfigService] getSystemConfigs - showSecret={} tenantKey={}", showSecret, tenantKey);
        List<PlatformConfig> result = platformConfigRepository.findByTenantIdAndIsSystem(tenantKey, true)
                .stream()
                .map(pc -> toResponse(pc, showSecret))
                .toList();
        log.info("[PlatformConfigService] getSystemConfigs - success count={} tenantKey={}", result.size(), tenantKey);
        return result;
    }

    /**
     * 플랫폼 설정 생성
     * <p>
     * 새로운 플랫폼 설정을 생성합니다. 다음 작업이 수행됩니다:
     * <ul>
     *   <li>네임스페이스 기반으로 isSystem 자동 설정 (system.* 키는 자동으로 isSystem=true)</li>
     *   <li>모든 검증기를 통한 설정 검증</li>
     *   <li>중복 키 검증</li>
     *   <li>ENCRYPTED 타입인 경우 자동 암호화 (이중 암호화 방지)</li>
     *   <li>감사 로그 기록</li>
     *   <li>설정 변경 이벤트 발행</li>
     * </ul>
     * </p>
     *
     * @param platformConfig 생성할 플랫폼 설정 정보
     * @return 생성된 플랫폼 설정
     * @throws ConfigValidationException 설정 검증 실패 시
     * @throws BusinessException 중복 키 또는 암호화 실패 시
     */
    @Transactional
    @CacheEvict(
            value = "platformConfigs",
            key = "T(com.agenticcp.core.common.context.TenantContextHolder).getCurrentTenantKey() + ':' + #platformConfig.configKey",
            beforeInvocation = false
    )
    public PlatformConfig createConfig(PlatformConfig platformConfig) {
        String tenantKey = requireTenantKey();
        log.info("[PlatformConfigService] createConfig - tenantKey={} configKey={} isEncrypted={} type={}",
                tenantKey,
                LogMaskingUtils.mask(platformConfig.getConfigKey(), 2, 2),
                platformConfig.getIsEncrypted(),
                platformConfig.getConfigType());

        platformConfig.setTenantId(tenantKey);
        
        // 네임스페이스 기반 자동 isSystem 설정
        if (platformConfig.getIsSystem() == null) {
            boolean isSystemKey = platformConfig.getConfigKey().matches("^system\\..*");
            platformConfig.setIsSystem(isSystemKey);
            log.info("[PlatformConfigService] createConfig - auto-set isSystem={} for tenantKey={} configKey={}",
                    isSystemKey, tenantKey, LogMaskingUtils.mask(platformConfig.getConfigKey(), 2, 2));
        }
        
        // 설정 검증 수행
        validateConfig(platformConfig);
        
        // 중복 키 검증
        if (platformConfigRepository.findByTenantIdAndConfigKey(tenantKey, platformConfig.getConfigKey()).isPresent()) {
            throw new ConfigValidationException(PlatformConfigErrorCode.CONFIG_ALREADY_EXISTS);
        }

        // ENCRYPTED 타입 저장 시 암호화 적용
        String rawNewValue = platformConfig.getConfigValue();
        if (platformConfig.getConfigType() == PlatformConfig.ConfigType.ENCRYPTED) {
            if (rawNewValue != null && !rawNewValue.isEmpty()) {
                if (!isProbablyEncrypted(rawNewValue)) {
                    try {
                        String encrypted = encryptionService.encrypt(rawNewValue);
                        platformConfig.setConfigValue(encrypted);
                    } catch (Exception e) {
                        log.error("[PlatformConfigService] createConfig - encryption failed for configKey={}", 
                                LogMaskingUtils.mask(platformConfig.getConfigKey(), 2, 2), e);
                        throw new BusinessException(PlatformConfigErrorCode.ENCRYPTION_FAILED, e.getMessage());
                    }
                }
            }
            platformConfig.setIsEncrypted(true);
        }

        PlatformConfig saved = platformConfigRepository.save(platformConfig);
        log.info("[PlatformConfigService] createConfig - success tenantKey={} configKey={}", tenantKey, LogMaskingUtils.mask(saved.getConfigKey(), 2, 2));

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
        
        // 설정 변경 이벤트 발행
        publishConfigChangeEvent(ConfigChangeEvent.ChangeType.CREATE, saved.getConfigKey(), 
                               null, rawNewValue, saved.getIsEncrypted(), saved.getDescription());
        
        return saved;
    }

    /**
     * 플랫폼 설정 수정
     * <p>
     * 기존 플랫폼 설정을 수정합니다. 다음 작업이 수행됩니다:
     * <ul>
     *   <li>시스템 설정의 타입 변경 방지</li>
     *   <li>네임스페이스 기반으로 isSystem 자동 설정 (값이 없는 경우)</li>
     *   <li>모든 검증기를 통한 설정 검증</li>
     *   <li>ENCRYPTED 타입인 경우 자동 암호화 (이중 암호화 방지)</li>
     *   <li>감사 로그 기록</li>
     *   <li>설정 변경 이벤트 발행</li>
     * </ul>
     * </p>
     *
     * @param configKey 수정할 설정 키
     * @param updatedConfig 수정할 플랫폼 설정 정보
     * @return 수정된 플랫폼 설정
     * @throws ResourceNotFoundException 설정을 찾을 수 없는 경우
     * @throws ConfigValidationException 설정 검증 실패 또는 시스템 설정 타입 변경 시도 시
     * @throws BusinessException 암호화 실패 시
     */
    @Transactional
    @CacheEvict(
            value = "platformConfigs",
            key = "T(com.agenticcp.core.common.context.TenantContextHolder).getCurrentTenantKey() + ':' + #configKey",
            beforeInvocation = false
    )
    public PlatformConfig updateConfig(String configKey, PlatformConfig updatedConfig) {
        String tenantKey = requireTenantKey();
        log.info("[PlatformConfigService] updateConfig - tenantKey={} configKey={}", tenantKey, LogMaskingUtils.mask(configKey, 2, 2));
        PlatformConfig existingConfig = platformConfigRepository.findByTenantIdAndConfigKey(tenantKey, configKey)
                .orElseThrow(() -> new ResourceNotFoundException(PlatformConfigErrorCode.CONFIG_NOT_FOUND));
        
        // 시스템 설정 타입 변경 방지
        if (Boolean.TRUE.equals(existingConfig.getIsSystem()) && 
            !existingConfig.getConfigType().equals(updatedConfig.getConfigType())) {
            throw new ConfigValidationException(PlatformConfigErrorCode.SYSTEM_CONFIG_TYPE_CHANGE_FORBIDDEN);
        }
        
        // 업데이트할 설정에 키 설정 (검증을 위해)
        updatedConfig.setConfigKey(configKey);
        updatedConfig.setTenantId(tenantKey);
        
        // 네임스페이스 기반 자동 isSystem 설정 (기존 값이 없는 경우에만)
        if (updatedConfig.getIsSystem() == null) {
            boolean isSystemKey = configKey.matches("^system\\..*");
            updatedConfig.setIsSystem(isSystemKey);
            log.info("[PlatformConfigService] updateConfig - auto-set isSystem={} for tenantKey={} configKey={}",
                    isSystemKey, tenantKey, LogMaskingUtils.mask(configKey, 2, 2));
        }
        
        // 설정 검증 수행
        validateConfig(updatedConfig);

        // ENCRYPTED 타입 업데이트 시 암호화 적용
        String rawOldValue = existingConfig.getConfigValue();
        String rawNewValue = updatedConfig.getConfigValue();
        if (updatedConfig.getConfigType() == PlatformConfig.ConfigType.ENCRYPTED) {
            String newValue = rawNewValue;
            if (newValue != null && !newValue.isEmpty()) {
                if (!isProbablyEncrypted(newValue)) {
                    try {
                        newValue = encryptionService.encrypt(newValue);
                    } catch (Exception e) {
                        log.error("[PlatformConfigService] updateConfig - encryption failed for configKey={}", 
                                LogMaskingUtils.mask(configKey, 2, 2), e);
                        throw new BusinessException(PlatformConfigErrorCode.ENCRYPTION_FAILED, e.getMessage());
                    }
                }
            }
            existingConfig.setConfigValue(newValue);
            existingConfig.setIsEncrypted(true);
        } else {
            existingConfig.setConfigValue(rawNewValue);
            // isEncrypted가 null이면 기존 값 유지, 아니면 새 값 사용
            if (updatedConfig.getIsEncrypted() != null) {
                existingConfig.setIsEncrypted(updatedConfig.getIsEncrypted());
            }
        }
        existingConfig.setConfigType(updatedConfig.getConfigType());
        existingConfig.setDescription(updatedConfig.getDescription());
        
        PlatformConfig saved = platformConfigRepository.save(existingConfig);
        log.info("[PlatformConfigService] updateConfig - success tenantKey={} configKey={}", tenantKey, LogMaskingUtils.mask(configKey, 2, 2));

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
        
        // 설정 변경 이벤트 발행
        publishConfigChangeEvent(ConfigChangeEvent.ChangeType.UPDATE, configKey, 
                               rawOldValue, rawNewValue, saved.getIsEncrypted(), saved.getDescription());
        
        return saved;
    }

    /**
     * 플랫폼 설정 소프트 삭제
     * <p>
     * 플랫폼 설정을 소프트 삭제(isDeleted=true)합니다. 다음 작업이 수행됩니다:
     * <ul>
     *   <li>시스템 설정 삭제 방지</li>
     *   <li>소프트 삭제 처리 (isDeleted=true)</li>
     *   <li>감사 로그 기록</li>
     *   <li>설정 변경 이벤트 발행</li>
     * </ul>
     * </p>
     *
     * @param configKey 삭제할 설정 키
     * @throws ResourceNotFoundException 설정을 찾을 수 없는 경우
     * @throws ConfigValidationException 시스템 설정 삭제 시도 시
     */
    @Transactional
    @CacheEvict(
            value = "platformConfigs",
            key = "T(com.agenticcp.core.common.context.TenantContextHolder).getCurrentTenantKey() + ':' + #configKey",
            beforeInvocation = false
    )
    public void deleteConfig(String configKey) {
        String tenantKey = requireTenantKey();
        log.info("[PlatformConfigService] deleteConfig - tenantKey={} configKey={}", tenantKey, LogMaskingUtils.mask(configKey, 2, 2));
        PlatformConfig config = platformConfigRepository.findByTenantIdAndConfigKey(tenantKey, configKey)
                .orElseThrow(() -> new ResourceNotFoundException(PlatformConfigErrorCode.CONFIG_NOT_FOUND));
        
        // 시스템 설정 삭제 방지
        if (Boolean.TRUE.equals(config.getIsSystem())) {
            throw new ConfigValidationException(PlatformConfigErrorCode.SYSTEM_CONFIG_CANNOT_DELETE);
        }
        

        String rawOldValue = config.getConfigValue();
        config.setIsDeleted(true);
        platformConfigRepository.save(config);
        log.info("[PlatformConfigService] deleteConfig - success tenantKey={} configKey={}", tenantKey, LogMaskingUtils.mask(configKey, 2, 2));

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
        
        // 설정 변경 이벤트 발행
        publishConfigChangeEvent(ConfigChangeEvent.ChangeType.DELETE, configKey, 
                               rawOldValue, null, config.getIsEncrypted(), config.getDescription());
    }

    /**
     * 플랫폼 설정 하드 삭제
     * <p>
     * 플랫폼 설정을 데이터베이스에서 완전히 삭제합니다. 시스템 설정은 삭제할 수 없습니다.
     * 주의: 이 메서드는 감사 로그나 이벤트를 발행하지 않습니다.
     * </p>
     *
     * @param configKey 삭제할 설정 키
     * @throws ResourceNotFoundException 설정을 찾을 수 없는 경우
     * @throws ConfigValidationException 시스템 설정 삭제 시도 시
     */
    @Transactional
    @CacheEvict(
            value = "platformConfigs",
            key = "T(com.agenticcp.core.common.context.TenantContextHolder).getCurrentTenantKey() + ':' + #configKey",
            beforeInvocation = false
    )
    public void hardDeleteConfig(String configKey) {
        String tenantKey = requireTenantKey();
        log.info("[PlatformConfigService] hardDeleteConfig - tenantKey={} configKey={}", tenantKey, LogMaskingUtils.mask(configKey, 2, 2));
        PlatformConfig config = platformConfigRepository.findByTenantIdAndConfigKey(tenantKey, configKey)
                .orElseThrow(() -> new ResourceNotFoundException(PlatformConfigErrorCode.CONFIG_NOT_FOUND));
        
        // 시스템 설정 삭제 방지
        if (Boolean.TRUE.equals(config.getIsSystem())) {
            throw new ConfigValidationException(PlatformConfigErrorCode.SYSTEM_CONFIG_CANNOT_DELETE);
        }
        
        platformConfigRepository.delete(config);
        log.info("[PlatformConfigService] hardDeleteConfig - success tenantKey={} configKey={}", tenantKey, LogMaskingUtils.mask(configKey, 2, 2));
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

    /**
     * 값이 이미 암호화되었는지 확인
     * <p>
     * 매우 보수적인 이중 암호화 방지 체크를 수행합니다.
     * Base64 디코딩이 가능하며 IV(12바이트) 이상 길이면 이미 암호문일 가능성으로 간주합니다.
     * </p>
     *
     * @param value 확인할 값
     * @return 이미 암호화된 것으로 판단되면 true, 그렇지 않으면 false
     */
    private boolean isProbablyEncrypted(String value) {
        try {
            byte[] decoded = java.util.Base64.getDecoder().decode(value);
            return decoded != null && decoded.length > 12; // AES-GCM IV 12바이트
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * 플랫폼 설정 응답 변환
     * <p>
     * 데이터베이스에서 조회한 플랫폼 설정을 응답용으로 변환합니다.
     * ENCRYPTED 타입은 기본적으로 마스킹되며, showSecret=true인 경우 복호화하여 평문을 반환합니다.
     * </p>
     *
     * @param source 원본 플랫폼 설정
     * @param showSecret 민감 정보 복호화 여부 (true: 복호화, false: 마스킹)
     * @return 응답용 플랫폼 설정
     * @throws BusinessException 복호화 실패 시
     */
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
                String configValue = source.getConfigValue();
                if (configValue == null || configValue.isEmpty()) {
                    throw new BusinessException(PlatformConfigErrorCode.ENCRYPTED_PAYLOAD_INVALID, 
                            "암호화된 설정 값이 비어있습니다.");
                }
                
                try {
                    String decrypted = encryptionService.decrypt(configValue);
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

    /**
     * 현재 인증된 사용자 ID 조회
     * <p>
     * SecurityContext에서 현재 인증된 사용자 ID를 조회합니다.
     * 인증 정보가 없거나 조회에 실패하면 "system"을 반환합니다.
     * </p>
     *
     * @return 현재 사용자 ID 또는 "system"
     */
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
    
    /**
     * 설정 값을 마스킹하여 이벤트에 안전하게 전달
     * 
     * @param configValue 원본 설정 값
     * @param isEncrypted 암호화 여부
     * @return 마스킹된 값
     */
    private String maskValueForEvent(String configValue, Boolean isEncrypted) {
        if (configValue == null) {
            return null;
        }
        
        // 암호화된 값이거나 이미 암호문인 경우 "Encrypted"로 마스킹
        if (Boolean.TRUE.equals(isEncrypted) || isProbablyEncrypted(configValue)) {
            return "Encrypted";
        }
        
        // 일반 값은 MaskingService를 사용하여 SECRET_KEY 타입으로 마스킹
        return maskingService.applyMaskingStrategy(configValue, MaskingType.SECRET_KEY);
    }
    
    /**
     * 설정 변경 이벤트 발행
     * 
     * @param changeType 변경 타입
     * @param configKey 설정 키
     * @param oldValue 이전 값
     * @param newValue 새로운 값
     * @param isEncrypted 암호화 여부
     * @param reason 변경 사유
     */
    private void publishConfigChangeEvent(ConfigChangeEvent.ChangeType changeType, String configKey, 
                                        String oldValue, String newValue, Boolean isEncrypted, String reason) {
        try {
            String tenantKey = requireTenantKey();
            String userId = getCurrentUserId();
            String oldValueMasked = maskValueForEvent(oldValue, isEncrypted);
            String newValueMasked = maskValueForEvent(newValue, isEncrypted);
            
            ConfigChangeEvent event;
            switch (changeType) {
                case CREATE:
                    event = ConfigChangeEvent.create(tenantKey, configKey, newValueMasked, newValue, userId, reason);
                    break;
                case UPDATE:
                    event = ConfigChangeEvent.update(tenantKey, configKey, oldValueMasked, newValueMasked, newValue, userId, reason);
                    break;
                case DELETE:
                    event = ConfigChangeEvent.delete(tenantKey, configKey, oldValueMasked, userId, reason);
                    break;
                default:
                    log.warn("[PlatformConfigService] Unknown change type: {}", changeType);
                    return;
            }
            
            eventPublisher.publishEvent(event);
            log.debug("[PlatformConfigService] Config change event published: tenantKey={} configKey={}, changeType={}", 
                     tenantKey, LogMaskingUtils.mask(configKey, 2, 2), changeType);
        } catch (Exception ex) {
            log.warn("[PlatformConfigService] Failed to publish config change event: {}", ex.getMessage());
        }
    }
}
