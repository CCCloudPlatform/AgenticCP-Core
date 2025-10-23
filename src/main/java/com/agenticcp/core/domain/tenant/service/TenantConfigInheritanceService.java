package com.agenticcp.core.domain.tenant.service;

import com.agenticcp.core.domain.platform.entity.PlatformConfig;
import com.agenticcp.core.domain.platform.service.PlatformConfigService;
import com.agenticcp.core.domain.tenant.dto.EffectiveConfigResponse;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.tenant.entity.TenantConfig;
import com.agenticcp.core.domain.tenant.entity.TenantTypeConfig;
import com.agenticcp.core.domain.tenant.repository.TenantConfigRepository;
import com.agenticcp.core.domain.tenant.repository.TenantTypeConfigRepository;
import com.agenticcp.core.common.util.LogMaskingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 테넌트 설정 상속 서비스
 * 
 * 플랫폼 전역 설정 → 테넌트 타입 기본 설정 → 개별 테넌트 설정 순서로 상속하여
 * 테넌트별 유효 설정을 계산합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TenantConfigInheritanceService {

    private final PlatformConfigService platformConfigService;
    private final TenantService tenantService;
    private final TenantConfigRepository tenantConfigRepository;
    private final TenantTypeConfigRepository tenantTypeConfigRepository;

    /**
     * 테넌트의 모든 유효 설정 조회
     * 설정값과 그 출처 정보를 포함합니다.
     */
    public EffectiveConfigResponse getEffectiveConfigurations(String tenantKey) {
        log.info("[TenantConfigInheritanceService] getEffectiveConfigurations - tenantKey={}", 
                LogMaskingUtils.maskTenantKey(tenantKey));

        Tenant tenant = tenantService.getTenantByKeyOrThrow(tenantKey);
        Map<String, EffectiveConfigResponse.ConfigValueWithSource> configurations = new HashMap<>();

        // 1. 플랫폼 전역 설정 로드
        loadPlatformConfigurations(configurations);

        // 2. 테넌트 타입별 기본 설정 적용 (오버라이드)
        loadTenantTypeConfigurations(tenant.getTenantType(), configurations);

        // 3. 개별 테넌트 설정 적용 (최종 오버라이드)
        loadTenantConfigurations(tenant, configurations);

        EffectiveConfigResponse response = EffectiveConfigResponse.builder()
                .tenantKey(tenantKey)
                .configurations(configurations)
                .build();

        log.info("[TenantConfigInheritanceService] getEffectiveConfigurations - success tenantKey={}, configCount={}", 
                LogMaskingUtils.maskTenantKey(tenantKey), configurations.size());

        return response;
    }

    /**
     * 테넌트의 특정 설정값 조회
     * 상속 순서에 따라 유효한 값을 반환합니다.
     */
    public Object getEffectiveConfiguration(String tenantKey, String configKey) {
        log.info("[TenantConfigInheritanceService] getEffectiveConfiguration - tenantKey={}, configKey={}", 
                LogMaskingUtils.maskTenantKey(tenantKey), configKey);

        Tenant tenant = tenantService.getTenantByKeyOrThrow(tenantKey);

        // 1. 개별 테넌트 설정 확인 (최우선)
        Optional<TenantConfig> tenantConfig = tenantConfigRepository
                .findByTenantAndConfigKeyAndIsDeletedFalse(tenant, configKey);
        if (tenantConfig.isPresent()) {
            log.info("[TenantConfigInheritanceService] getEffectiveConfiguration - found in tenant config");
            return parseConfigValue(tenantConfig.get().getConfigValue(), tenantConfig.get().getConfigType());
        }

        // 2. 테넌트 타입별 설정 확인
        Optional<TenantTypeConfig> typeConfig = tenantTypeConfigRepository
                .findByTenantTypeAndConfigKeyAndIsDeletedFalse(tenant.getTenantType(), configKey);
        if (typeConfig.isPresent()) {
            log.info("[TenantConfigInheritanceService] getEffectiveConfiguration - found in tenant type config");
            return parseConfigValue(typeConfig.get().getConfigValue(), typeConfig.get().getConfigType());
        }

        // 3. 플랫폼 전역 설정 확인
        Optional<PlatformConfig> platformConfig = platformConfigService.getConfigByKey(configKey);
        if (platformConfig.isPresent()) {
            log.info("[TenantConfigInheritanceService] getEffectiveConfiguration - found in platform config");
            return parseConfigValue(platformConfig.get().getConfigValue(), platformConfig.get().getConfigType());
        }

        log.info("[TenantConfigInheritanceService] getEffectiveConfiguration - not found tenantKey={}, configKey={}", 
                LogMaskingUtils.maskTenantKey(tenantKey), configKey);
        return null;
    }

    /**
     * 플랫폼 전역 설정 로드
     */
    private void loadPlatformConfigurations(Map<String, EffectiveConfigResponse.ConfigValueWithSource> configurations) {
        List<PlatformConfig> platformConfigs = platformConfigService.getAllConfigs();
        for (PlatformConfig config : platformConfigs) {
            configurations.put(config.getConfigKey(), 
                    EffectiveConfigResponse.ConfigValueWithSource.builder()
                            .value(parseConfigValue(config.getConfigValue(), config.getConfigType()))
                            .source(EffectiveConfigResponse.ConfigSource.PLATFORM)
                            .description(config.getDescription())
                            .configType(mapConfigType(config.getConfigType()))
                            .build());
        }
    }

    /**
     * 테넌트 타입별 설정 로드 (오버라이드)
     */
    private void loadTenantTypeConfigurations(Tenant.TenantType tenantType, 
                                            Map<String, EffectiveConfigResponse.ConfigValueWithSource> configurations) {
        List<TenantTypeConfig> typeConfigs = tenantTypeConfigRepository
                .findByTenantTypeAndIsDeletedFalse(tenantType);
        
        for (TenantTypeConfig config : typeConfigs) {
            configurations.put(config.getConfigKey(),
                    EffectiveConfigResponse.ConfigValueWithSource.builder()
                            .value(parseConfigValue(config.getConfigValue(), config.getConfigType()))
                            .source(EffectiveConfigResponse.ConfigSource.TENANT_TYPE)
                            .description(config.getDescription())
                            .configType(mapConfigType(config.getConfigType()))
                            .build());
        }
    }

    /**
     * 개별 테넌트 설정 로드 (최종 오버라이드)
     */
    private void loadTenantConfigurations(Tenant tenant, 
                                        Map<String, EffectiveConfigResponse.ConfigValueWithSource> configurations) {
        List<TenantConfig> tenantConfigs = tenantConfigRepository
                .findByTenantAndIsDeletedFalse(tenant);
        
        for (TenantConfig config : tenantConfigs) {
            configurations.put(config.getConfigKey(),
                    EffectiveConfigResponse.ConfigValueWithSource.builder()
                            .value(parseConfigValue(config.getConfigValue(), config.getConfigType()))
                            .source(EffectiveConfigResponse.ConfigSource.TENANT)
                            .description(config.getDescription())
                            .configType(config.getConfigType())
                            .build());
        }
    }

    /**
     * 설정값 파싱
     */
    private Object parseConfigValue(String configValue, Object configType) {
        if (configValue == null) {
            return null;
        }

        if (configType instanceof TenantConfig.ConfigType) {
            return parseConfigValue(configValue, (TenantConfig.ConfigType) configType);
        } else if (configType instanceof TenantTypeConfig.ConfigType) {
            return parseConfigValue(configValue, (TenantTypeConfig.ConfigType) configType);
        } else if (configType instanceof PlatformConfig.ConfigType) {
            return parseConfigValue(configValue, (PlatformConfig.ConfigType) configType);
        }

        return configValue;
    }

    private Object parseConfigValue(String configValue, TenantConfig.ConfigType configType) {
        return parseConfigValueByType(configValue, configType.name());
    }

    private Object parseConfigValue(String configValue, TenantTypeConfig.ConfigType configType) {
        return parseConfigValueByType(configValue, configType.name());
    }

    private Object parseConfigValue(String configValue, PlatformConfig.ConfigType configType) {
        return parseConfigValueByType(configValue, configType.name());
    }

    private Object parseConfigValueByType(String configValue, String typeName) {
        try {
            switch (typeName) {
                case "NUMBER":
                    if (configValue.contains(".")) {
                        return Double.parseDouble(configValue);
                    } else {
                        return Long.parseLong(configValue);
                    }
                case "BOOLEAN":
                    return Boolean.parseBoolean(configValue);
                case "JSON":
                    // JSON은 문자열로 반환 (필요시 Jackson으로 파싱)
                    return configValue;
                default:
                    return configValue;
            }
        } catch (Exception e) {
            log.warn("설정값 파싱 실패 - value={}, type={}", configValue, typeName);
            return configValue;
        }
    }

    /**
     * TenantTypeConfig.ConfigType을 TenantConfig.ConfigType으로 매핑
     */
    private TenantConfig.ConfigType mapConfigType(TenantTypeConfig.ConfigType typeConfigType) {
        switch (typeConfigType) {
            case STRING:
                return TenantConfig.ConfigType.STRING;
            case NUMBER:
                return TenantConfig.ConfigType.NUMBER;
            case BOOLEAN:
                return TenantConfig.ConfigType.BOOLEAN;
            case JSON:
                return TenantConfig.ConfigType.JSON;
            case ENCRYPTED:
                return TenantConfig.ConfigType.ENCRYPTED;
            default:
                return TenantConfig.ConfigType.STRING;
        }
    }

    /**
     * PlatformConfig.ConfigType을 TenantConfig.ConfigType으로 매핑
     */
    private TenantConfig.ConfigType mapConfigType(PlatformConfig.ConfigType platformType) {
        switch (platformType) {
            case STRING:
                return TenantConfig.ConfigType.STRING;
            case NUMBER:
                return TenantConfig.ConfigType.NUMBER;
            case BOOLEAN:
                return TenantConfig.ConfigType.BOOLEAN;
            case JSON:
                return TenantConfig.ConfigType.JSON;
            case ENCRYPTED:
                return TenantConfig.ConfigType.ENCRYPTED;
            default:
                return TenantConfig.ConfigType.STRING;
        }
    }
}
