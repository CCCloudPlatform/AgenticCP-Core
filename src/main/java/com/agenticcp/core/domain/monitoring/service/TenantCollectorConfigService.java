package com.agenticcp.core.domain.monitoring.service;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.monitoring.dto.TenantCollectorConfigDto;
import com.agenticcp.core.domain.monitoring.entity.TenantCollectorConfig;
import com.agenticcp.core.domain.monitoring.entity.TenantCollectorMetadata;
import com.agenticcp.core.domain.monitoring.enums.CollectorType;
import com.agenticcp.core.domain.monitoring.enums.MonitoringErrorCode;
import com.agenticcp.core.domain.monitoring.repository.TenantCollectorConfigRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 테넌트별 수집기 설정 서비스
 * 
 * 테넌트별 메트릭 수집기 설정을 관리합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TenantCollectorConfigService {

    private final TenantCollectorConfigRepository repository;
    private final ObjectMapper objectMapper;

    /**
     * 테넌트별 활성화된 수집기 설정 조회
     */
    public List<TenantCollectorConfigDto> getEnabledConfigsByTenant(String tenantId) {
        log.info("테넌트별 활성화된 수집기 설정 조회: tenantId={}", tenantId);
        
        List<TenantCollectorConfig> configs = repository.findEnabledByTenantId(tenantId);
        
        return configs.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 테넌트별 모든 수집기 설정 조회
     */
    public List<TenantCollectorConfigDto> getAllConfigsByTenant(String tenantId) {
        log.info("테넌트별 모든 수집기 설정 조회: tenantId={}", tenantId);
        
        List<TenantCollectorConfig> configs = repository.findAllByTenantId(tenantId);
        
        return configs.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 특정 수집기 설정 조회
     */
    public TenantCollectorConfigDto getConfigByTenantAndType(String tenantId, CollectorType collectorType) {
        log.info("특정 수집기 설정 조회: tenantId={}, collectorType={}", tenantId, collectorType);
        
        TenantCollectorConfig config = repository.findByTenantIdAndCollectorType(tenantId, collectorType)
                .orElseThrow(() -> new ResourceNotFoundException(MonitoringErrorCode.COLLECTOR_CONFIG_NOT_FOUND));
        
        return convertToDto(config);
    }

    /**
     * 수집기 설정 생성
     */
    @Transactional
    public TenantCollectorConfigDto createConfig(TenantCollectorConfigDto configDto) {
        log.info("수집기 설정 생성: tenantId={}, collectorType={}", 
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
        
        log.info("수집기 설정 생성 완료: id={}", savedConfig.getId());
        return convertToDto(savedConfig);
    }

    /**
     * 수집기 설정 수정
     */
    @Transactional
    public TenantCollectorConfigDto updateConfig(Long configId, TenantCollectorConfigDto configDto) {
        log.info("수집기 설정 수정: configId={}", configId);
        
        TenantCollectorConfig existingConfig = repository.findById(configId)
                .orElseThrow(() -> new ResourceNotFoundException(MonitoringErrorCode.COLLECTOR_CONFIG_NOT_FOUND));
        
        // 설정 검증
        validateConfig(configDto);
        
        // 설정 업데이트
        updateConfigFields(existingConfig, configDto);
        TenantCollectorConfig savedConfig = repository.save(existingConfig);
        
        log.info("수집기 설정 수정 완료: id={}", savedConfig.getId());
        return convertToDto(savedConfig);
    }

    /**
     * 수집기 설정 삭제
     */
    @Transactional
    public void deleteConfig(Long configId) {
        log.info("수집기 설정 삭제: configId={}", configId);
        
        if (!repository.existsById(configId)) {
            throw new ResourceNotFoundException(MonitoringErrorCode.COLLECTOR_CONFIG_NOT_FOUND);
        }
        
        repository.deleteById(configId);
        log.info("수집기 설정 삭제 완료: configId={}", configId);
    }

    /**
     * 수집기 활성화/비활성화
     */
    @Transactional
    public TenantCollectorConfigDto toggleConfig(Long configId, boolean enabled) {
        log.info("수집기 활성화 상태 변경: configId={}, enabled={}", configId, enabled);
        
        TenantCollectorConfig config = repository.findById(configId)
                .orElseThrow(() -> new ResourceNotFoundException(MonitoringErrorCode.COLLECTOR_CONFIG_NOT_FOUND));
        
        config.setEnabled(enabled);
        TenantCollectorConfig savedConfig = repository.save(config);
        
        log.info("수집기 활성화 상태 변경 완료: id={}, enabled={}", savedConfig.getId(), enabled);
        return convertToDto(savedConfig);
    }

    /**
     * 테넌트별 활성화된 수집기 타입 목록 조회
     */
    public List<CollectorType> getEnabledCollectorTypesByTenant(String tenantId) {
        log.info("테넌트별 활성화된 수집기 타입 조회: tenantId={}", tenantId);
        
        return repository.findEnabledCollectorTypesByTenantId(tenantId);
    }

    /**
     * 특정 수집기 타입을 사용하는 테넌트 목록 조회
     */
    public List<String> getTenantIdsByCollectorType(CollectorType collectorType) {
        log.info("특정 수집기 타입 사용 테넌트 조회: collectorType={}", collectorType);
        
        return repository.findTenantIdsByCollectorType(collectorType);
    }

    /**
     * 테넌트별 활성화된 수집기 수 조회
     */
    public long countEnabledByTenant(String tenantId) {
        return repository.countEnabledByTenantId(tenantId);
    }

    /**
     * 설정 검증
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
                log.warn("타겟 메트릭 JSON 변환 실패: {}", e.getMessage());
            }
        }
        
        if (configDto.getCollectorSettings() != null) {
            try {
                String settingsJson = objectMapper.writeValueAsString(configDto.getCollectorSettings());
                config.updateCollectorSettings(settingsJson);
            } catch (Exception e) {
                log.warn("수집기 설정 JSON 변환 실패: {}", e.getMessage());
            }
        }
        
        if (configDto.getPriority() != null) {
            config.updatePriority(configDto.getPriority());
        }
    }

    /**
     * Entity를 DTO로 변환
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
                    .build();
                    
        } catch (Exception e) {
            log.error("DTO 변환 중 오류 발생: {}", e.getMessage(), e);
            throw new BusinessException(MonitoringErrorCode.CONFIG_CONVERSION_FAILED, 
                    "설정 변환 중 오류가 발생했습니다.");
        }
    }

    /**
     * DTO를 Entity로 변환
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
            log.error("Entity 변환 중 오류 발생: {}", e.getMessage(), e);
            throw new BusinessException(MonitoringErrorCode.CONFIG_CONVERSION_FAILED, 
                    "설정 변환 중 오류가 발생했습니다.");
        }
    }
}
