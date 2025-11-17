package com.agenticcp.core.domain.tenant.controller;

import com.agenticcp.core.common.audit.AuditController;
import com.agenticcp.core.common.audit.AuditRequired;
import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import com.agenticcp.core.domain.tenant.dto.TenantConfigRequest;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.tenant.entity.TenantTypeConfig;
import com.agenticcp.core.domain.tenant.repository.TenantTypeConfigRepository;
import com.agenticcp.core.domain.tenant.service.TenantConfigCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

/**
 * 테넌트 타입별 기본 설정 관리 컨트롤러
 * 
 * 테넌트 타입(ENTERPRISE, STANDARD, TRIAL)별 기본 설정을 관리하는 API를 제공합니다.
 * 이 설정은 해당 타입의 모든 테넌트에 적용되는 기본값으로 사용되며,
 * 개별 테넌트는 이 값을 오버라이드할 수 있습니다.
 * 
 * 설정 우선순위: TenantConfig > TenantTypeConfig > PlatformConfig
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-23
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/tenant-types/{tenantType}/configs")
@RequiredArgsConstructor
@Tag(name = "Tenant Type Configuration", description = "테넌트 타입별 설정 관리 API")
@AuditController(
    resourceType = AuditResourceType.TENANT_TYPE_CONFIG,
    defaultSeverity = AuditSeverity.HIGH,
    defaultIncludeRequestData = true,
    targetHttpMethods = {"POST", "PUT", "DELETE"}
)
public class TenantTypeConfigController {

    private final TenantTypeConfigRepository tenantTypeConfigRepository;
    private final TenantConfigCacheService tenantConfigCacheService;

    /**
     * 테넌트 타입의 모든 기본 설정 목록 조회
     * 
     * 특정 테넌트 타입(ENTERPRISE, STANDARD, TRIAL)의 모든 기본 설정을 조회합니다.
     * 이 설정은 해당 타입의 모든 테넌트에 적용되는 기본값입니다.
     * 
     * @param tenantType 조회할 테넌트 타입 (ENTERPRISE, STANDARD, TRIAL)
     * @return 테넌트 타입의 기본 설정 목록
     */
    @GetMapping
    @Operation(summary = "테넌트 타입 설정 목록 조회", 
               description = "특정 테넌트 타입의 모든 기본 설정을 조회합니다.")
    @AuditRequired(
        action = "getTenantTypeConfigurations",
        resourceType = AuditResourceType.TENANT_TYPE_CONFIG,
        severity = AuditSeverity.LOW,
        includeRequestData = false
    )
    public ResponseEntity<ApiResponse<List<TenantTypeConfig>>> getTenantTypeConfigurations(
            @Parameter(description = "테넌트 타입") @PathVariable Tenant.TenantType tenantType) {
        
        log.info("[TenantTypeConfigController] getTenantTypeConfigurations - tenantType={}", tenantType);
        
        List<TenantTypeConfig> configs = tenantTypeConfigRepository.findByTenantTypeAndIsDeletedFalse(tenantType);
        
        return ResponseEntity.ok(ApiResponse.success(configs));
    }

    /**
     * 테넌트 타입의 특정 기본 설정 단건 조회
     * 
     * 테넌트 타입의 특정 기본 설정을 조회합니다.
     * 해당 타입의 모든 테넌트에 적용되는 기본값을 확인할 때 사용합니다.
     * 
     * @param tenantType 조회할 테넌트 타입 (ENTERPRISE, STANDARD, TRIAL)
     * @param configKey 조회할 설정 키
     * @return 테넌트 타입의 기본 설정 (없으면 null)
     */
    @GetMapping("/{configKey}")
    @Operation(summary = "테넌트 타입 특정 설정 조회", 
               description = "테넌트 타입의 특정 기본 설정을 조회합니다.")
    @AuditRequired(
        action = "getTenantTypeConfiguration",
        resourceType = AuditResourceType.TENANT_TYPE_CONFIG,
        severity = AuditSeverity.LOW,
        includeRequestData = false
    )
    public ResponseEntity<ApiResponse<TenantTypeConfig>> getTenantTypeConfiguration(
            @Parameter(description = "테넌트 타입") @PathVariable Tenant.TenantType tenantType,
            @Parameter(description = "설정 키") @PathVariable String configKey) {
        
        log.info("[TenantTypeConfigController] getTenantTypeConfiguration - tenantType={}, configKey={}", 
                tenantType, configKey);
        
        Optional<TenantTypeConfig> config = tenantTypeConfigRepository
                .findByTenantTypeAndConfigKeyAndIsDeletedFalse(tenantType, configKey);
        
        return config.map(c -> ResponseEntity.ok(ApiResponse.success(c)))
                .orElse(ResponseEntity.ok(ApiResponse.success(null)));
    }

    /**
     * 테넌트 타입 기본 설정 생성/수정 (Upsert)
     * 
     * 테넌트 타입의 기본 설정을 생성하거나 수정합니다.
     * 같은 configKey가 이미 존재하면 수정, 없으면 신규 생성합니다.
     * 
     * 주의: 이 설정은 해당 타입의 모든 테넌트에 영향을 줍니다.
     * 저장 후 해당 타입의 모든 테넌트 캐시를 자동으로 무효화합니다.
     * 
     * @param tenantType 설정을 저장할 테넌트 타입
     * @param request 설정 요청 정보 (키, 값, 타입, 설명, 암호화 여부)
     * @return 저장된 테넌트 타입 기본 설정
     */
    @PostMapping
    @Operation(summary = "테넌트 타입 설정 생성/수정", 
               description = "테넌트 타입의 기본 설정을 생성하거나 수정합니다.")
    @AuditRequired(
        action = "saveTenantTypeConfiguration",
        resourceType = AuditResourceType.TENANT_TYPE_CONFIG,
        severity = AuditSeverity.HIGH,
        includeRequestData = true
    )
    public ResponseEntity<ApiResponse<TenantTypeConfig>> saveTenantTypeConfiguration(
            @Parameter(description = "테넌트 타입") @PathVariable Tenant.TenantType tenantType,
            @Valid @RequestBody TenantConfigRequest request) {
        
        log.info("[TenantTypeConfigController] saveTenantTypeConfiguration - tenantType={}, configKey={}", 
                tenantType, request.getConfigKey());
        
        // Upsert 패턴: 기존 설정이 있는지 확인
        Optional<TenantTypeConfig> existingConfig = tenantTypeConfigRepository
                .findByTenantTypeAndConfigKeyAndIsDeletedFalse(tenantType, request.getConfigKey());

        TenantTypeConfig config;
        if (existingConfig.isPresent()) {
            // 기존 설정 수정 (UPDATE)
            config = existingConfig.get();
            config.setConfigValue(request.getConfigValue());
            config.setConfigType(mapConfigType(request.getConfigType()));
            config.setDescription(request.getDescription());
            config.setIsEncrypted(request.getIsEncrypted());
            log.info("[TenantTypeConfigController] saveTenantTypeConfiguration - updated existing config");
        } else {
            // 신규 설정 생성 (INSERT)
            config = TenantTypeConfig.builder()
                    .tenantType(tenantType)
                    .configKey(request.getConfigKey())
                    .configValue(request.getConfigValue())
                    .configType(mapConfigType(request.getConfigType()))
                    .description(request.getDescription())
                    .isEncrypted(request.getIsEncrypted())
                    .build();
            log.info("[TenantTypeConfigController] saveTenantTypeConfiguration - created new config");
        }

        TenantTypeConfig savedConfig = tenantTypeConfigRepository.save(config);
        
        // 캐시 무효화: 해당 타입을 사용하는 모든 테넌트의 설정 캐시 갱신
        tenantConfigCacheService.evictCacheByTenantType(tenantType.name());
        
        return ResponseEntity.ok(ApiResponse.success(savedConfig));
    }

    /**
     * 테넌트 타입 기본 설정 삭제 (소프트 삭제)
     * 
     * 테넌트 타입의 특정 기본 설정을 삭제합니다.
     * 물리적 삭제가 아닌 isDeleted 플래그를 설정하는 소프트 삭제입니다.
     * 
     * 주의: 이 설정은 해당 타입의 모든 테넌트에 영향을 줍니다.
     * 삭제 후 해당 타입의 모든 테넌트 캐시를 자동으로 무효화합니다.
     * 
     * @param tenantType 삭제할 설정이 속한 테넌트 타입
     * @param configKey 삭제할 설정 키
     * @return 성공 응답
     */
    @DeleteMapping("/{configKey}")
    @Operation(summary = "테넌트 타입 설정 삭제", 
               description = "테넌트 타입의 기본 설정을 삭제합니다.")
    @AuditRequired(
        action = "deleteTenantTypeConfiguration",
        resourceType = AuditResourceType.TENANT_TYPE_CONFIG,
        severity = AuditSeverity.HIGH,
        includeRequestData = true
    )
    public ResponseEntity<ApiResponse<Void>> deleteTenantTypeConfiguration(
            @Parameter(description = "테넌트 타입") @PathVariable Tenant.TenantType tenantType,
            @Parameter(description = "설정 키") @PathVariable String configKey) {
        
        log.info("[TenantTypeConfigController] deleteTenantTypeConfiguration - tenantType={}, configKey={}", 
                tenantType, configKey);
        
        Optional<TenantTypeConfig> configOpt = tenantTypeConfigRepository
                .findByTenantTypeAndConfigKeyAndIsDeletedFalse(tenantType, configKey);
        
        if (configOpt.isPresent()) {
            // 소프트 삭제: isDeleted 플래그만 설정
            TenantTypeConfig config = configOpt.get();
            config.setIsDeleted(true);
            tenantTypeConfigRepository.save(config);
            
            log.info("[TenantTypeConfigController] deleteTenantTypeConfiguration - soft deleted successfully");
            
            // 캐시 무효화: 해당 타입을 사용하는 모든 테넌트의 설정 캐시 갱신
            tenantConfigCacheService.evictCacheByTenantType(tenantType.name());
        } else {
            log.warn("[TenantTypeConfigController] deleteTenantTypeConfiguration - config not found");
        }
        
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * ConfigType 매핑 헬퍼 메서드
     * 
     * TenantConfig.ConfigType을 TenantTypeConfig.ConfigType으로 변환합니다.
     * TenantConfig와 TenantTypeConfig는 동일한 ConfigType enum을 가지지만
     * 패키지가 다르기 때문에 매핑이 필요합니다.
     * 
     * @param tenantConfigType 변환할 TenantConfig의 ConfigType
     * @return 변환된 TenantTypeConfig의 ConfigType (기본값: STRING)
     */
    private TenantTypeConfig.ConfigType mapConfigType(com.agenticcp.core.domain.tenant.entity.TenantConfig.ConfigType tenantConfigType) {
        // ConfigType 1:1 매핑
        switch (tenantConfigType) {
            case STRING:
                return TenantTypeConfig.ConfigType.STRING;
            case NUMBER:
                return TenantTypeConfig.ConfigType.NUMBER;
            case BOOLEAN:
                return TenantTypeConfig.ConfigType.BOOLEAN;
            case JSON:
                return TenantTypeConfig.ConfigType.JSON;
            case ENCRYPTED:
                return TenantTypeConfig.ConfigType.ENCRYPTED;
            default:
                // 알 수 없는 타입의 경우 STRING으로 폴백
                log.warn("[TenantTypeConfigController] mapConfigType - unknown type: {}, fallback to STRING", tenantConfigType);
                return TenantTypeConfig.ConfigType.STRING;
        }
    }
}
