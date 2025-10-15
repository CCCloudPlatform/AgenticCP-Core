package com.agenticcp.core.domain.monitoring.controller;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.common.enums.CommonErrorCode;
import com.agenticcp.core.domain.monitoring.entity.TenantDataRetentionPolicy;
import com.agenticcp.core.domain.monitoring.service.TenantDataRetentionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 테넌트별 데이터 보관 정책 관리 컨트롤러
 * 
 * 테넌트별로 메트릭 데이터의 보관 기간과 정책을 관리합니다.
 */
@Tag(name = "Metric Data Retention Policy", description = "테넌트별 메트릭 데이터 보관 정책 관리 API")
@RestController
@RequestMapping("/api/v1/monitoring/retention")
@RequiredArgsConstructor
@Slf4j
@Validated
public class DataRetentionController {

    private final TenantDataRetentionService retentionService;

    /**
     * 테넌트별 기본 보관 정책 설정 (30일)
     */
    @Operation(summary = "기본 보관 정책 설정", description = "30일 기본 보관 정책을 설정합니다")
    @PostMapping("/policies/default")
    public ResponseEntity<ApiResponse<TenantDataRetentionPolicy>> createDefaultRetentionPolicy() {
        try {
            String tenantId = TenantContextHolder.getCurrentTenantKeyOrThrow();
            log.info("테넌트별 기본 보관 정책 설정: tenantId={}", tenantId);
            
            TenantDataRetentionPolicy policy = retentionService.createDefaultRetentionPolicy(tenantId);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(policy, "기본 보관 정책이 설정되었습니다 (30일)."));
        } catch (IllegalStateException e) {
            log.warn("테넌트 컨텍스트가 없습니다: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(CommonErrorCode.TENANT_CONTEXT_NOT_SET));
        }
    }

    /**
     * 테넌트별 보관 정책 목록 조회
     */
    @Operation(summary = "보관 정책 목록 조회", description = "테넌트의 모든 보관 정책을 조회합니다")
    @GetMapping("/policies")
    public ResponseEntity<ApiResponse<List<TenantDataRetentionPolicy>>> getRetentionPolicies() {
        String tenantId = TenantContextHolder.getCurrentTenantKeyOrThrow();
        log.info("테넌트별 보관 정책 조회: tenantId={}", tenantId);
        
        List<TenantDataRetentionPolicy> policies = retentionService.getRetentionPolicies(tenantId);
        
        return ResponseEntity.ok(ApiResponse.success(policies));
    }

    /**
     * 테넌트별 활성화된 보관 정책 조회
     */
    @Operation(summary = "활성화된 보관 정책 조회", description = "테넌트의 활성화된 보관 정책을 조회합니다")
    @GetMapping("/policies/enabled")
    public ResponseEntity<ApiResponse<List<TenantDataRetentionPolicy>>> getEnabledRetentionPolicies() {
        String tenantId = TenantContextHolder.getCurrentTenantKeyOrThrow();
        log.info("테넌트별 활성화된 보관 정책 조회: tenantId={}", tenantId);
        
        List<TenantDataRetentionPolicy> policies = retentionService.getEnabledRetentionPolicies(tenantId);
        
        return ResponseEntity.ok(ApiResponse.success(policies));
    }

    /**
     * 테넌트별 특정 데이터 타입 보관 정책 조회
     */
    @Operation(summary = "특정 데이터 타입 보관 정책 조회", description = "특정 데이터 타입의 보관 정책을 조회합니다")
    @GetMapping("/policies/{dataType}")
    public ResponseEntity<ApiResponse<TenantDataRetentionPolicy>> getRetentionPolicy(
            @Parameter(description = "데이터 타입", required = true)
            @PathVariable @NotBlank String dataType) {
        String tenantId = TenantContextHolder.getCurrentTenantKeyOrThrow();
        log.info("테넌트별 보관 정책 조회: tenantId={}, dataType={}", tenantId, dataType);
        
        TenantDataRetentionPolicy policy = retentionService.getRetentionPolicy(tenantId, dataType);
        
        if (policy == null) {
            return ResponseEntity.ok(ApiResponse.success(null, "보관 정책이 설정되지 않았습니다."));
        }
        
        return ResponseEntity.ok(ApiResponse.success(policy));
    }

    /**
     * 테넌트별 보관 정책 업데이트
     */
    @Operation(summary = "보관 정책 업데이트", description = "특정 데이터 타입의 보관 정책을 업데이트합니다")
    @PutMapping("/policies/{dataType}")
    public ResponseEntity<ApiResponse<TenantDataRetentionPolicy>> updateRetentionPolicy(
            @Parameter(description = "데이터 타입", required = true)
            @PathVariable @NotBlank String dataType,
            @RequestBody @NotNull RetentionPolicyUpdateRequest request) {
        String tenantId = TenantContextHolder.getCurrentTenantKeyOrThrow();
        log.info("테넌트별 보관 정책 업데이트: tenantId={}, dataType={}, retentionDays={}", 
                tenantId, dataType, request.getRetentionDays());
        
        // 유효성 검증
        if (request.getRetentionDays() == null || request.getRetentionDays() <= 0) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(CommonErrorCode.BAD_REQUEST));
        }
        
        TenantDataRetentionPolicy policy = retentionService.updateRetentionPolicy(
                tenantId, dataType, request.getRetentionDays(), 
                request.getDeletionStrategy(), request.getDescription());
        
        return ResponseEntity.ok(ApiResponse.success(policy, "보관 정책이 업데이트되었습니다."));
    }

    /**
     * 테넌트별 보관 정책 활성화/비활성화
     */
    @Operation(summary = "보관 정책 활성화/비활성화", description = "특정 데이터 타입의 보관 정책을 활성화/비활성화합니다")
    @PatchMapping("/policies/{dataType}/toggle")
    public ResponseEntity<ApiResponse<String>> toggleRetentionPolicy(
            @Parameter(description = "데이터 타입", required = true)
            @PathVariable @NotBlank String dataType,
            @Parameter(description = "활성화 여부", required = true)
            @RequestParam boolean enabled) {
        String tenantId = TenantContextHolder.getCurrentTenantKeyOrThrow();
        log.info("테넌트별 보관 정책 토글: tenantId={}, dataType={}, enabled={}", tenantId, dataType, enabled);
        
        retentionService.toggleRetentionPolicy(tenantId, dataType, enabled);
        
        String message = enabled ? "보관 정책이 활성화되었습니다." : "보관 정책이 비활성화되었습니다.";
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    /**
     * 수동 데이터 정리 실행
     */
    @Operation(summary = "수동 데이터 정리", description = "특정 데이터 타입의 오래된 메트릭 데이터를 수동으로 정리합니다")
    @PostMapping("/cleanup/{dataType}")
    public ResponseEntity<ApiResponse<String>> manualCleanup(
            @Parameter(description = "데이터 타입", required = true)
            @PathVariable @NotBlank String dataType) {
        String tenantId = TenantContextHolder.getCurrentTenantKeyOrThrow();
        log.info("수동 데이터 정리 실행: tenantId={}, dataType={}", tenantId, dataType);
        
        int cleanedCount = retentionService.manualCleanupTenantData(tenantId, dataType);
        
        return ResponseEntity.ok(ApiResponse.success(
                String.format("%d개의 오래된 레코드가 정리되었습니다.", cleanedCount)));
    }

    /**
     * 보관 정책 통계 조회
     */
    @Operation(summary = "보관 정책 통계 조회", description = "보관 정책 관련 통계 정보를 조회합니다")
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<TenantDataRetentionService.RetentionPolicyStatistics>> getRetentionStatistics() {
        log.info("보관 정책 통계 조회");
        
        TenantDataRetentionService.RetentionPolicyStatistics statistics = 
                retentionService.getRetentionPolicyStatistics();
        
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }

    /**
     * 보관 정책 업데이트 요청 DTO
     */
    @lombok.Data
    public static class RetentionPolicyUpdateRequest {
        @NotNull(message = "보관 기간은 필수입니다")
        private Integer retentionDays;
        
        private TenantDataRetentionPolicy.DeletionStrategy deletionStrategy = 
                TenantDataRetentionPolicy.DeletionStrategy.DELETE;
        
        private String description;
    }
}
