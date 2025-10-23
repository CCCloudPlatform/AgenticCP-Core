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
 * 테넌트 타입별 설정 관리 컨트롤러
 * 
 * 테넌트 타입별 기본 설정을 관리하는 API를 제공합니다.
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
     * 테넌트 타입의 모든 설정 조회
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
     * 테넌트 타입의 특정 설정 조회
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
     * 테넌트 타입 설정 생성/수정
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
        
        // 기존 설정이 있는지 확인
        Optional<TenantTypeConfig> existingConfig = tenantTypeConfigRepository
                .findByTenantTypeAndConfigKeyAndIsDeletedFalse(tenantType, request.getConfigKey());

        TenantTypeConfig config;
        if (existingConfig.isPresent()) {
            // 수정
            config = existingConfig.get();
            config.setConfigValue(request.getConfigValue());
            config.setConfigType(mapConfigType(request.getConfigType()));
            config.setDescription(request.getDescription());
            config.setIsEncrypted(request.getIsEncrypted());
            log.info("[TenantTypeConfigController] saveTenantTypeConfiguration - updated existing config");
        } else {
            // 생성
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
        
        // 해당 타입을 사용하는 모든 테넌트의 캐시 무효화
        tenantConfigCacheService.evictCacheByTenantType(tenantType.name());
        
        return ResponseEntity.ok(ApiResponse.success(savedConfig));
    }

    /**
     * 테넌트 타입 설정 삭제
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
            TenantTypeConfig config = configOpt.get();
            config.setIsDeleted(true);
            tenantTypeConfigRepository.save(config);
            
            // 해당 타입을 사용하는 모든 테넌트의 캐시 무효화
            tenantConfigCacheService.evictCacheByTenantType(tenantType.name());
        }
        
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * TenantConfig.ConfigType을 TenantTypeConfig.ConfigType으로 매핑
     */
    private TenantTypeConfig.ConfigType mapConfigType(com.agenticcp.core.domain.tenant.entity.TenantConfig.ConfigType tenantConfigType) {
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
                return TenantTypeConfig.ConfigType.STRING;
        }
    }
}
