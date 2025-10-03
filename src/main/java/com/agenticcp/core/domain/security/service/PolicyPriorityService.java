package com.agenticcp.core.domain.security.service;

import com.agenticcp.core.domain.security.dto.PolicyConflictResolution;
import com.agenticcp.core.domain.security.dto.PolicyConflictResolution.ConflictSeverity;
import com.agenticcp.core.domain.security.entity.SecurityPolicy;
import com.agenticcp.core.domain.security.enums.ConflictResolutionStrategy;
import com.agenticcp.core.domain.security.enums.PolicyDecision;
import com.agenticcp.core.domain.security.repository.SecurityPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 정책 우선순위 및 충돌 해결 서비스
 * 정책 간 우선순위를 관리하고 충돌을 해결하는 핵심 로직
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyPriorityService {
    
    private final SecurityPolicyRepository policyRepository;
    
    // 우선순위 상수
    public static final int MIN_PRIORITY = 1;
    public static final int MAX_PRIORITY = 1000;
    public static final int DEFAULT_PRIORITY = 500;
    public static final int PRIORITY_INCREMENT = 10;
    
    /**
     * 우선순위 유효성 검증
     * @param priority 검증할 우선순위
     * @return true if 유효한 우선순위
     */
    public boolean isValidPriority(Integer priority) {
        return priority != null && 
               priority >= MIN_PRIORITY && 
               priority <= MAX_PRIORITY;
    }
    
    /**
     * 우선순위 유효성 검증 (예외 발생)
     * @param priority 검증할 우선순위
     * @throws IllegalArgumentException 유효하지 않은 우선순위
     */
    public void validatePriority(Integer priority) {
        if (!isValidPriority(priority)) {
            throw new IllegalArgumentException(
                String.format("정책 우선순위는 %d~%d 범위여야 합니다. (입력값: %d)",
                    MIN_PRIORITY, MAX_PRIORITY, priority)
            );
        }
    }
    
    /**
     * 정책 목록을 우선순위 기준으로 정렬 (내림차순)
     * @param policies 정책 목록
     * @return 우선순위로 정렬된 정책 목록
     */
    public List<SecurityPolicy> sortByPriority(List<SecurityPolicy> policies) {
        return sortByPriority(policies, false);
    }
    
    /**
     * 정책 목록을 우선순위 기준으로 정렬
     * @param policies 정책 목록
     * @param ascending true면 오름차순, false면 내림차순
     * @return 정렬된 정책 목록
     */
    public List<SecurityPolicy> sortByPriority(List<SecurityPolicy> policies, boolean ascending) {
        if (policies == null || policies.isEmpty()) {
            return new ArrayList<>();
        }
        
        Comparator<SecurityPolicy> comparator = Comparator
            .comparing(SecurityPolicy::getPriority, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(SecurityPolicy::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
        
        if (!ascending) {
            comparator = comparator.reversed();
        }
        
        return policies.stream()
            .sorted(comparator)
            .collect(Collectors.toList());
    }
    
    /**
     * 자동 우선순위 할당
     * 기존 정책들의 우선순위를 분석하여 적절한 우선순위 자동 할당
     * @param tenantId 테넌트 ID (null이면 전역)
     * @return 할당된 우선순위
     */
    @Transactional(readOnly = true)
    public Integer assignAutoPriority(String tenantId) {
        List<SecurityPolicy> existingPolicies;
        
        if (tenantId != null) {
            existingPolicies = policyRepository.findByTenantIdAndIsEnabledTrue(tenantId);
        } else {
            existingPolicies = policyRepository.findByIsGlobalTrueAndIsEnabledTrue();
        }
        
        if (existingPolicies.isEmpty()) {
            return DEFAULT_PRIORITY;
        }
        
        // 평균 우선순위 계산
        double avgPriority = existingPolicies.stream()
            .map(SecurityPolicy::getPriority)
            .filter(Objects::nonNull)
            .mapToInt(Integer::intValue)
            .average()
            .orElse(DEFAULT_PRIORITY);
        
        // 평균에 가까운 값 반환 (10 단위로 반올림)
        int newPriority = ((int) Math.round(avgPriority / 10.0)) * 10;
        
        // 범위 검증
        newPriority = Math.max(MIN_PRIORITY, Math.min(MAX_PRIORITY, newPriority));
        
        log.debug("자동 우선순위 할당: {} (평균: {}, 정책 개수: {})", 
            newPriority, avgPriority, existingPolicies.size());
        
        return newPriority;
    }
    
    /**
     * 정책 충돌 해결
     * @param policies 충돌이 발생한 정책 목록
     * @param policyDecisions 각 정책의 평가 결과
     * @param strategy 충돌 해결 전략
     * @param resourceId 리소스 ID
     * @param resourceType 리소스 타입
     * @param action 액션
     * @return 충돌 해결 결과
     */
    public PolicyConflictResolution resolveConflict(
            List<SecurityPolicy> policies,
            Map<String, PolicyDecision> policyDecisions,
            ConflictResolutionStrategy strategy,
            String resourceId,
            String resourceType,
            String action) {
        
        LocalDateTime startTime = LocalDateTime.now();
        
        // 기본값 설정
        if (strategy == null) {
            strategy = ConflictResolutionStrategy.getDefault();
        }
        
        // 충돌 해결 객체 생성
        PolicyConflictResolution.PolicyConflictResolutionBuilder builder = PolicyConflictResolution.builder()
            .resolutionId(UUID.randomUUID().toString())
            .resourceId(resourceId)
            .resourceType(resourceType)
            .action(action)
            .strategy(strategy)
            .conflictDetectedAt(startTime)
            .conflictingPolicyIds(new ArrayList<>())
            .policyDecisions(new HashMap<>(policyDecisions))
            .policyPriorities(new HashMap<>());
        
        // 정책 정보 수집
        for (SecurityPolicy policy : policies) {
            String policyId = policy.getId().toString();
            builder.conflictingPolicyIds(
                new ArrayList<>(builder.build().getConflictingPolicyIds())
            );
            builder.build().getConflictingPolicyIds().add(policyId);
            builder.build().getPolicyPriorities().put(policyId, policy.getPriority());
        }
        
        PolicyConflictResolution resolution = builder.build();
        
        // 전략별 충돌 해결
        PolicyDecision finalDecision;
        String decidingPolicyId;
        String resolutionReason;
        
        switch (strategy) {
            case DENY_OVERRIDES:
                finalDecision = resolveDenyOverrides(resolution);
                decidingPolicyId = findDenyPolicy(resolution);
                resolutionReason = "하나 이상의 정책이 거부를 반환하여 최종 결과는 거부입니다.";
                break;
                
            case ALLOW_OVERRIDES:
                finalDecision = resolveAllowOverrides(resolution);
                decidingPolicyId = findAllowPolicy(resolution);
                resolutionReason = "하나 이상의 정책이 허용을 반환하여 최종 결과는 허용입니다.";
                break;
                
            case FIRST_MATCH:
            case HIGHEST_PRIORITY:
                finalDecision = resolveHighestPriority(policies, policyDecisions);
                decidingPolicyId = resolution.getHighestPriorityPolicyId();
                resolutionReason = String.format("우선순위가 가장 높은 정책(ID: %s)의 결과를 적용합니다.", decidingPolicyId);
                break;
                
            case LOWEST_PRIORITY:
                finalDecision = resolveLowestPriority(policies, policyDecisions);
                decidingPolicyId = resolution.getLowestPriorityPolicyId();
                resolutionReason = String.format("우선순위가 가장 낮은 정책(ID: %s)의 결과를 적용합니다.", decidingPolicyId);
                break;
                
            case MOST_RESTRICTIVE:
                finalDecision = PolicyDecision.DENY; // 가장 제한적 = DENY
                decidingPolicyId = findDenyPolicy(resolution);
                resolutionReason = "가장 제한적인 정책(DENY)을 적용합니다.";
                break;
                
            case MOST_PERMISSIVE:
                finalDecision = PolicyDecision.ALLOW; // 가장 허용적 = ALLOW
                decidingPolicyId = findAllowPolicy(resolution);
                resolutionReason = "가장 허용적인 정책(ALLOW)을 적용합니다.";
                break;
                
            case MAJORITY_WINS:
                finalDecision = resolveMajorityWins(resolution);
                decidingPolicyId = null; // 다수결은 특정 정책 없음
                resolutionReason = String.format("다수결 결과: ALLOW %d개, DENY %d개", 
                    resolution.getAllowCount(), resolution.getDenyCount());
                break;
                
            case WEIGHTED_SUM:
                finalDecision = resolveWeightedSum(policies, policyDecisions);
                decidingPolicyId = null; // 가중치 합산은 특정 정책 없음
                resolutionReason = "우선순위 가중치를 합산하여 최종 결과를 계산했습니다.";
                break;
                
            default:
                finalDecision = PolicyDecision.DENY; // 기본값은 거부
                decidingPolicyId = null;
                resolutionReason = "알 수 없는 전략으로 인해 기본값(DENY)을 반환합니다.";
                resolution.addWarning("알 수 없는 충돌 해결 전략: " + strategy);
        }
        
        // 결과 설정
        resolution.setFinalDecision(finalDecision);
        resolution.setDecidingPolicyId(decidingPolicyId);
        resolution.setResolutionReason(resolutionReason);
        LocalDateTime resolvedAt = LocalDateTime.now();
        resolution.setResolvedAt(resolvedAt);
        
        // 해결 시간 계산
        if (resolution.getConflictDetectedAt() != null) {
            long duration = java.time.Duration.between(
                resolution.getConflictDetectedAt(), 
                resolvedAt
            ).toMillis();
            resolution.setResolutionTimeMs(duration);
        }
        
        // 충돌 심각도 계산
        resolution.setSeverity(calculateConflictSeverity(resolution));
        
        log.info("정책 충돌 해결 완료: {} 정책, 전략: {}, 최종 결정: {}", 
            policies.size(), strategy, finalDecision);
        
        return resolution;
    }
    
    /**
     * DENY_OVERRIDES 전략 해결
     */
    private PolicyDecision resolveDenyOverrides(PolicyConflictResolution resolution) {
        long denyCount = resolution.getDenyCount();
        return denyCount > 0 ? PolicyDecision.DENY : PolicyDecision.ALLOW;
    }
    
    /**
     * ALLOW_OVERRIDES 전략 해결
     */
    private PolicyDecision resolveAllowOverrides(PolicyConflictResolution resolution) {
        long allowCount = resolution.getAllowCount();
        return allowCount > 0 ? PolicyDecision.ALLOW : PolicyDecision.DENY;
    }
    
    /**
     * HIGHEST_PRIORITY 전략 해결
     */
    private PolicyDecision resolveHighestPriority(
            List<SecurityPolicy> policies, 
            Map<String, PolicyDecision> decisions) {
        
        return policies.stream()
            .max(Comparator.comparing(SecurityPolicy::getPriority, Comparator.nullsLast(Comparator.naturalOrder())))
            .map(p -> decisions.get(p.getId().toString()))
            .orElse(PolicyDecision.DENY);
    }
    
    /**
     * LOWEST_PRIORITY 전략 해결
     */
    private PolicyDecision resolveLowestPriority(
            List<SecurityPolicy> policies,
            Map<String, PolicyDecision> decisions) {
        
        return policies.stream()
            .min(Comparator.comparing(SecurityPolicy::getPriority, Comparator.nullsLast(Comparator.naturalOrder())))
            .map(p -> decisions.get(p.getId().toString()))
            .orElse(PolicyDecision.DENY);
    }
    
    /**
     * MAJORITY_WINS 전략 해결
     */
    private PolicyDecision resolveMajorityWins(PolicyConflictResolution resolution) {
        long allowCount = resolution.getAllowCount();
        long denyCount = resolution.getDenyCount();
        
        if (allowCount > denyCount) {
            return PolicyDecision.ALLOW;
        } else if (denyCount > allowCount) {
            return PolicyDecision.DENY;
        } else {
            // 동수일 경우 보안 우선 (DENY)
            resolution.addWarning("ALLOW와 DENY가 동수입니다. 보안을 위해 DENY를 선택합니다.");
            return PolicyDecision.DENY;
        }
    }
    
    /**
     * WEIGHTED_SUM 전략 해결
     */
    private PolicyDecision resolveWeightedSum(
            List<SecurityPolicy> policies,
            Map<String, PolicyDecision> decisions) {
        
        int allowWeight = 0;
        int denyWeight = 0;
        
        for (SecurityPolicy policy : policies) {
            String policyId = policy.getId().toString();
            PolicyDecision decision = decisions.get(policyId);
            int priority = policy.getPriority() != null ? policy.getPriority() : DEFAULT_PRIORITY;
            
            if (decision == PolicyDecision.ALLOW) {
                allowWeight += priority;
            } else if (decision == PolicyDecision.DENY) {
                denyWeight += priority;
            }
        }
        
        return allowWeight > denyWeight ? PolicyDecision.ALLOW : PolicyDecision.DENY;
    }
    
    /**
     * DENY 정책 찾기
     */
    private String findDenyPolicy(PolicyConflictResolution resolution) {
        return resolution.getPolicyDecisions().entrySet().stream()
            .filter(e -> e.getValue() == PolicyDecision.DENY)
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * ALLOW 정책 찾기
     */
    private String findAllowPolicy(PolicyConflictResolution resolution) {
        return resolution.getPolicyDecisions().entrySet().stream()
            .filter(e -> e.getValue() == PolicyDecision.ALLOW)
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 충돌 심각도 계산
     */
    private ConflictSeverity calculateConflictSeverity(PolicyConflictResolution resolution) {
        int conflictCount = resolution.getConflictCount();
        boolean hasMixedDecisions = resolution.hasMixedDecisions();
        
        if (!hasMixedDecisions) {
            return ConflictSeverity.LOW; // 모두 같은 결정이면 낮음
        }
        
        if (conflictCount >= 10) {
            return ConflictSeverity.CRITICAL; // 10개 이상 충돌은 치명적
        } else if (conflictCount >= 5) {
            return ConflictSeverity.HIGH; // 5개 이상 충돌은 높음
        } else if (conflictCount >= 3) {
            return ConflictSeverity.MEDIUM; // 3개 이상 충돌은 보통
        } else {
            return ConflictSeverity.LOW; // 3개 미만은 낮음
        }
    }
    
    /**
     * 다음 사용 가능한 우선순위 찾기
     * @param tenantId 테넌트 ID
     * @param currentPriority 현재 우선순위
     * @return 다음 사용 가능한 우선순위
     */
    @Transactional(readOnly = true)
    public Integer findNextAvailablePriority(String tenantId, Integer currentPriority) {
        if (currentPriority == null) {
            currentPriority = DEFAULT_PRIORITY;
        }
        
        List<SecurityPolicy> policies;
        if (tenantId != null) {
            policies = policyRepository.findByTenantIdAndIsEnabledTrue(tenantId);
        } else {
            policies = policyRepository.findByIsGlobalTrueAndIsEnabledTrue();
        }
        
        Set<Integer> usedPriorities = policies.stream()
            .map(SecurityPolicy::getPriority)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        
        // 현재 우선순위부터 위로 탐색
        for (int priority = currentPriority; priority <= MAX_PRIORITY; priority += PRIORITY_INCREMENT) {
            if (!usedPriorities.contains(priority)) {
                return priority;
            }
        }
        
        // 위로 찾지 못하면 아래로 탐색
        for (int priority = currentPriority - PRIORITY_INCREMENT; priority >= MIN_PRIORITY; priority -= PRIORITY_INCREMENT) {
            if (!usedPriorities.contains(priority)) {
                return priority;
            }
        }
        
        // 모든 우선순위가 사용 중이면 현재 우선순위 반환
        return currentPriority;
    }
}

