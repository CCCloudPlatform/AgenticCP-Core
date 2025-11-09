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
 * 
 * 설정 우선순위:
 * 1. TenantConfig (개별 테넌트 설정) - 최우선
 * 2. TenantTypeConfig (테넌트 타입별 기본 설정) - 중간
 * 3. PlatformConfig (플랫폼 전역 설정) - 최하위
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-23
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
     * 
     * 플랫폼, 테넌트 타입, 개별 테넌트의 설정을 계층적으로 병합하여
     * 최종 유효 설정값과 출처 정보를 반환합니다.
     * 
     * @param tenantKey 조회할 테넌트 키
     * @return 유효 설정 응답 (설정값과 출처 정보 포함)
     */
    public EffectiveConfigResponse getEffectiveConfigurations(String tenantKey) {
        log.info("[TenantConfigInheritanceService] getEffectiveConfigurations - tenantKey={}", 
                LogMaskingUtils.maskTenantKey(tenantKey));

        Tenant tenant = tenantService.getTenantByKeyOrThrow(tenantKey);
        Map<String, EffectiveConfigResponse.ConfigValueWithSource> configurations = new HashMap<>();

        // 설정 상속 프로세스 (하위 레벨이 상위 레벨을 오버라이드)
        // 1. 플랫폼 전역 설정 로드 (기본값)
        loadPlatformConfigurations(configurations);

        // 2. 테넌트 타입별 기본 설정 적용 (플랫폼 설정 오버라이드)
        // 예: ENTERPRISE 플랜의 기본 스펙 적용
        loadTenantTypeConfigurations(tenant.getTenantType(), configurations);

        // 3. 개별 테넌트 설정 적용 (최종 오버라이드)
        // 예: 특별 계약 고객의 맞춤 설정
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
     * 
     * 상속 순서(개별 테넌트 → 테넌트 타입 → 플랫폼)에 따라
     * 가장 우선순위가 높은 설정값을 반환합니다.
     * 
     * @param tenantKey 조회할 테넌트 키
     * @param configKey 조회할 설정 키
     * @return 유효 설정값 (파싱된 객체), 없으면 null
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
     * 
     * 플랫폼 전체에 적용되는 기본 설정을 맵에 추가합니다.
     * 
     * @param configurations 설정을 저장할 맵
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
     * 
     * 테넌트 타입(ENTERPRISE, STANDARD, TRIAL)에 해당하는 기본 설정을 로드하여
     * 플랫폼 설정을 오버라이드합니다.
     * 
     * @param tenantType 테넌트 타입
     * @param configurations 설정을 저장할 맵 (기존 값 오버라이드)
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
     * 
     * 특정 테넌트만의 고유 설정을 로드하여
     * 플랫폼 및 타입 설정을 최종 오버라이드합니다.
     * 
     * @param tenant 테넌트 엔티티
     * @param configurations 설정을 저장할 맵 (최종 오버라이드)
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
     * 설정값 파싱 (타입 다형성 처리)
     * 
     * 다양한 ConfigType(TenantConfig, TenantTypeConfig, PlatformConfig)을
     * 처리하기 위한 오버로딩 메서드 디스패처입니다.
     * 
     * @param configValue 파싱할 설정값 문자열
     * @param configType 설정 타입 (다형성)
     * @return 파싱된 객체 (String, Long, Double, Boolean 등)
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

    /**
     * TenantConfig.ConfigType 기반 설정값 파싱
     * 
     * @param configValue 파싱할 설정값
     * @param configType 테넌트 설정 타입
     * @return 파싱된 객체
     */
    private Object parseConfigValue(String configValue, TenantConfig.ConfigType configType) {
        return parseConfigValueByType(configValue, configType.name());
    }

    /**
     * TenantTypeConfig.ConfigType 기반 설정값 파싱
     * 
     * @param configValue 파싱할 설정값
     * @param configType 테넌트 타입 설정 타입
     * @return 파싱된 객체
     */
    private Object parseConfigValue(String configValue, TenantTypeConfig.ConfigType configType) {
        return parseConfigValueByType(configValue, configType.name());
    }

    /**
     * PlatformConfig.ConfigType 기반 설정값 파싱
     * 
     * @param configValue 파싱할 설정값
     * @param configType 플랫폼 설정 타입
     * @return 파싱된 객체
     */
    private Object parseConfigValue(String configValue, PlatformConfig.ConfigType configType) {
        return parseConfigValueByType(configValue, configType.name());
    }

    /**
     * 타입 이름 기반 설정값 파싱
     * 
     * 타입에 따라 문자열을 적절한 객체로 변환합니다:
     * - NUMBER: 소수점 있으면 Double, 없으면 Long
     * - BOOLEAN: Boolean
     * - JSON: String (그대로 반환, 필요 시 클라이언트에서 파싱)
     * - 기타: String
     * 
     * @param configValue 파싱할 설정값
     * @param typeName 타입 이름 (STRING, NUMBER, BOOLEAN, JSON, ENCRYPTED)
     * @return 파싱된 객체, 파싱 실패 시 원본 문자열 반환
     */
    private Object parseConfigValueByType(String configValue, String typeName) {
        try {
            switch (typeName) {
                case "NUMBER":
                    // 숫자 타입: 소수점 포함 여부로 Double/Long 구분
                    if (configValue.contains(".")) {
                        return Double.parseDouble(configValue);  // 실수
                    } else {
                        return Long.parseLong(configValue);      // 정수
                    }
                case "BOOLEAN":
                    // 불리언 타입: "true", "false" 문자열을 Boolean으로 변환
                    return Boolean.parseBoolean(configValue);
                case "JSON":
                    // JSON 타입: 문자열로 반환 (클라이언트에서 필요 시 파싱)
                    // 향후 ObjectMapper를 사용한 검증 추가 고려
                    return configValue;
                default:
                    // STRING, ENCRYPTED 등: 문자열 그대로 반환
                    return configValue;
            }
        } catch (NumberFormatException e) {
            // 파싱 실패 시 경고 로그 후 원본 문자열 반환 (시스템 중단 방지)
            log.warn("[TenantConfigInheritanceService] 설정값 파싱 실패 - value={}, type={}, error={}", 
                    configValue, typeName, e.getMessage());
            return configValue;
        }
    }

    /**
     * TenantTypeConfig.ConfigType을 TenantConfig.ConfigType으로 매핑
     * 
     * 응답 DTO에서 일관된 타입을 사용하기 위한 매핑 메서드입니다.
     * 
     * @param typeConfigType 테넌트 타입 설정 타입
     * @return 테넌트 설정 타입
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
     * 
     * 응답 DTO에서 일관된 타입을 사용하기 위한 매핑 메서드입니다.
     * 
     * @param platformType 플랫폼 설정 타입
     * @return 테넌트 설정 타입
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
