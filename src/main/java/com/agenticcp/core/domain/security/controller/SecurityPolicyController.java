package com.agenticcp.core.domain.security.controller;

import com.agenticcp.core.common.dto.ApiResponse;
import com.agenticcp.core.domain.security.entity.SecurityPolicy;
import com.agenticcp.core.domain.security.service.SecurityPolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/security/policies")
@RequiredArgsConstructor
@Tag(name = "Security Policy Management", description = "보안 정책 관리 API")
public class SecurityPolicyController {

    private final SecurityPolicyService securityPolicyService;

    @GetMapping
    @Operation(summary = "모든 보안 정책 조회")
    public ResponseEntity<ApiResponse<List<SecurityPolicy>>> getAllPolicies() {
        List<SecurityPolicy> policies = securityPolicyService.getAllPolicies();
        return ResponseEntity.ok(ApiResponse.success(policies));
    }

    @GetMapping("/active")
    @Operation(summary = "활성 보안 정책 조회")
    public ResponseEntity<ApiResponse<List<SecurityPolicy>>> getActivePolicies() {
        List<SecurityPolicy> policies = securityPolicyService.getActivePolicies();
        return ResponseEntity.ok(ApiResponse.success(policies));
    }

    @GetMapping("/{policyKey}")
    @Operation(summary = "특정 보안 정책 조회")
    public ResponseEntity<ApiResponse<SecurityPolicy>> getPolicyByKey(@PathVariable String policyKey) {
        return securityPolicyService.getPolicyByKey(policyKey)
                .map(policy -> ResponseEntity.ok(ApiResponse.success(policy)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/type/{policyType}")
    @Operation(summary = "정책 타입별 조회")
    public ResponseEntity<ApiResponse<List<SecurityPolicy>>> getPoliciesByType(
            @PathVariable SecurityPolicy.PolicyType policyType) {
        List<SecurityPolicy> policies = securityPolicyService.getPoliciesByType(policyType);
        return ResponseEntity.ok(ApiResponse.success(policies));
    }

    @GetMapping("/global")
    @Operation(summary = "글로벌 보안 정책 조회")
    public ResponseEntity<ApiResponse<List<SecurityPolicy>>> getGlobalPolicies() {
        List<SecurityPolicy> policies = securityPolicyService.getGlobalPolicies();
        return ResponseEntity.ok(ApiResponse.success(policies));
    }

    @GetMapping("/system")
    @Operation(summary = "시스템 보안 정책 조회")
    public ResponseEntity<ApiResponse<List<SecurityPolicy>>> getSystemPolicies() {
        List<SecurityPolicy> policies = securityPolicyService.getSystemPolicies();
        return ResponseEntity.ok(ApiResponse.success(policies));
    }

    @GetMapping("/effective")
    @Operation(summary = "유효한 보안 정책 조회")
    public ResponseEntity<ApiResponse<List<SecurityPolicy>>> getEffectivePolicies() {
        List<SecurityPolicy> policies = securityPolicyService.getEffectivePolicies();
        return ResponseEntity.ok(ApiResponse.success(policies));
    }

    @GetMapping("/type/{policyType}/ordered")
    @Operation(summary = "우선순위별 정책 조회")
    public ResponseEntity<ApiResponse<List<SecurityPolicy>>> getPoliciesByTypeOrderedByPriority(
            @PathVariable SecurityPolicy.PolicyType policyType) {
        List<SecurityPolicy> policies = securityPolicyService.getPoliciesByTypeOrderedByPriority(policyType);
        return ResponseEntity.ok(ApiResponse.success(policies));
    }

    @PostMapping
    @Operation(summary = "보안 정책 생성")
    public ResponseEntity<ApiResponse<SecurityPolicy>> createPolicy(@RequestBody SecurityPolicy securityPolicy) {
        SecurityPolicy createdPolicy = securityPolicyService.createPolicy(securityPolicy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdPolicy, "보안 정책이 생성되었습니다."));
    }

    @PutMapping("/{policyKey}")
    @Operation(summary = "보안 정책 수정")
    public ResponseEntity<ApiResponse<SecurityPolicy>> updatePolicy(
            @PathVariable String policyKey, 
            @RequestBody SecurityPolicy securityPolicy) {
        SecurityPolicy updatedPolicy = securityPolicyService.updatePolicy(policyKey, securityPolicy);
        return ResponseEntity.ok(ApiResponse.success(updatedPolicy, "보안 정책이 수정되었습니다."));
    }

    @PatchMapping("/{policyKey}/toggle")
    @Operation(summary = "보안 정책 토글")
    public ResponseEntity<ApiResponse<SecurityPolicy>> togglePolicy(
            @PathVariable String policyKey, 
            @RequestParam boolean enabled) {
        SecurityPolicy toggledPolicy = securityPolicyService.togglePolicy(policyKey, enabled);
        return ResponseEntity.ok(ApiResponse.success(toggledPolicy, "보안 정책이 토글되었습니다."));
    }

    @PatchMapping("/{policyKey}/activate")
    @Operation(summary = "보안 정책 활성화")
    public ResponseEntity<ApiResponse<SecurityPolicy>> activatePolicy(@PathVariable String policyKey) {
        SecurityPolicy activatedPolicy = securityPolicyService.activatePolicy(policyKey);
        return ResponseEntity.ok(ApiResponse.success(activatedPolicy, "보안 정책이 활성화되었습니다."));
    }

    @PatchMapping("/{policyKey}/deactivate")
    @Operation(summary = "보안 정책 비활성화")
    public ResponseEntity<ApiResponse<SecurityPolicy>> deactivatePolicy(@PathVariable String policyKey) {
        SecurityPolicy deactivatedPolicy = securityPolicyService.deactivatePolicy(policyKey);
        return ResponseEntity.ok(ApiResponse.success(deactivatedPolicy, "보안 정책이 비활성화되었습니다."));
    }

    @DeleteMapping("/{policyKey}")
    @Operation(summary = "보안 정책 삭제")
    public ResponseEntity<ApiResponse<Void>> deletePolicy(@PathVariable String policyKey) {
        securityPolicyService.deletePolicy(policyKey);
        return ResponseEntity.ok(ApiResponse.success(null, "보안 정책이 삭제되었습니다."));
    }
}
