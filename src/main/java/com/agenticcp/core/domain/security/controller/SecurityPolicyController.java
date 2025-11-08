package com.agenticcp.core.domain.security.controller;

import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.domain.security.entity.SecurityPolicy;
import com.agenticcp.core.domain.security.enums.SecurityErrorCode;
import com.agenticcp.core.domain.security.service.SecurityPolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 보안 정책 관리 REST API 컨트롤러
 *
 * <p>보안 정책의 조회, 생성, 수정 및 삭제 기능을 제공합니다.</p>
 *
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-08
 */
@RestController
@RequestMapping("/v1/security/policies")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Security Policy Management", description = "보안 정책 관리 API")
public class SecurityPolicyController {

    private final SecurityPolicyService securityPolicyService;

    /**
     * 모든 보안 정책을 조회합니다.
     */
    @GetMapping
    @Operation(summary = "모든 보안 정책 조회", description = "시스템에 등록된 모든 보안 정책 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<SecurityPolicy>>> getAllPolicies() {
        log.info("[SecurityPolicyController] getAllPolicies");
        List<SecurityPolicy> policies = securityPolicyService.getAllPolicies();
        return ResponseEntity.ok(ApiResponse.success(policies, "보안 정책 목록을 조회했습니다."));
    }

    /**
     * 활성화된 보안 정책을 조회합니다.
     */
    @GetMapping("/active")
    @Operation(summary = "활성 보안 정책 조회", description = "현재 활성화된 보안 정책 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<SecurityPolicy>>> getActivePolicies() {
        log.info("[SecurityPolicyController] getActivePolicies");
        List<SecurityPolicy> policies = securityPolicyService.getActivePolicies();
        return ResponseEntity.ok(ApiResponse.success(policies, "활성 보안 정책을 조회했습니다."));
    }

    /**
     * 정책 키로 보안 정책을 조회합니다.
     */
    @GetMapping("/{policyKey}")
    @Operation(summary = "특정 보안 정책 조회", description = "정책 키로 보안 정책을 조회합니다.")
    public ResponseEntity<ApiResponse<SecurityPolicy>> getPolicyByKey(@PathVariable String policyKey) {
        log.info("[SecurityPolicyController] getPolicyByKey - policyKey={}", policyKey);
        return securityPolicyService.getPolicyByKey(policyKey)
                .map(policy -> ResponseEntity.ok(ApiResponse.success(policy, "보안 정책을 조회했습니다.")))
                .orElseGet(() -> {
                    log.warn("[SecurityPolicyController] getPolicyByKey - not found, policyKey={}", policyKey);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(ApiResponse.error(SecurityErrorCode.POLICY_NOT_FOUND,
                                    "보안 정책을 찾을 수 없습니다: " + policyKey));
                });
    }

    /**
     * 정책 타입별로 보안 정책을 조회합니다.
     */
    @GetMapping("/type/{policyType}")
    @Operation(summary = "정책 타입별 조회", description = "정책 타입에 따라 보안 정책 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<SecurityPolicy>>> getPoliciesByType(
            @PathVariable SecurityPolicy.PolicyType policyType) {
        log.info("[SecurityPolicyController] getPoliciesByType - policyType={}", policyType);
        List<SecurityPolicy> policies = securityPolicyService.getPoliciesByType(policyType);
        return ResponseEntity.ok(ApiResponse.success(policies, "정책 타입별 정책을 조회했습니다."));
    }

    /**
     * 글로벌 보안 정책을 조회합니다.
     */
    @GetMapping("/global")
    @Operation(summary = "글로벌 보안 정책 조회", description = "모든 테넌트에 적용되는 글로벌 보안 정책을 조회합니다.")
    public ResponseEntity<ApiResponse<List<SecurityPolicy>>> getGlobalPolicies() {
        log.info("[SecurityPolicyController] getGlobalPolicies");
        List<SecurityPolicy> policies = securityPolicyService.getGlobalPolicies();
        return ResponseEntity.ok(ApiResponse.success(policies, "글로벌 보안 정책을 조회했습니다."));
    }

    /**
     * 시스템 보안 정책을 조회합니다.
     */
    @GetMapping("/system")
    @Operation(summary = "시스템 보안 정책 조회", description = "시스템에서 관리하는 보안 정책을 조회합니다.")
    public ResponseEntity<ApiResponse<List<SecurityPolicy>>> getSystemPolicies() {
        log.info("[SecurityPolicyController] getSystemPolicies");
        List<SecurityPolicy> policies = securityPolicyService.getSystemPolicies();
        return ResponseEntity.ok(ApiResponse.success(policies, "시스템 보안 정책을 조회했습니다."));
    }

    /**
     * 유효한 보안 정책을 조회합니다.
     */
    @GetMapping("/effective")
    @Operation(summary = "유효한 보안 정책 조회", description = "현재 유효한 보안 정책 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<SecurityPolicy>>> getEffectivePolicies() {
        log.info("[SecurityPolicyController] getEffectivePolicies");
        List<SecurityPolicy> policies = securityPolicyService.getEffectivePolicies();
        return ResponseEntity.ok(ApiResponse.success(policies, "유효한 보안 정책을 조회했습니다."));
    }

    /**
     * 정책 타입별로 우선순위 순 정렬된 보안 정책을 조회합니다.
     */
    @GetMapping("/type/{policyType}/ordered")
    @Operation(summary = "우선순위별 정책 조회", description = "정책 타입별로 우선순위 순으로 정렬된 보안 정책을 조회합니다.")
    public ResponseEntity<ApiResponse<List<SecurityPolicy>>> getPoliciesByTypeOrderedByPriority(
            @PathVariable SecurityPolicy.PolicyType policyType) {
        log.info("[SecurityPolicyController] getPoliciesByTypeOrderedByPriority - policyType={}", policyType);
        List<SecurityPolicy> policies = securityPolicyService.getPoliciesByTypeOrderedByPriority(policyType);
        return ResponseEntity.ok(ApiResponse.success(policies, "우선순위 순으로 정책을 조회했습니다."));
    }

    /**
     * 보안 정책을 생성합니다.
     */
    @PostMapping
    @Operation(summary = "보안 정책 생성", description = "새로운 보안 정책을 생성합니다.")
    public ResponseEntity<ApiResponse<SecurityPolicy>> createPolicy(@RequestBody SecurityPolicy securityPolicy) {
        String policyKeyLog = securityPolicy != null ? securityPolicy.getPolicyKey() : "null";
        log.info("[SecurityPolicyController] createPolicy - policyKey={}", policyKeyLog);
        SecurityPolicy createdPolicy = securityPolicyService.createPolicy(securityPolicy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdPolicy, "보안 정책이 생성되었습니다."));
    }

    /**
     * 보안 정책을 수정합니다.
     */
    @PutMapping("/{policyKey}")
    @Operation(summary = "보안 정책 수정", description = "보안 정책을 전체 수정합니다.")
    public ResponseEntity<ApiResponse<SecurityPolicy>> updatePolicy(
            @PathVariable String policyKey,
            @RequestBody SecurityPolicy securityPolicy) {
        log.info("[SecurityPolicyController] updatePolicy - policyKey={}, payloadKey={}", policyKey,
                securityPolicy != null ? securityPolicy.getPolicyKey() : "null");
        SecurityPolicy updatedPolicy = securityPolicyService.updatePolicy(policyKey, securityPolicy);
        return ResponseEntity.ok(ApiResponse.success(updatedPolicy, "보안 정책이 수정되었습니다."));
    }

    /**
     * 보안 정책의 활성화 상태를 토글합니다.
     */
    @PatchMapping("/{policyKey}/toggle")
    @Operation(summary = "보안 정책 토글", description = "보안 정책의 활성화 상태를 토글합니다.")
    public ResponseEntity<ApiResponse<SecurityPolicy>> togglePolicy(
            @PathVariable String policyKey,
            @RequestParam boolean enabled) {
        log.info("[SecurityPolicyController] togglePolicy - policyKey={}, enabled={}", policyKey, enabled);
        SecurityPolicy toggledPolicy = securityPolicyService.togglePolicy(policyKey, enabled);
        return ResponseEntity.ok(ApiResponse.success(toggledPolicy, "보안 정책이 토글되었습니다."));
    }

    /**
     * 보안 정책을 활성화합니다.
     */
    @PatchMapping("/{policyKey}/activate")
    @Operation(summary = "보안 정책 활성화", description = "비활성화된 보안 정책을 활성화합니다.")
    public ResponseEntity<ApiResponse<SecurityPolicy>> activatePolicy(@PathVariable String policyKey) {
        log.info("[SecurityPolicyController] activatePolicy - policyKey={}", policyKey);
        SecurityPolicy activatedPolicy = securityPolicyService.activatePolicy(policyKey);
        return ResponseEntity.ok(ApiResponse.success(activatedPolicy, "보안 정책이 활성화되었습니다."));
    }

    /**
     * 보안 정책을 비활성화합니다.
     */
    @PatchMapping("/{policyKey}/deactivate")
    @Operation(summary = "보안 정책 비활성화", description = "보안 정책을 비활성화합니다.")
    public ResponseEntity<ApiResponse<SecurityPolicy>> deactivatePolicy(@PathVariable String policyKey) {
        log.info("[SecurityPolicyController] deactivatePolicy - policyKey={}", policyKey);
        SecurityPolicy deactivatedPolicy = securityPolicyService.deactivatePolicy(policyKey);
        return ResponseEntity.ok(ApiResponse.success(deactivatedPolicy, "보안 정책이 비활성화되었습니다."));
    }

    /**
     * 보안 정책을 삭제합니다.
     */
    @DeleteMapping("/{policyKey}")
    @Operation(summary = "보안 정책 삭제", description = "보안 정책을 삭제합니다.")
    public ResponseEntity<ApiResponse<Void>> deletePolicy(@PathVariable String policyKey) {
        log.info("[SecurityPolicyController] deletePolicy - policyKey={}", policyKey);
        securityPolicyService.deletePolicy(policyKey);
        return ResponseEntity.ok(ApiResponse.success(null, "보안 정책이 삭제되었습니다."));
    }
}
