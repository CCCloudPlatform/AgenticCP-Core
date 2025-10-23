package com.agenticcp.core.domain.tenant.controller;

import com.agenticcp.core.common.audit.AuditController;
import com.agenticcp.core.common.audit.AuditRequired;
import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import com.agenticcp.core.domain.tenant.dto.EffectiveConfigResponse;
import com.agenticcp.core.domain.tenant.dto.TenantConfigRequest;
import com.agenticcp.core.domain.tenant.entity.TenantConfig;
import com.agenticcp.core.domain.tenant.service.TenantConfigCacheService;
import com.agenticcp.core.domain.tenant.service.TenantConfigInheritanceService;
import com.agenticcp.core.domain.tenant.service.TenantConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 테넌트 설정 관리 컨트롤러
 * 
 * 테넌트별 설정 상속 시스템의 API를 제공합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/tenants/{tenantKey}/configs")
@RequiredArgsConstructor
@Tag(name = "Tenant Configuration", description = "테넌트 설정 관리 API")
@AuditController(
    resourceType = AuditResourceType.TENANT_CONFIG,
    defaultSeverity = AuditSeverity.MEDIUM,
    defaultIncludeRequestData = true,
    targetHttpMethods = {"POST", "PUT", "DELETE"}
)
public class TenantConfigController {

    private final TenantConfigService tenantConfigService;
    private final TenantConfigCacheService tenantConfigCacheService;

    /**
     * 테넌트의 유효 설정 조회 (상속 포함)
     */
    @GetMapping("/effective")
    @Operation(summary = "테넌트 유효 설정 조회", 
               description = "플랫폼 전역 설정, 테넌트 타입 기본 설정, 개별 테넌트 설정을 상속하여 최종 유효 설정을 조회합니다.")
    @AuditRequired(
        action = "getEffectiveConfigurations",
        resourceType = AuditResourceType.TENANT_CONFIG,
        severity = AuditSeverity.LOW,
        includeRequestData = false
    )
    public ResponseEntity<ApiResponse<EffectiveConfigResponse>> getEffectiveConfigurations(
            @Parameter(description = "테넌트 키") @PathVariable String tenantKey) {
        
        log.info("[TenantConfigController] getEffectiveConfigurations - tenantKey={}", tenantKey);
        
        EffectiveConfigResponse response = tenantConfigCacheService.getCachedConfigurations(tenantKey);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 테넌트의 특정 유효 설정 조회
     */
    @GetMapping("/effective/{configKey}")
    @Operation(summary = "테넌트 특정 유효 설정 조회", 
               description = "특정 설정 키에 대한 테넌트의 유효 설정값을 조회합니다.")
    @AuditRequired(
        action = "getEffectiveConfiguration",
        resourceType = AuditResourceType.TENANT_CONFIG,
        severity = AuditSeverity.LOW,
        includeRequestData = false
    )
    public ResponseEntity<ApiResponse<Object>> getEffectiveConfiguration(
            @Parameter(description = "테넌트 키") @PathVariable String tenantKey,
            @Parameter(description = "설정 키") @PathVariable String configKey) {
        
        log.info("[TenantConfigController] getEffectiveConfiguration - tenantKey={}, configKey={}", tenantKey, configKey);
        
        Object value = tenantConfigCacheService.getCachedConfiguration(tenantKey, configKey);
        
        return ResponseEntity.ok(ApiResponse.success(value));
    }

    /**
     * 테넌트의 개별 설정 목록 조회
     */
    @GetMapping
    @Operation(summary = "테넌트 개별 설정 목록 조회", 
               description = "테넌트가 직접 설정한 개별 설정 목록을 조회합니다.")
    @AuditRequired(
        action = "getTenantConfigurations",
        resourceType = AuditResourceType.TENANT_CONFIG,
        severity = AuditSeverity.LOW,
        includeRequestData = false
    )
    public ResponseEntity<ApiResponse<List<TenantConfig>>> getTenantConfigurations(
            @Parameter(description = "테넌트 키") @PathVariable String tenantKey) {
        
        log.info("[TenantConfigController] getTenantConfigurations - tenantKey={}", tenantKey);
        
        List<TenantConfig> configs = tenantConfigService.getTenantConfigurations(tenantKey);
        
        return ResponseEntity.ok(ApiResponse.success(configs));
    }

    /**
     * 테넌트의 특정 개별 설정 조회
     */
    @GetMapping("/{configKey}")
    @Operation(summary = "테넌트 특정 개별 설정 조회", 
               description = "테넌트가 직접 설정한 특정 설정을 조회합니다.")
    @AuditRequired(
        action = "getTenantConfiguration",
        resourceType = AuditResourceType.TENANT_CONFIG,
        severity = AuditSeverity.LOW,
        includeRequestData = false
    )
    public ResponseEntity<ApiResponse<TenantConfig>> getTenantConfiguration(
            @Parameter(description = "테넌트 키") @PathVariable String tenantKey,
            @Parameter(description = "설정 키") @PathVariable String configKey) {
        
        log.info("[TenantConfigController] getTenantConfiguration - tenantKey={}, configKey={}", tenantKey, configKey);
        
        return tenantConfigService.getTenantConfiguration(tenantKey, configKey)
                .map(config -> ResponseEntity.ok(ApiResponse.success(config)))
                .orElse(ResponseEntity.ok(ApiResponse.success(null)));
    }

    /**
     * 테넌트 설정 생성/수정
     */
    @PostMapping
    @Operation(summary = "테넌트 설정 생성/수정", 
               description = "테넌트의 개별 설정을 생성하거나 수정합니다.")
    @AuditRequired(
        action = "saveTenantConfiguration",
        resourceType = AuditResourceType.TENANT_CONFIG,
        severity = AuditSeverity.MEDIUM,
        includeRequestData = true
    )
    public ResponseEntity<ApiResponse<TenantConfig>> saveTenantConfiguration(
            @Parameter(description = "테넌트 키") @PathVariable String tenantKey,
            @Valid @RequestBody TenantConfigRequest request) {
        
        log.info("[TenantConfigController] saveTenantConfiguration - tenantKey={}, configKey={}", 
                tenantKey, request.getConfigKey());
        
        TenantConfig config = tenantConfigService.saveTenantConfiguration(tenantKey, request);
        
        // 캐시 무효화
        tenantConfigCacheService.evictTenantConfigCache(tenantKey);
        
        return ResponseEntity.ok(ApiResponse.success(config));
    }

    /**
     * 테넌트 설정 삭제
     */
    @DeleteMapping("/{configKey}")
    @Operation(summary = "테넌트 설정 삭제", 
               description = "테넌트의 개별 설정을 삭제합니다.")
    @AuditRequired(
        action = "deleteTenantConfiguration",
        resourceType = AuditResourceType.TENANT_CONFIG,
        severity = AuditSeverity.MEDIUM,
        includeRequestData = true
    )
    public ResponseEntity<ApiResponse<Void>> deleteTenantConfiguration(
            @Parameter(description = "테넌트 키") @PathVariable String tenantKey,
            @Parameter(description = "설정 키") @PathVariable String configKey) {
        
        log.info("[TenantConfigController] deleteTenantConfiguration - tenantKey={}, configKey={}", 
                tenantKey, configKey);
        
        tenantConfigService.deleteTenantConfiguration(tenantKey, configKey);
        
        // 캐시 무효화
        tenantConfigCacheService.evictTenantConfigCache(tenantKey, configKey);
        
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 테넌트 설정 캐시 무효화
     */
    @PostMapping("/cache/evict")
    @Operation(summary = "테넌트 설정 캐시 무효화", 
               description = "테넌트의 설정 캐시를 강제로 무효화합니다.")
    @AuditRequired(
        action = "evictTenantConfigCache",
        resourceType = AuditResourceType.TENANT_CONFIG,
        severity = AuditSeverity.HIGH,
        includeRequestData = false
    )
    public ResponseEntity<ApiResponse<Void>> evictTenantConfigCache(
            @Parameter(description = "테넌트 키") @PathVariable String tenantKey) {
        
        log.info("[TenantConfigController] evictTenantConfigCache - tenantKey={}", tenantKey);
        
        tenantConfigCacheService.evictTenantConfigCache(tenantKey);
        
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
