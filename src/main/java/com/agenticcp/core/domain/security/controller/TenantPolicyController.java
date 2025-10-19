package com.agenticcp.core.domain.security.controller;

import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.domain.security.dto.EffectivePolicySetDTO;
import com.agenticcp.core.domain.security.entity.SecurityPolicy;
import com.agenticcp.core.domain.security.service.TenantPolicyService;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.tenant.repository.TenantRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 테넌트 정책 관리 컨트롤러
 * 
 * <p>테넌트별 정책 조회, 초기화, 캐시 관리 API를 제공합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/api/security/tenant-policies")
@RequiredArgsConstructor
@Tag(name = "Tenant Policy Management", description = "테넌트별 정책 관리 API")
public class TenantPolicyController {
    
    private final TenantPolicyService tenantPolicyService;
    private final TenantRepository tenantRepository;
    
    /**
     * 특정 테넌트 정책 조회 (테넌트 전용)
     */
    @GetMapping("/{tenantId}")
    @Operation(summary = "테넌트 정책 조회", description = "특정 테넌트의 정책만 조회 (글로벌 정책 제외)")
    public ResponseEntity<ApiResponse<List<EffectivePolicySetDTO.PolicySummaryDTO>>> getTenantPolicies(
            @PathVariable Long tenantId) {
        
        log.info("[TenantPolicyController] getTenantPolicies - tenantId={}", tenantId);
        
        EffectivePolicySetDTO effectivePolicies = tenantPolicyService.getEffectivePolicies(tenantId);
        List<EffectivePolicySetDTO.PolicySummaryDTO> tenantPolicies = effectivePolicies.getTenantPolicies();
        
        return ResponseEntity.ok(ApiResponse.success(tenantPolicies, 
                String.format("테넌트 정책 조회 성공 (총 %d개)", tenantPolicies.size())));
    }
    
    /**
     * 유효한 정책 조회 (글로벌 + 테넌트)
     */
    @GetMapping("/{tenantId}/effective")
    @Operation(summary = "유효한 정책 조회", description = "테넌트에 적용되는 모든 유효한 정책 조회 (글로벌 + 테넌트)")
    public ResponseEntity<ApiResponse<EffectivePolicySetDTO>> getEffectivePolicies(
            @PathVariable Long tenantId) {
        
        log.info("[TenantPolicyController] getEffectivePolicies - tenantId={}", tenantId);
        
        EffectivePolicySetDTO result = tenantPolicyService.getEffectivePolicies(tenantId);
        
        return ResponseEntity.ok(ApiResponse.success(result, 
                String.format("유효한 정책 조회 성공 (총 %d개: 글로벌 %d개, 테넌트 %d개)", 
                        result.getTotalPolicyCount(), 
                        result.getGlobalPolicyCount(), 
                        result.getTenantPolicyCount())));
    }
    
    /**
     * 정책 타입별 조회
     */
    @GetMapping("/{tenantId}/by-type/{policyType}")
    @Operation(summary = "정책 타입별 조회", description = "특정 정책 타입에 해당하는 정책만 조회")
    public ResponseEntity<ApiResponse<List<SecurityPolicy>>> getPoliciesByType(
            @PathVariable Long tenantId,
            @PathVariable SecurityPolicy.PolicyType policyType) {
        
        log.info("[TenantPolicyController] getPoliciesByType - tenantId={}, policyType={}", tenantId, policyType);
        
        List<SecurityPolicy> policies = tenantPolicyService.getPoliciesByType(tenantId, policyType);
        
        return ResponseEntity.ok(ApiResponse.success(policies, 
                String.format("%s 타입 정책 조회 성공 (총 %d개)", policyType, policies.size())));
    }
    
    /**
     * 정렬된 유효한 정책 조회
     */
    @GetMapping("/{tenantId}/sorted")
    @Operation(summary = "정렬된 정책 조회", description = "우선순위별로 정렬된 정책 조회")
    public ResponseEntity<ApiResponse<List<SecurityPolicy>>> getSortedEffectivePolicies(
            @PathVariable Long tenantId,
            @RequestParam(defaultValue = "false") boolean ascending) {
        
        log.info("[TenantPolicyController] getSortedEffectivePolicies - tenantId={}, ascending={}", tenantId, ascending);
        
        List<SecurityPolicy> policies = tenantPolicyService.getSortedEffectivePolicies(tenantId, ascending);
        
        return ResponseEntity.ok(ApiResponse.success(policies, 
                String.format("정렬된 정책 조회 성공 (총 %d개, %s)", 
                        policies.size(), 
                        ascending ? "우선순위 오름차순" : "우선순위 내림차순")));
    }
    
    /**
     * 기본 정책 초기화
     */
    @PostMapping("/{tenantId}/initialize")
    @Operation(summary = "기본 정책 초기화", description = "테넌트 생성 시 기본 보안 정책 자동 생성")
    public ResponseEntity<ApiResponse<List<SecurityPolicy>>> initializeDefaultPolicies(
            @PathVariable Long tenantId) {
        
        log.info("[TenantPolicyController] initializeDefaultPolicies - tenantId={}", tenantId);
        
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("테넌트를 찾을 수 없습니다: " + tenantId));
        
        List<SecurityPolicy> policies = tenantPolicyService.initializeDefaultPolicies(tenant);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(policies, 
                        String.format("기본 정책 초기화 성공 (총 %d개 정책 생성)", policies.size())));
    }
    
    /**
     * 테넌트 정책 캐시 무효화
     */
    @DeleteMapping("/{tenantId}/cache")
    @Operation(summary = "캐시 무효화", description = "특정 테넌트의 정책 캐시 삭제")
    public ResponseEntity<ApiResponse<Void>> evictTenantPolicyCache(
            @PathVariable Long tenantId) {
        
        log.info("[TenantPolicyController] evictTenantPolicyCache - tenantId={}", tenantId);
        
        tenantPolicyService.evictTenantPolicyCache(tenantId);
        
        return ResponseEntity.ok(ApiResponse.success(null, "캐시 무효화 성공"));
    }
    
    /**
     * 전체 캐시 무효화
     */
    @DeleteMapping("/cache/all")
    @Operation(summary = "전체 캐시 무효화", description = "모든 테넌트 정책 캐시 삭제")
    public ResponseEntity<ApiResponse<Void>> evictAllTenantPolicyCache() {
        
        log.info("[TenantPolicyController] evictAllTenantPolicyCache");
        
        tenantPolicyService.evictAllTenantPolicyCache();
        
        return ResponseEntity.ok(ApiResponse.success(null, "전체 캐시 무효화 성공"));
    }
    
    /**
     * 정책 통계 조회
     */
    @GetMapping("/{tenantId}/statistics")
    @Operation(summary = "정책 통계 조회", description = "테넌트의 정책 통계 정보 조회")
    public ResponseEntity<ApiResponse<TenantPolicyService.PolicyStatisticsDTO>> getPolicyStatistics(
            @PathVariable Long tenantId) {
        
        log.info("[TenantPolicyController] getPolicyStatistics - tenantId={}", tenantId);
        
        TenantPolicyService.PolicyStatisticsDTO statistics = tenantPolicyService.getPolicyStatistics(tenantId);
        
        return ResponseEntity.ok(ApiResponse.success(statistics, "정책 통계 조회 성공"));
    }
}

