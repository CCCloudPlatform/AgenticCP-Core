package com.agenticcp.core.domain.security.controller;

import com.agenticcp.core.domain.security.dto.PolicyEvaluationRequest;
import com.agenticcp.core.domain.security.dto.PolicyEvaluationResult;
import com.agenticcp.core.domain.security.service.PolicyEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * 정책 엔진 REST API 컨트롤러
 * 
 * <p>정책 평가 및 관리를 위한 REST API 엔드포인트를 제공합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@RestController
@RequestMapping("/api/v1/security/policy")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@Tag(name = "Policy Engine", description = "정책 엔진 및 규칙 평가 API")
public class PolicyEngineController {

    private final PolicyEngineService policyEngineService;

    /**
     * 정책 평가 API
     * 
     * @param request 정책 평가 요청
     * @return 정책 평가 결과
     */
    @PostMapping("/evaluate")
    @Operation(summary = "정책 평가", description = "정책 평가 요청을 처리하고 결과를 반환합니다.")
    public ResponseEntity<PolicyEvaluationResult> evaluatePolicy(
            @Valid @RequestBody PolicyEvaluationRequest request) {
        
        log.info("정책 평가 요청: {}", request);
        
        try {
            PolicyEvaluationResult result = policyEngineService.evaluatePolicy(request);
            log.info("정책 평가 결과: {}", result);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("정책 평가 중 오류 발생", e);
            PolicyEvaluationResult errorResult = PolicyEvaluationResult.inconclusive("정책 평가 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.ok(errorResult);
        }
    }

    /**
     * 정책 캐시 무효화 API
     * 
     * @param resourceType 리소스 타입 (선택사항)
     * @param action 액션 (선택사항)
     * @return 무효화 결과
     */
    @DeleteMapping("/cache")
    @Operation(summary = "캐시 무효화", description = "정책 평가 결과 캐시를 무효화합니다.")
    public ResponseEntity<Map<String, Object>> evictPolicyCache(
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String action) {
        
        log.info("정책 캐시 무효화 요청: resourceType={}, action={}", resourceType, action);
        
        try {
            if (resourceType != null && !resourceType.isEmpty() && 
                action != null && !action.isEmpty()) {
                policyEngineService.evictPolicyCache(resourceType, action);
            } else {
                policyEngineService.evictAllPolicyCache();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "캐시가 성공적으로 무효화되었습니다");
            response.put("resourceType", resourceType);
            response.put("action", action);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("캐시 무효화 중 오류 발생", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "캐시 무효화 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 정책 엔진 상태 확인 API
     * 
     * @return 정책 엔진 상태 정보
     */
    @GetMapping("/health")
    @Operation(summary = "헬스 체크", description = "정책 엔진 서비스 상태를 확인합니다.")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "PolicyEngine");
        response.put("version", "1.0.0");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 간단한 정책 평가 테스트 API
     * 
     * @return 테스트 결과
     */
    @GetMapping("/test")
    @Operation(summary = "정책 엔진 테스트", description = "정책 엔진 기본 기능을 테스트합니다.")
    public ResponseEntity<Map<String, Object>> testPolicyEngine() {
        log.info("정책 엔진 테스트 요청");
        
        try {
            // 테스트용 요청 생성
            PolicyEvaluationRequest testRequest = PolicyEvaluationRequest.builder()
                    .resourceType("TEST_RESOURCE")
                    .resourceId("test-001")
                    .action("READ")
                    .userId("test-user")
                    .tenantKey("test-tenant")
                    .clientIp("127.0.0.1")
                    .build();
            
            PolicyEvaluationResult result = policyEngineService.evaluatePolicy(testRequest);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "정책 엔진이 정상적으로 동작합니다");
            response.put("testResult", result);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("정책 엔진 테스트 중 오류 발생", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "정책 엔진 테스트 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.ok(response);
        }
    }
}