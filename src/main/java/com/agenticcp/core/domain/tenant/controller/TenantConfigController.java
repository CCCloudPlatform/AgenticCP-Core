package com.agenticcp.core.domain.tenant.controller;

import com.agenticcp.core.common.audit.AuditController;
import com.agenticcp.core.common.audit.AuditRequired;
import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import com.agenticcp.core.common.util.LogMaskingUtils;
import com.agenticcp.core.domain.tenant.dto.EffectiveConfigResponse;
import com.agenticcp.core.domain.tenant.dto.TenantConfigRequest;
import com.agenticcp.core.domain.tenant.entity.TenantConfig;
import com.agenticcp.core.domain.tenant.service.TenantConfigCacheService;
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
 * 개별 테넌트 설정(TenantConfig)의 생성, 조회, 수정, 삭제 및
 * 계층적 상속을 통한 유효 설정 조회 기능을 제공합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-23
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
     * 테넌트의 유효 설정 전체 조회 (상속 포함)
     * 
     * 플랫폼, 테넌트 타입, 개별 테넌트 설정을 계층적으로 상속하여
     * 최종 유효 설정을 모두 조회합니다. 캐시를 활용하여 성능을 최적화합니다.
     * 
     * @param tenantKey 조회할 테넌트 키
     * @return 유효 설정 응답 (설정값과 출처 정보 포함)
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
        
        log.info("[TenantConfigController] getEffectiveConfigurations - tenantKey={}", 
                LogMaskingUtils.maskTenantKey(tenantKey));
        
        EffectiveConfigResponse response = tenantConfigCacheService.getCachedConfigurations(tenantKey);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 테넌트의 특정 유효 설정 단건 조회
     * 
     * 특정 설정 키에 대한 테넌트의 유효 설정값을 조회합니다.
     * 상속 계층(개별 → 타입 → 플랫폼)을 따라 우선순위가 높은 값을 반환합니다.
     * 
     * @param tenantKey 조회할 테넌트 키
     * @param configKey 조회할 설정 키
     * @return 유효 설정값 (파싱된 객체), 없으면 null
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
        
        log.info("[TenantConfigController] getEffectiveConfiguration - tenantKey={}, configKey={}", 
                LogMaskingUtils.maskTenantKey(tenantKey), configKey);
        
        Object value = tenantConfigCacheService.getCachedConfiguration(tenantKey, configKey);
        
        return ResponseEntity.ok(ApiResponse.success(value));
    }

    /**
     * 테넌트의 개별 설정 목록 조회
     * 
     * 테넌트가 직접 설정한 모든 개별 설정을 조회합니다.
     * 상속된 설정이 아닌 TenantConfig 레벨의 설정만 반환합니다.
     * 
     * @param tenantKey 조회할 테넌트 키
     * @return 테넌트의 개별 설정 목록
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
        
        log.info("[TenantConfigController] getTenantConfigurations - tenantKey={}", 
                LogMaskingUtils.maskTenantKey(tenantKey));
        
        List<TenantConfig> configs = tenantConfigService.getTenantConfigurations(tenantKey);
        
        return ResponseEntity.ok(ApiResponse.success(configs));
    }

    /**
     * 테넌트의 특정 개별 설정 단건 조회
     * 
     * 테넌트가 직접 설정한 특정 개별 설정을 조회합니다.
     * 상속된 값이 아닌 TenantConfig 레벨의 설정만 조회합니다.
     * 
     * @param tenantKey 조회할 테넌트 키
     * @param configKey 조회할 설정 키
     * @return 테넌트의 개별 설정 (없으면 null)
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
        
        log.info("[TenantConfigController] getTenantConfiguration - tenantKey={}, configKey={}", 
                LogMaskingUtils.maskTenantKey(tenantKey), configKey);
        
        return tenantConfigService.getTenantConfiguration(tenantKey, configKey)
                .map(config -> ResponseEntity.ok(ApiResponse.success(config)))
                .orElse(ResponseEntity.ok(ApiResponse.success(null)));
    }

    /**
     * 테넌트 설정 생성/수정 (Upsert)
     * 
     * 테넌트의 개별 설정을 생성하거나 수정합니다.
     * 같은 configKey가 이미 존재하면 수정, 없으면 신규 생성합니다.
     * 저장 후 자동으로 캐시를 무효화합니다.
     * 
     * @param tenantKey 설정을 저장할 테넌트 키
     * @param request 설정 요청 정보 (키, 값, 타입, 설명, 암호화 여부)
     * @return 저장된 테넌트 설정
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
                LogMaskingUtils.maskTenantKey(tenantKey), request.getConfigKey());
        
        TenantConfig config = tenantConfigService.saveTenantConfiguration(tenantKey, request);
        
        // 캐시 무효화: 해당 테넌트의 설정 캐시를 갱신
        tenantConfigCacheService.evictTenantConfigCache(tenantKey);
        
        return ResponseEntity.ok(ApiResponse.success(config));
    }

    /**
     * 테넌트 설정 삭제 (소프트 삭제)
     * 
     * 테넌트의 특정 개별 설정을 삭제합니다.
     * 물리적 삭제가 아닌 isDeleted 플래그를 설정하는 소프트 삭제입니다.
     * 삭제 후 자동으로 캐시를 무효화합니다.
     * 
     * @param tenantKey 삭제할 설정이 속한 테넌트 키
     * @param configKey 삭제할 설정 키
     * @return 성공 응답
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
                LogMaskingUtils.maskTenantKey(tenantKey), configKey);
        
        tenantConfigService.deleteTenantConfiguration(tenantKey, configKey);
        
        // 캐시 무효화: 해당 테넌트의 특정 설정 캐시를 제거
        tenantConfigCacheService.evictTenantConfigCache(tenantKey, configKey);
        
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 테넌트 설정 캐시 강제 무효화
     * 
     * 테넌트의 설정 캐시를 수동으로 무효화합니다.
     * 캐시 동기화 문제가 발생하거나 강제로 최신 상태로 갱신해야 할 때 사용합니다.
     * 주의: 이 API는 관리자 또는 디버깅 용도로만 사용해야 합니다.
     * 
     * @param tenantKey 캐시를 무효화할 테넌트 키
     * @return 성공 응답
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
        
        log.info("[TenantConfigController] evictTenantConfigCache - tenantKey={}", 
                LogMaskingUtils.maskTenantKey(tenantKey));
        
        tenantConfigCacheService.evictTenantConfigCache(tenantKey);
        
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
