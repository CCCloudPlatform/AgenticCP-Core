package com.agenticcp.core.domain.security.service;

import com.agenticcp.core.domain.security.dto.PolicyViolationEvent;
import com.agenticcp.core.domain.security.entity.PolicyViolation;
import com.agenticcp.core.domain.security.entity.SecurityPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 테넌트 인식 정책 평가 엔진
 * 
 * <p>테넌트 컨텍스트 기반으로 정책을 평가하고, 글로벌 정책과 테넌트 정책을 우선순위에 따라 적용합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantAwarePolicyEngine {
    
    private final TenantPolicyService tenantPolicyService;
    private final ApplicationEventPublisher eventPublisher;
    
    /**
     * 테넌트 컨텍스트 기반 정책 평가
     * 
     * @param tenantId 테넌트 ID
     * @param resourceType 리소스 타입
     * @param action 액션
     * @param context 평가 컨텍스트
     * @return 평가 결과
     */
    public PolicyEvaluationResult evaluateWithTenantContext(
            Long tenantId, 
            String resourceType, 
            String action, 
            java.util.Map<String, Object> context) {
        
        log.info("[TenantAwarePolicyEngine] evaluateWithTenantContext - tenantId={}, resourceType={}, action={}", 
                tenantId, resourceType, action);
        
        // 1. 테넌트별 유효한 정책 조회 (글로벌 + 테넌트, 우선순위 정렬)
        List<SecurityPolicy> effectivePolicies = tenantPolicyService.getSortedEffectivePolicies(tenantId, false);
        
        if (effectivePolicies.isEmpty()) {
            log.warn("[TenantAwarePolicyEngine] No effective policies found for tenantId={}", tenantId);
            return PolicyEvaluationResult.builder()
                    .decision(PolicyDecision.DENY)
                    .reason("정책이 없습니다. 기본적으로 거부합니다.")
                    .appliedPolicyCount(0)
                    .evaluatedAt(LocalDateTime.now())
                    .build();
        }
        
        // 2. 정책 필터링 (리소스 타입, 액션 매칭)
        List<SecurityPolicy> applicablePolicies = filterApplicablePolicies(effectivePolicies, resourceType, action);
        
        if (applicablePolicies.isEmpty()) {
            log.warn("[TenantAwarePolicyEngine] No applicable policies for resourceType={}, action={}", resourceType, action);
            return PolicyEvaluationResult.builder()
                    .decision(PolicyDecision.DENY)
                    .reason("적용 가능한 정책이 없습니다. 기본적으로 거부합니다.")
                    .appliedPolicyCount(0)
                    .evaluatedAt(LocalDateTime.now())
                    .build();
        }
        
        // 3. 정책 평가 (우선순위 순으로)
        PolicyEvaluationResult result = evaluatePolicies(applicablePolicies, context);
        
        // 4. 위반 감지 시 이벤트 발행 (Feature 4 통합)
        if (result.getDecision() == PolicyDecision.DENY) {
            publishViolationEvent(tenantId, applicablePolicies, resourceType, action, context, result);
        }
        
        log.info("[TenantAwarePolicyEngine] evaluateWithTenantContext - success decision={}, appliedCount={}", 
                result.getDecision(), result.getAppliedPolicyCount());
        
        return result;
    }
    
    /**
     * 테넌트별 정책 타입 평가
     * 
     * @param tenantId 테넌트 ID
     * @param policyType 정책 타입
     * @param context 평가 컨텍스트
     * @return 평가 결과
     */
    public PolicyEvaluationResult evaluateByPolicyType(
            Long tenantId, 
            SecurityPolicy.PolicyType policyType, 
            java.util.Map<String, Object> context) {
        
        log.info("[TenantAwarePolicyEngine] evaluateByPolicyType - tenantId={}, policyType={}", tenantId, policyType);
        
        // 1. 정책 타입별 조회
        List<SecurityPolicy> policies = tenantPolicyService.getPoliciesByType(tenantId, policyType);
        
        if (policies.isEmpty()) {
            log.warn("[TenantAwarePolicyEngine] No policies found for policyType={}", policyType);
            return PolicyEvaluationResult.builder()
                    .decision(PolicyDecision.DENY)
                    .reason("정책 타입에 해당하는 정책이 없습니다.")
                    .appliedPolicyCount(0)
                    .evaluatedAt(LocalDateTime.now())
                    .build();
        }
        
        // 2. 정책 평가
        PolicyEvaluationResult result = evaluatePolicies(policies, context);
        
        log.info("[TenantAwarePolicyEngine] evaluateByPolicyType - success decision={}", result.getDecision());
        
        return result;
    }
    
    /**
     * 테넌트 우선순위 적용 정책 평가
     * - 글로벌 정책보다 테넌트 정책이 더 높은 우선순위를 가질 수 있음
     * 
     * @param tenantId 테넌트 ID
     * @param resourceType 리소스 타입
     * @param action 액션
     * @param context 평가 컨텍스트
     * @return 평가 결과
     */
    public PolicyEvaluationResult evaluateWithPriorityOverride(
            Long tenantId,
            String resourceType,
            String action,
            java.util.Map<String, Object> context) {
        
        log.info("[TenantAwarePolicyEngine] evaluateWithPriorityOverride - tenantId={}", tenantId);
        
        // 1. 테넌트별 정책 조회 (우선순위 내림차순)
        List<SecurityPolicy> sortedPolicies = tenantPolicyService.getSortedEffectivePolicies(tenantId, false);
        
        // 2. 리소스/액션 필터링
        List<SecurityPolicy> applicablePolicies = filterApplicablePolicies(sortedPolicies, resourceType, action);
        
        if (applicablePolicies.isEmpty()) {
            return PolicyEvaluationResult.builder()
                    .decision(PolicyDecision.DENY)
                    .reason("적용 가능한 정책이 없습니다.")
                    .appliedPolicyCount(0)
                    .evaluatedAt(LocalDateTime.now())
                    .build();
        }
        
        // 3. 최고 우선순위 정책만 적용 (FIRST_MATCH 전략)
        SecurityPolicy highestPriorityPolicy = applicablePolicies.get(0);
        
        PolicyEvaluationResult result = PolicyEvaluationResult.builder()
                .decision(evaluateSinglePolicy(highestPriorityPolicy, context))
                .reason(String.format("최고 우선순위 정책 적용: %s (우선순위: %d)", 
                        highestPriorityPolicy.getPolicyName(), 
                        highestPriorityPolicy.getPriority()))
                .appliedPolicyCount(1)
                .appliedPolicyIds(List.of(highestPriorityPolicy.getId()))
                .evaluatedAt(LocalDateTime.now())
                .build();
        
        log.info("[TenantAwarePolicyEngine] evaluateWithPriorityOverride - success decision={}", result.getDecision());
        
        return result;
    }
    
    // ==================== Private 메서드 ====================
    
    /**
     * 적용 가능한 정책 필터링
     */
    private List<SecurityPolicy> filterApplicablePolicies(
            List<SecurityPolicy> policies, 
            String resourceType, 
            String action) {
        
        return policies.stream()
                .filter(policy -> isApplicable(policy, resourceType, action))
                .toList();
    }
    
    /**
     * 정책 적용 가능 여부 판단
     */
    private boolean isApplicable(SecurityPolicy policy, String resourceType, String action) {
        // 1. 활성화 여부
        if (!policy.getIsEnabled()) {
            return false;
        }
        
        // 2. 유효 기간 체크
        LocalDateTime now = LocalDateTime.now();
        if (policy.getEffectiveFrom() != null && now.isBefore(policy.getEffectiveFrom())) {
            return false;
        }
        if (policy.getEffectiveUntil() != null && now.isAfter(policy.getEffectiveUntil())) {
            return false;
        }
        
        // 3. 리소스 타입 매칭 (targetResources에 포함되어 있는지 확인)
        // 간단한 구현: targetResources가 null이면 모든 리소스에 적용
        if (policy.getTargetResources() != null && !policy.getTargetResources().isEmpty()) {
            if (!policy.getTargetResources().contains(resourceType)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 여러 정책 평가 (우선순위 순)
     */
    private PolicyEvaluationResult evaluatePolicies(
            List<SecurityPolicy> policies, 
            java.util.Map<String, Object> context) {
        
        PolicyDecision finalDecision = PolicyDecision.DENY;  // 기본: DENY
        List<Long> appliedPolicyIds = new ArrayList<>();
        StringBuilder reasonBuilder = new StringBuilder();
        
        for (SecurityPolicy policy : policies) {
            PolicyDecision decision = evaluateSinglePolicy(policy, context);
            appliedPolicyIds.add(policy.getId());
            
            reasonBuilder.append(String.format("[%s: %s] ", policy.getPolicyName(), decision));
            
            // 첫 ALLOW가 나오면 허용 (ALLOW_OVERRIDES 전략)
            if (decision == PolicyDecision.ALLOW) {
                finalDecision = PolicyDecision.ALLOW;
                reasonBuilder.append(" -> 정책 허용");
                break;
            }
        }
        
        if (finalDecision == PolicyDecision.DENY) {
            reasonBuilder.append(" -> 모든 정책 거부 또는 기본 거부");
        }
        
        return PolicyEvaluationResult.builder()
                .decision(finalDecision)
                .reason(reasonBuilder.toString())
                .appliedPolicyCount(appliedPolicyIds.size())
                .appliedPolicyIds(appliedPolicyIds)
                .evaluatedAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * 단일 정책 평가
     */
    private PolicyDecision evaluateSinglePolicy(SecurityPolicy policy, java.util.Map<String, Object> context) {
        // 간단한 규칙 평가
        // 실제 환경에서는 PolicyEngineService를 호출하여 복잡한 규칙 평가
        
        // JSON 규칙 파싱 및 평가는 PolicyEngineService에 위임
        // 여기서는 기본 허용 로직
        
        // 규칙이 없으면 기본 DENY
        if (policy.getRules() == null || policy.getRules().isEmpty()) {
            return PolicyDecision.DENY;
        }
        
        // 간단한 규칙 평가: "defaultAction": "ALLOW" or "DENY"
        if (policy.getRules().contains("\"defaultAction\": \"ALLOW\"") ||
            policy.getRules().contains("\"defaultAction\":\"ALLOW\"")) {
            return PolicyDecision.ALLOW;
        }
        
        return PolicyDecision.DENY;
    }
    
    /**
     * 정책 위반 이벤트 발행 (Feature 4 통합)
     */
    private void publishViolationEvent(
            Long tenantId,
            List<SecurityPolicy> policies,
            String resourceType,
            String action,
            java.util.Map<String, Object> context,
            PolicyEvaluationResult result) {
        
        if (policies.isEmpty()) {
            return;
        }
        
        // 첫 번째 적용된 정책 정보 사용
        SecurityPolicy violatedPolicy = policies.get(0);
        
        // 컨텍스트에서 사용자 정보 추출
        Long userId = context.get("userId") != null ? 
                Long.valueOf(context.get("userId").toString()) : null;
        String username = context.get("username") != null ? 
                context.get("username").toString() : null;
        String ipAddress = context.get("ipAddress") != null ? 
                context.get("ipAddress").toString() : null;
        String userAgent = context.get("userAgent") != null ? 
                context.get("userAgent").toString() : null;
        
        // 위반 타입 결정
        PolicyViolation.ViolationType violationType = determineViolationType(action, resourceType);
        
        // 위반 이벤트 생성
        PolicyViolationEvent event = PolicyViolationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .policyId(violatedPolicy.getId())
                .policyName(violatedPolicy.getPolicyName())
                .userId(userId)
                .username(username)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .violationType(violationType)
                .severity(violatedPolicy.getSeverity())
                .description(String.format("정책 위반: %s - %s", action, result.getReason()))
                .detectedAt(LocalDateTime.now())
                .resourceType(resourceType)
                .resourceId(context.get("resourceId") != null ? 
                        context.get("resourceId").toString() : null)
                .actionAttempted(action)
                .requiresAutoResponse(violatedPolicy.getSeverity() == SecurityPolicy.Severity.HIGH || 
                                     violatedPolicy.getSeverity() == SecurityPolicy.Severity.CRITICAL)
                .requiresNotification(true)
                .build();
        
        // 이벤트 발행
        log.warn("🚨 [정책 위반 감지] 이벤트 발행 - tenantId={}, policyId={}, violationType={}, severity={}", 
                tenantId, violatedPolicy.getId(), violationType, violatedPolicy.getSeverity());
        
        eventPublisher.publishEvent(event);
    }
    
    /**
     * 액션과 리소스 타입으로 위반 타입 결정
     */
    private PolicyViolation.ViolationType determineViolationType(String action, String resourceType) {
        if (action == null) {
            return PolicyViolation.ViolationType.POLICY_RULE_VIOLATION;
        }
        
        String actionLower = action.toLowerCase();
        
        // 액션 기반 위반 타입 매핑
        if (actionLower.contains("login") || actionLower.contains("auth")) {
            return PolicyViolation.ViolationType.AUTHENTICATION_FAILURE;
        } else if (actionLower.contains("access") || actionLower.contains("read") || actionLower.contains("view")) {
            return PolicyViolation.ViolationType.ACCESS_DENIED;
        } else if (actionLower.contains("create") || actionLower.contains("update") || actionLower.contains("delete")) {
            return PolicyViolation.ViolationType.AUTHORIZATION_FAILURE;
        } else if (actionLower.contains("rate") || actionLower.contains("limit")) {
            return PolicyViolation.ViolationType.RATE_LIMIT_EXCEEDED;
        } else {
            return PolicyViolation.ViolationType.POLICY_RULE_VIOLATION;
        }
    }
    
    // ==================== 내부 클래스 ====================
    
    /**
     * 정책 평가 결과
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PolicyEvaluationResult {
        private PolicyDecision decision;
        private String reason;
        private Integer appliedPolicyCount;
        private List<Long> appliedPolicyIds;
        private LocalDateTime evaluatedAt;
    }
    
    /**
     * 정책 결정
     */
    public enum PolicyDecision {
        ALLOW,
        DENY,
        NOT_APPLICABLE
    }
}

