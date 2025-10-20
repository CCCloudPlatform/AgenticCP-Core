package com.agenticcp.core.domain.security.controller;

import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.domain.security.dto.PolicyConflictResolution;
import com.agenticcp.core.domain.security.entity.SecurityPolicy;
import com.agenticcp.core.domain.security.enums.ConflictResolutionStrategy;
import com.agenticcp.core.domain.security.enums.PolicyDecision;
import com.agenticcp.core.domain.security.enums.SecurityErrorCode;
import com.agenticcp.core.domain.security.repository.SecurityPolicyRepository;
import com.agenticcp.core.domain.security.service.PolicyPriorityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 정책 우선순위 및 충돌 해결 테스트용 컨트롤러
 * Feature 2 기능을 수동으로 테스트하기 위한 API 제공
 */
@RestController
@RequestMapping("/security/priority")
@RequiredArgsConstructor
@Tag(name = "Policy Priority Management", description = "정책 우선순위 및 충돌 해결 테스트 API")
public class PolicyPriorityController {

    private final PolicyPriorityService policyPriorityService;
    private final SecurityPolicyRepository policyRepository;

    /**
     * 우선순위 검증
     */
    @GetMapping("/validate/{priority}")
    @Operation(summary = "우선순위 검증", description = "입력된 우선순위가 유효한지 검증합니다 (1-1000 범위)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validatePriority(
            @PathVariable Integer priority) {
        
        boolean isValid = policyPriorityService.isValidPriority(priority);
        
        Map<String, Object> result = new HashMap<>();
        result.put("priority", priority);
        result.put("isValid", isValid);
        result.put("validRange", "1 ~ 1000");
        result.put("message", isValid ? "유효한 우선순위입니다" : "유효하지 않은 우선순위입니다");
        
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 자동 우선순위 할당
     */
    @GetMapping("/auto-assign")
    @Operation(summary = "자동 우선순위 할당", description = "기존 정책들을 분석하여 적절한 우선순위를 자동으로 할당합니다")
    public ResponseEntity<ApiResponse<Map<String, Object>>> assignAutoPriority(
            @RequestParam(required = false) String tenantId) {
        
        Integer assignedPriority = policyPriorityService.assignAutoPriority(tenantId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("tenantId", tenantId != null ? tenantId : "GLOBAL");
        result.put("assignedPriority", assignedPriority);
        result.put("message", String.format("우선순위 %d가 자동 할당되었습니다", assignedPriority));
        
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 다음 사용 가능한 우선순위 찾기
     */
    @GetMapping("/next-available")
    @Operation(summary = "다음 사용 가능한 우선순위 찾기", description = "현재 우선순위를 기준으로 다음 사용 가능한 우선순위를 찾습니다")
    public ResponseEntity<ApiResponse<Map<String, Object>>> findNextAvailablePriority(
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) Integer currentPriority) {
        
        Integer nextPriority = policyPriorityService.findNextAvailablePriority(tenantId, currentPriority);
        
        Map<String, Object> result = new HashMap<>();
        result.put("tenantId", tenantId != null ? tenantId : "GLOBAL");
        result.put("currentPriority", currentPriority);
        result.put("nextAvailablePriority", nextPriority);
        result.put("message", String.format("다음 사용 가능한 우선순위는 %d입니다", nextPriority));
        
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 정책 우선순위별 정렬 조회
     */
    @GetMapping("/sorted")
    @Operation(summary = "우선순위별 정렬 조회", description = "활성화된 정책을 우선순위 순으로 정렬하여 조회합니다")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSortedPolicies(
            @RequestParam(required = false) String tenantId,
            @RequestParam(defaultValue = "false") boolean ascending) {
        
        List<SecurityPolicy> policies;
        if (tenantId != null) {
            policies = policyRepository.findByTenantIdAndIsEnabledTrue(tenantId);
        } else {
            policies = policyRepository.findByIsGlobalTrueAndIsEnabledTrue();
        }
        
        List<SecurityPolicy> sortedPolicies = policyPriorityService.sortByPriority(policies, ascending);
        
        // 간단한 응답용 DTO
        List<Map<String, Object>> simplePolicies = sortedPolicies.stream()
            .map(p -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", p.getId());
                map.put("policyKey", p.getPolicyKey());
                map.put("policyName", p.getPolicyName());
                map.put("priority", p.getPriority());
                return map;
            })
            .collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("tenantId", tenantId != null ? tenantId : "GLOBAL");
        result.put("ascending", ascending);
        result.put("totalCount", simplePolicies.size());
        result.put("policies", simplePolicies);
        
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 정책 충돌 해결 시뮬레이션
     */
    @PostMapping("/resolve-conflict")
    @Operation(summary = "정책 충돌 해결 시뮬레이션", description = "여러 정책 간 충돌을 특정 전략으로 해결합니다")
    public ResponseEntity<ApiResponse<PolicyConflictResolution>> resolveConflict(
            @RequestBody ConflictSimulationRequest request) {
        
        // 정책 ID 목록으로 정책 조회
        List<SecurityPolicy> policies = request.getPolicyIds().stream()
            .map(id -> policyRepository.findById(id).orElse(null))
            .filter(p -> p != null)
            .collect(Collectors.toList());
        
        if (policies.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(SecurityErrorCode.POLICY_NOT_FOUND, "유효한 정책을 찾을 수 없습니다"));
        }
        
        // 정책별 결정 맵 생성
        Map<String, PolicyDecision> policyDecisions = new HashMap<>();
        for (int i = 0; i < policies.size() && i < request.getDecisions().size(); i++) {
            policyDecisions.put(
                policies.get(i).getId().toString(), 
                request.getDecisions().get(i)
            );
        }
        
        // 충돌 해결
        PolicyConflictResolution resolution = policyPriorityService.resolveConflict(
            policies,
            policyDecisions,
            request.getStrategy(),
            request.getResourceId(),
            request.getResourceType(),
            request.getAction()
        );
        
        return ResponseEntity.ok(ApiResponse.success(resolution));
    }

    /**
     * 사용 가능한 충돌 해결 전략 목록
     */
    @GetMapping("/strategies")
    @Operation(summary = "충돌 해결 전략 목록", description = "사용 가능한 모든 충돌 해결 전략을 조회합니다")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getStrategies() {
        List<Map<String, String>> strategies = java.util.Arrays.stream(ConflictResolutionStrategy.values())
            .map(s -> {
                Map<String, String> map = new HashMap<>();
                map.put("name", s.name());
                map.put("displayName", s.getDisplayName());
                map.put("description", s.getDescription());
                return map;
            })
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(strategies));
    }

    /**
     * 충돌 시뮬레이션 요청 DTO
     */
    @Data
    @Builder
    @AllArgsConstructor
    public static class ConflictSimulationRequest {
        private List<Long> policyIds;
        private List<PolicyDecision> decisions;
        private ConflictResolutionStrategy strategy;
        private String resourceId;
        private String resourceType;
        private String action;
    }
}

