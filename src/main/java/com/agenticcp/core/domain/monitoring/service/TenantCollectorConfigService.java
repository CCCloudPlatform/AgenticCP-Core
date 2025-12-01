package com.agenticcp.core.domain.monitoring.service;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.monitoring.dto.TenantCollectorConfigDto;
import com.agenticcp.core.domain.monitoring.entity.TenantCollectorConfig;
import com.agenticcp.core.domain.monitoring.entity.TenantCollectorMetadata;
import com.agenticcp.core.domain.monitoring.enums.CollectorType;
import com.agenticcp.core.domain.monitoring.enums.MonitoringErrorCode;
import com.agenticcp.core.domain.monitoring.enums.QuotaExceededAction;
import com.agenticcp.core.domain.monitoring.repository.TenantCollectorConfigRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 테넌트별 수집기 설정 서비스
 * 
 * <p>테넌트별 메트릭 수집기 설정을 관리합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TenantCollectorConfigService {

    private final TenantCollectorConfigRepository repository;
    private final TenantDataRetentionService retentionService;
    private final ObjectMapper objectMapper;

    /**
     * 테넌트별 활성화된 수집기 설정 조회
     *
     * @param tenantId 테넌트 ID
     * @return 활성화된 수집기 설정 목록
     */
    public List<TenantCollectorConfigDto> getEnabledConfigsByTenant(String tenantId) {
        log.info("[TenantCollectorConfigService] getEnabledConfigsByTenant - 테넌트별 활성화된 수집기 설정 조회: tenantId={}", tenantId);
        
        List<TenantCollectorConfig> configs = repository.findEnabledByTenantId(tenantId);
        
        return configs.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 테넌트별 모든 수집기 설정 조회
     *
     * @param tenantId 테넌트 ID
     * @return 모든 수집기 설정 목록
     */
    public List<TenantCollectorConfigDto> getAllConfigsByTenant(String tenantId) {
        log.info("[TenantCollectorConfigService] getAllConfigsByTenant - 테넌트별 모든 수집기 설정 조회: tenantId={}", tenantId);
        
        List<TenantCollectorConfig> configs = repository.findAllByTenantId(tenantId);
        
        return configs.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 특정 수집기 설정 조회
     *
     * @param tenantId 테넌트 ID
     * @param collectorType 수집기 타입
     * @return 수집기 설정
     * @throws ResourceNotFoundException 설정을 찾을 수 없는 경우
     */
    public TenantCollectorConfigDto getConfigByTenantAndType(String tenantId, CollectorType collectorType) {
        log.info("[TenantCollectorConfigService] getConfigByTenantAndType - 특정 수집기 설정 조회: tenantId={}, collectorType={}", tenantId, collectorType);
        
        TenantCollectorConfig config = repository.findByTenantIdAndCollectorType(tenantId, collectorType)
                .orElseThrow(() -> new ResourceNotFoundException(MonitoringErrorCode.COLLECTOR_CONFIG_NOT_FOUND));
        
        return convertToDto(config);
    }

    /**
     * 수집기 설정 생성
     *
     * @param configDto 수집기 설정 DTO
     * @return 생성된 수집기 설정
     * @throws BusinessException 설정 검증 실패 또는 중복 설정인 경우
     */
    @Transactional
    public TenantCollectorConfigDto createConfig(TenantCollectorConfigDto configDto) {
        log.info("[TenantCollectorConfigService] createConfig - 수집기 설정 생성: tenantId={}, collectorType={}", 
                configDto.getTenantId(), configDto.getCollectorType());
        
        // 중복 설정 확인
        if (repository.existsByTenantIdAndCollectorType(configDto.getTenantId(), configDto.getCollectorType())) {
            throw new BusinessException(MonitoringErrorCode.COLLECTOR_CONFIG_ALREADY_EXISTS,
                    "이미 존재하는 수집기 설정입니다.");
        }
        
        // 설정 검증
        validateConfig(configDto);
        
        TenantCollectorConfig config = convertToEntity(configDto);
        TenantCollectorConfig savedConfig = repository.save(config);
        
        // 테넌트별 기본 데이터 보관 정책 자동 생성 (30일)
        try {
            retentionService.createDefaultRetentionPolicy(configDto.getTenantId());
            log.info("[TenantCollectorConfigService] createConfig - 테넌트별 기본 보관 정책 자동 생성 완료: tenantId={}", configDto.getTenantId());
        } catch (Exception e) {
            log.warn("[TenantCollectorConfigService] createConfig - 테넌트별 기본 보관 정책 생성 실패 (설정은 생성됨): tenantId={}, error={}", 
                    configDto.getTenantId(), e.getMessage());
        }
        
        log.info("[TenantCollectorConfigService] createConfig - 수집기 설정 생성 완료: id={}", savedConfig.getId());
        return convertToDto(savedConfig);
    }

    /**
     * 수집기 설정 수정
     *
     * @param configId 설정 ID
     * @param configDto 수집기 설정 DTO
     * @return 수정된 수집기 설정
     * @throws ResourceNotFoundException 설정을 찾을 수 없는 경우
     */
    @Transactional
    public TenantCollectorConfigDto updateConfig(Long configId, TenantCollectorConfigDto configDto) {
        log.info("[TenantCollectorConfigService] updateConfig - 수집기 설정 수정: configId={}", configId);
        
        TenantCollectorConfig existingConfig = repository.findById(configId)
                .orElseThrow(() -> new ResourceNotFoundException(MonitoringErrorCode.COLLECTOR_CONFIG_NOT_FOUND));
        
        // 설정 검증
        validateConfig(configDto);
        
        // 설정 업데이트
        updateConfigFields(existingConfig, configDto);
        TenantCollectorConfig savedConfig = repository.save(existingConfig);
        
        log.info("[TenantCollectorConfigService] updateConfig - 수집기 설정 수정 완료: id={}", savedConfig.getId());
        return convertToDto(savedConfig);
    }

    /**
     * 수집기 설정 삭제
     *
     * @param configId 설정 ID
     * @throws ResourceNotFoundException 설정을 찾을 수 없는 경우
     */
    @Transactional
    public void deleteConfig(Long configId) {
        log.info("[TenantCollectorConfigService] deleteConfig - 수집기 설정 삭제: configId={}", configId);
        
        if (!repository.existsById(configId)) {
            throw new ResourceNotFoundException(MonitoringErrorCode.COLLECTOR_CONFIG_NOT_FOUND);
        }
        
        repository.deleteById(configId);
        log.info("[TenantCollectorConfigService] deleteConfig - 수집기 설정 삭제 완료: configId={}", configId);
    }

    /**
     * 수집기 활성화/비활성화
     *
     * @param configId 설정 ID
     * @param enabled 활성화 여부
     * @return 변경된 수집기 설정
     * @throws ResourceNotFoundException 설정을 찾을 수 없는 경우
     */
    @Transactional
    public TenantCollectorConfigDto toggleConfig(Long configId, boolean enabled) {
        log.info("[TenantCollectorConfigService] toggleConfig - 수집기 활성화 상태 변경: configId={}, enabled={}", configId, enabled);
        
        TenantCollectorConfig config = repository.findById(configId)
                .orElseThrow(() -> new ResourceNotFoundException(MonitoringErrorCode.COLLECTOR_CONFIG_NOT_FOUND));
        
        config.setEnabled(enabled);
        TenantCollectorConfig savedConfig = repository.save(config);
        
        log.info("[TenantCollectorConfigService] toggleConfig - 수집기 활성화 상태 변경 완료: id={}, enabled={}", savedConfig.getId(), enabled);
        return convertToDto(savedConfig);
    }

    /**
     * 테넌트별 활성화된 수집기 타입 목록 조회
     *
     * @param tenantId 테넌트 ID
     * @return 활성화된 수집기 타입 목록
     */
    public List<CollectorType> getEnabledCollectorTypesByTenant(String tenantId) {
        log.info("[TenantCollectorConfigService] getEnabledCollectorTypesByTenant - 테넌트별 활성화된 수집기 타입 조회: tenantId={}", tenantId);
        
        return repository.findEnabledCollectorTypesByTenantId(tenantId);
    }

    /**
     * 특정 수집기 타입을 사용하는 테넌트 목록 조회
     *
     * @param collectorType 수집기 타입
     * @return 테넌트 ID 목록
     */
    public List<String> getTenantIdsByCollectorType(CollectorType collectorType) {
        log.info("[TenantCollectorConfigService] getTenantIdsByCollectorType - 특정 수집기 타입 사용 테넌트 조회: collectorType={}", collectorType);
        
        return repository.findTenantIdsByCollectorType(collectorType);
    }

    /**
     * 테넌트별 활성화된 수집기 수 조회
     *
     * @param tenantId 테넌트 ID
     * @return 활성화된 수집기 수
     */
    public long countEnabledByTenant(String tenantId) {
        return repository.countEnabledByTenantId(tenantId);
    }

    /**
     * 설정 검증
     *
     * @param configDto 수집기 설정 DTO
     * @throws BusinessException 설정이 유효하지 않은 경우
     */
    private void validateConfig(TenantCollectorConfigDto configDto) {
        if (configDto.getTenantId() == null || configDto.getTenantId().trim().isEmpty()) {
            throw new BusinessException(MonitoringErrorCode.INVALID_TENANT_ID, "테넌트 ID가 유효하지 않습니다.");
        }
        
        if (configDto.getCollectorType() == null) {
            throw new BusinessException(MonitoringErrorCode.INVALID_COLLECTOR_TYPE, "수집기 타입이 유효하지 않습니다.");
        }
        
        if (configDto.getCollectionInterval() != null && configDto.getCollectionInterval() <= 0) {
            throw new BusinessException(MonitoringErrorCode.INVALID_COLLECTION_INTERVAL, "수집 주기가 유효하지 않습니다.");
        }
        
        if (configDto.getRetryCount() != null && configDto.getRetryCount() < 0) {
            throw new BusinessException(MonitoringErrorCode.INVALID_RETRY_COUNT, "재시도 횟수가 유효하지 않습니다.");
        }
        
        if (configDto.getTimeout() != null && configDto.getTimeout() <= 0) {
            throw new BusinessException(MonitoringErrorCode.INVALID_TIMEOUT, "타임아웃이 유효하지 않습니다.");
        }
    }

    /**
     * 설정 필드 업데이트
     *
     * @param config 수집기 설정 엔티티
     * @param configDto 수집기 설정 DTO
     */
    private void updateConfigFields(TenantCollectorConfig config, TenantCollectorConfigDto configDto) {
        if (configDto.getIsEnabled() != null) {
            config.setEnabled(configDto.getIsEnabled());
        }
        
        if (configDto.getCollectionInterval() != null) {
            config.updateCollectionInterval(configDto.getCollectionInterval());
        }
        
        if (configDto.getRetryCount() != null) {
            config.updateRetryCount(configDto.getRetryCount());
        }
        
        if (configDto.getTimeout() != null) {
            config.updateTimeout(configDto.getTimeout());
        }
        
        if (configDto.getTargetMetrics() != null) {
            try {
                String targetMetricsJson = objectMapper.writeValueAsString(configDto.getTargetMetrics());
                config.updateTargetMetrics(targetMetricsJson);
            } catch (Exception e) {
                log.warn("[TenantCollectorConfigService] updateConfigFields - 타겟 메트릭 JSON 변환 실패: {}", e.getMessage());
            }
        }
        
        if (configDto.getCollectorSettings() != null) {
            try {
                String settingsJson = objectMapper.writeValueAsString(configDto.getCollectorSettings());
                config.updateCollectorSettings(settingsJson);
            } catch (Exception e) {
                log.warn("[TenantCollectorConfigService] updateConfigFields - 수집기 설정 JSON 변환 실패: {}", e.getMessage());
            }
        }
        
        if (configDto.getPriority() != null) {
            config.updatePriority(configDto.getPriority());
        }
    }

    /**
     * Entity를 DTO로 변환
     *
     * @param config 수집기 설정 엔티티
     * @return 수집기 설정 DTO
     * @throws BusinessException 변환 중 오류 발생 시
     */
    private TenantCollectorConfigDto convertToDto(TenantCollectorConfig config) {
        try {
            List<String> targetMetrics = null;
            if (config.getTargetMetrics() != null) {
                targetMetrics = objectMapper.readValue(config.getTargetMetrics(), new TypeReference<List<String>>() {});
            }
            
            Map<String, Object> collectorSettings = null;
            if (config.getCollectorSettings() != null) {
                collectorSettings = objectMapper.readValue(config.getCollectorSettings(), new TypeReference<Map<String, Object>>() {});
            }
            
            Map<String, String> metadata = config.getMetadataList().stream()
                    .collect(Collectors.toMap(
                            TenantCollectorMetadata::getMetadataKey,
                            TenantCollectorMetadata::getMetadataValue
                    ));
            
            // 할당량 관련 계산
            Boolean isQuotaExceeded = config.isQuotaExceeded();
            Double dailyUsagePercentage = null;
            Double storageUsagePercentage = null;
            
            if (config.getDailyMetricLimit() != null && config.getCurrentDailyUsage() != null) {
                dailyUsagePercentage = (double) config.getCurrentDailyUsage() / config.getDailyMetricLimit() * 100;
            }
            
            if (config.getStorageQuotaMb() != null && config.getCurrentStorageUsageMb() != null) {
                storageUsagePercentage = (double) config.getCurrentStorageUsageMb() / config.getStorageQuotaMb() * 100;
            }
            
            return TenantCollectorConfigDto.builder()
                    .tenantId(config.getTenantId())
                    .collectorType(config.getCollectorType())
                    .isEnabled(config.getIsEnabled())
                    .collectionInterval(config.getCollectionInterval())
                    .retryCount(config.getRetryCount())
                    .timeout(config.getTimeout())
                    .targetMetrics(targetMetrics)
                    .collectorSettings(collectorSettings)
                    .priority(config.getPriority())
                    .metadata(metadata)
                    // 할당량 관련 필드들
                    .dailyMetricLimit(config.getDailyMetricLimit())
                    .storageQuotaMb(config.getStorageQuotaMb())
                    .currentDailyUsage(config.getCurrentDailyUsage())
                    .currentStorageUsageMb(config.getCurrentStorageUsageMb())
                    .quotaExceededAction(config.getQuotaExceededAction())
                    .lastResetAt(config.getLastResetAt())
                    .isQuotaExceeded(isQuotaExceeded)
                    .dailyUsagePercentage(dailyUsagePercentage)
                    .storageUsagePercentage(storageUsagePercentage)
                    .build();
                    
        } catch (Exception e) {
            log.error("[TenantCollectorConfigService] convertToDto - DTO 변환 중 오류 발생: {}", e.getMessage(), e);
            throw new BusinessException(MonitoringErrorCode.CONFIG_CONVERSION_FAILED, 
                    "설정 변환 중 오류가 발생했습니다.");
        }
    }

    /**
     * DTO를 Entity로 변환
     *
     * @param configDto 수집기 설정 DTO
     * @return 수집기 설정 엔티티
     * @throws BusinessException 변환 중 오류 발생 시
     */
    private TenantCollectorConfig convertToEntity(TenantCollectorConfigDto configDto) {
        try {
            String targetMetricsJson = null;
            if (configDto.getTargetMetrics() != null) {
                targetMetricsJson = objectMapper.writeValueAsString(configDto.getTargetMetrics());
            }
            
            String collectorSettingsJson = null;
            if (configDto.getCollectorSettings() != null) {
                collectorSettingsJson = objectMapper.writeValueAsString(configDto.getCollectorSettings());
            }
            
            return TenantCollectorConfig.builder()
                    .tenantId(configDto.getTenantId())
                    .collectorType(configDto.getCollectorType())
                    .isEnabled(configDto.getIsEnabled())
                    .collectionInterval(configDto.getCollectionInterval())
                    .retryCount(configDto.getRetryCount())
                    .timeout(configDto.getTimeout())
                    .targetMetrics(targetMetricsJson)
                    .collectorSettings(collectorSettingsJson)
                    .priority(configDto.getPriority())
                    .build();
                    
        } catch (Exception e) {
            log.error("[TenantCollectorConfigService] convertToEntity - Entity 변환 중 오류 발생: {}", e.getMessage(), e);
            throw new BusinessException(MonitoringErrorCode.CONFIG_CONVERSION_FAILED, 
                    "설정 변환 중 오류가 발생했습니다.");
        }
    }

    // ===== 할당량 관련 메서드들 =====
    
    /**
     * 테넌트별 할당량 설정
     *
     * @param tenantId 테넌트 ID
     * @param dailyMetricLimit 일일 메트릭 제한
     * @param storageQuotaMb 저장 공간 할당량 (MB)
     * @param quotaExceededAction 할당량 초과 시 동작
     */
    @Transactional
    public void setQuotaForTenant(String tenantId, Long dailyMetricLimit, Long storageQuotaMb, 
                                 QuotaExceededAction quotaExceededAction) {
        log.info("[TenantCollectorConfigService] setQuotaForTenant - 테넌트별 할당량 설정: tenantId={}, dailyLimit={}, storageQuota={}, action={}", 
                tenantId, dailyMetricLimit, storageQuotaMb, quotaExceededAction);
        
        List<TenantCollectorConfig> configs = repository.findAllByTenantId(tenantId);
        
        for (TenantCollectorConfig config : configs) {
            config.updateQuota(dailyMetricLimit, storageQuotaMb, quotaExceededAction);
            repository.save(config);
        }
        
        log.info("[TenantCollectorConfigService] setQuotaForTenant - 테넌트별 할당량 설정 완료: tenantId={}", tenantId);
    }
    
    /**
     * 테넌트별 할당량 조회
     *
     * @param tenantId 테넌트 ID
     * @return 할당량 정보가 포함된 수집기 설정
     * @throws BusinessException 테넌트 설정을 찾을 수 없는 경우
     */
    public TenantCollectorConfigDto getQuotaForTenant(String tenantId) {
        log.info("[TenantCollectorConfigService] getQuotaForTenant - 테넌트별 할당량 조회: tenantId={}", tenantId);
        
        List<TenantCollectorConfig> configs = repository.findAllByTenantId(tenantId);
        if (configs.isEmpty()) {
            throw new BusinessException(MonitoringErrorCode.COLLECTOR_CONFIG_NOT_FOUND, 
                    "테넌트 설정을 찾을 수 없습니다: " + tenantId);
        }
        
        // 첫 번째 설정의 할당량 정보 반환
        TenantCollectorConfig firstConfig = configs.get(0);
        return convertToDto(firstConfig);
    }
    
    /**
     * 할당량 초과 여부 확인
     *
     * @param tenantId 테넌트 ID
     * @return 할당량 초과 여부
     */
    public boolean isQuotaExceeded(String tenantId) {
        List<TenantCollectorConfig> configs = repository.findAllByTenantId(tenantId);
        
        for (TenantCollectorConfig config : configs) {
            if (config.isQuotaExceeded()) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 일일 사용량 증가
     *
     * @param tenantId 테넌트 ID
     * @param amount 증가할 사용량
     */
    @Transactional
    public void incrementDailyUsage(String tenantId, Long amount) {
        log.debug("[TenantCollectorConfigService] incrementDailyUsage - 테넌트별 일일 사용량 증가: tenantId={}, amount={}", tenantId, amount);
        
        List<TenantCollectorConfig> configs = repository.findAllByTenantId(tenantId);
        
        for (TenantCollectorConfig config : configs) {
            config.incrementDailyUsage(amount);
            repository.save(config);
        }
    }
    
    /**
     * 저장 공간 사용량 증가
     *
     * @param tenantId 테넌트 ID
     * @param amountMb 증가할 저장 공간 사용량 (MB)
     */
    @Transactional
    public void incrementStorageUsage(String tenantId, Long amountMb) {
        log.debug("[TenantCollectorConfigService] incrementStorageUsage - 테넌트별 저장 공간 사용량 증가: tenantId={}, amount={}MB", tenantId, amountMb);
        
        List<TenantCollectorConfig> configs = repository.findAllByTenantId(tenantId);
        
        for (TenantCollectorConfig config : configs) {
            config.incrementStorageUsage(amountMb);
            repository.save(config);
        }
    }
    
    /**
     * 일일 사용량 리셋 (매일 자정에 실행)
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void resetDailyUsage() {
        log.info("[TenantCollectorConfigService] resetDailyUsage - 일일 사용량 리셋 시작");
        
        List<TenantCollectorConfig> allConfigs = repository.findAll();
        
        for (TenantCollectorConfig config : allConfigs) {
            config.resetDailyUsage();
            repository.save(config);
        }
        
        log.info("[TenantCollectorConfigService] resetDailyUsage - 일일 사용량 리셋 완료: {} 개 설정", allConfigs.size());
    }
    
    /**
     * 할당량 초과 동작 실행
     *
     * @param tenantId 테넌트 ID
     */
    @Transactional
    public void handleQuotaExceeded(String tenantId) {
        log.warn("[TenantCollectorConfigService] handleQuotaExceeded - 할당량 초과 처리: tenantId={}", tenantId);
        
        List<TenantCollectorConfig> configs = repository.findAllByTenantId(tenantId);
        
        for (TenantCollectorConfig config : configs) {
            if (config.isQuotaExceeded()) {
                QuotaExceededAction action = config.getQuotaExceededAction();
                
                switch (action) {
                    case BLOCK_COLLECTION -> {
                        config.setEnabled(false);
                        log.warn("[TenantCollectorConfigService] handleQuotaExceeded - 수집기 비활성화: tenantId={}, collectorType={}", 
                                tenantId, config.getCollectorType());
                    }
                    case THROTTLE_COLLECTION -> {
                        // 수집 주기를 2배로 늘림
                        Long newInterval = config.getCollectionInterval() * 2;
                        config.updateCollectionInterval(newInterval);
                        log.warn("[TenantCollectorConfigService] handleQuotaExceeded - 수집 주기 조절: tenantId={}, newInterval={}", 
                                tenantId, newInterval);
                    }
                    case WARN_ONLY -> {
                        log.warn("[TenantCollectorConfigService] handleQuotaExceeded - 할당량 초과 경고: tenantId={}, collectorType={}", 
                                tenantId, config.getCollectorType());
                    }
                    case AUTO_UPGRADE -> {
                        // 자동 업그레이드 로직 (추후 구현)
                        log.info("[TenantCollectorConfigService] handleQuotaExceeded - 자동 업그레이드 요청: tenantId={}", tenantId);
                    }
                }
                
                repository.save(config);
            }
        }
    }
}
