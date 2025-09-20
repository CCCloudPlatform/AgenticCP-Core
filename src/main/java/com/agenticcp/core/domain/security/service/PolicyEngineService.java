package com.agenticcp.core.domain.security.service;

import com.agenticcp.core.domain.security.dto.*;
import com.agenticcp.core.domain.security.entity.SecurityPolicy;
import com.agenticcp.core.domain.security.enums.PolicyDecision;
import com.agenticcp.core.domain.security.repository.SecurityPolicyRepository;
// import com.fasterxml.jackson.core.JsonProcessingException;
// import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 정책 엔진 서비스
 * 
 * <p>정책 평가의 핵심 로직을 담당하는 서비스입니다.
 * 정책 조건과 규칙을 평가하여 최종 결정을 내립니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PolicyEngineService {
    
    private final SecurityPolicyRepository securityPolicyRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final PolicyJsonParser policyJsonParser;
    
    private static final String CACHE_KEY_PREFIX = "policy_evaluation:";
    private static final String CACHE_KEY_POLICIES = "applicable_policies:";
    private static final int CACHE_TTL_MINUTES = 5;
    
    /**
     * 정책 평가 수행
     * 
     * @param request 정책 평가 요청
     * @return 정책 평가 결과
     */
    public PolicyEvaluationResult evaluatePolicy(PolicyEvaluationRequest request) {
        log.debug("정책 평가 시작: {}", request);
        
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            // 요청 유효성 검증
            validateRequest(request);
            
            // 캐시에서 결과 확인
            PolicyEvaluationResult cachedResult = getCachedResult(request);
            if (cachedResult != null && !cachedResult.isExpired()) {
                log.debug("캐시된 정책 평가 결과 반환: {}", cachedResult);
                return cachedResult;
            }
            
            // 적용 가능한 정책 조회
            List<SecurityPolicy> applicablePolicies = getApplicablePolicies(request);
            log.debug("적용 가능한 정책 수: {}", applicablePolicies.size());
            
            // 정책 우선순위별 평가
            PolicyEvaluationResult result = evaluatePoliciesByPriority(applicablePolicies, request);
            
            // 평가 시간 설정
            result.setEvaluationTime(startTime);
            
            // 결과 캐싱
            cacheResult(request, result);
            
            log.info("정책 평가 완료: decision={}, policyKey={}, evaluationTime={}ms", 
                result.getDecision(), result.getPolicyKey(), result.getEvaluationTimeMs());
            
            return result;
            
        } catch (Exception e) {
            log.error("정책 평가 중 오류 발생: {}", e.getMessage(), e);
            return PolicyEvaluationResult.deny("정책 평가 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 적용 가능한 정책 조회
     * 
     * @param request 정책 평가 요청
     * @return 적용 가능한 정책 목록
     */
    private List<SecurityPolicy> getApplicablePolicies(PolicyEvaluationRequest request) {
        String cacheKey = CACHE_KEY_POLICIES + request.getResourceType() + ":" + request.getAction();
        
        // 캐시에서 확인
        @SuppressWarnings("unchecked")
        List<SecurityPolicy> cachedPolicies = (List<SecurityPolicy>) redisTemplate.opsForValue().get(cacheKey);
        if (cachedPolicies != null) {
            return cachedPolicies;
        }
        
        List<SecurityPolicy> policies = new ArrayList<>();
        
        // 글로벌 정책 조회
        policies.addAll(securityPolicyRepository.findGlobalPolicies(com.agenticcp.core.common.enums.Status.ACTIVE));
        
        // 테넌트별 정책 조회 (임시로 모든 활성 정책 조회)
        if (request.getTenantKey() != null) {
            policies.addAll(securityPolicyRepository.findActivePolicies(com.agenticcp.core.common.enums.Status.ACTIVE));
        }
        
        // 리소스 타입별 필터링
        List<SecurityPolicy> filteredPolicies = policies.stream()
                .filter(policy -> isPolicyApplicable(policy, request))
                .filter(policy -> isPolicyEffective(policy))
                .collect(Collectors.toList());
        
        // 우선순위별 정렬
        filteredPolicies.sort(Comparator
                .comparing(SecurityPolicy::getPriority, Comparator.reverseOrder())
                .thenComparing(SecurityPolicy::getCreatedAt, Comparator.reverseOrder()));
        
        // 캐시에 저장
        redisTemplate.opsForValue().set(cacheKey, filteredPolicies, Duration.ofMinutes(CACHE_TTL_MINUTES));
        
        return filteredPolicies;
    }
    
    /**
     * 정책 우선순위별 평가
     * 
     * @param policies 정책 목록
     * @param request 정책 평가 요청
     * @return 정책 평가 결과
     */
    private PolicyEvaluationResult evaluatePoliciesByPriority(List<SecurityPolicy> policies, PolicyEvaluationRequest request) {
        for (SecurityPolicy policy : policies) {
            PolicyEvaluationResult result = evaluateSinglePolicy(policy, request);
            if (result.getDecision() != PolicyDecision.INCONCLUSIVE) {
                return result;
            }
        }
        
        // 기본 정책 (모든 정책이 INCONCLUSIVE인 경우)
        return PolicyEvaluationResult.allow("적용 가능한 정책이 없어 기본적으로 허용합니다");
    }
    
    /**
     * 단일 정책 평가
     * 
     * @param policy 평가할 정책
     * @param request 정책 평가 요청
     * @return 정책 평가 결과
     */
    private PolicyEvaluationResult evaluateSinglePolicy(SecurityPolicy policy, PolicyEvaluationRequest request) {
        try {
            log.debug("정책 평가 중: policyKey={}", policy.getPolicyKey());
            
            // 조건 평가
            if (!evaluateConditions(policy.getConditions(), request)) {
                return PolicyEvaluationResult.inconclusive("조건을 만족하지 않습니다");
            }
            
            // 규칙 평가
            PolicyDecision decision = evaluateRules(policy.getRules(), request);
            
            PolicyEvaluationResult result = PolicyEvaluationResult.builder()
                    .decision(decision)
                    .policyKey(policy.getPolicyKey())
                    .policyName(policy.getPolicyName())
                    .reason(decision == PolicyDecision.ALLOW ? "정책이 접근을 허용합니다" : "정책이 접근을 거부합니다")
                    .policyPriority(policy.getPriority())
                    .policyVersion("1.0") // TODO: 버전 필드 추가 시 수정
                    .policyCreatedAt(policy.getCreatedAt())
                    .policyUpdatedAt(policy.getUpdatedAt())
                    .evaluatedAt(LocalDateTime.now())
                    .build();
            
            // 액션 정보 추가
            if (policy.getActions() != null) {
                result.setActions(parseActions(policy.getActions()));
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("정책 평가 중 오류 발생: policyKey={}, error={}", policy.getPolicyKey(), e.getMessage(), e);
            return PolicyEvaluationResult.inconclusive("정책 평가 중 오류가 발생했습니다");
        }
    }
    
    /**
     * 정책 조건 평가
     * 
     * @param conditionsJson 조건 JSON
     * @param request 정책 평가 요청
     * @return 조건 만족 여부
     */
    private boolean evaluateConditions(String conditionsJson, PolicyEvaluationRequest request) {
        if (conditionsJson == null || conditionsJson.isEmpty()) {
            return true; // 조건이 없으면 항상 만족
        }
        
        try {
            // JSON 파싱
            PolicyConditions conditions = policyJsonParser.parsePolicyConditions(conditionsJson);
            if (conditions.isEmpty()) {
                return true; // 조건이 비어있으면 항상 만족
            }
            
            // 각 조건 타입별 평가
            boolean timeResult = evaluateTimeConditions(conditions.getTimeConditions());
            boolean ipResult = evaluateIpConditions(conditions.getIpConditions(), request.getClientIp());
            boolean userResult = evaluateUserConditions(conditions.getUserConditions(), request);
            boolean resourceResult = evaluateResourceConditions(conditions.getResourceConditions(), request);
            boolean networkResult = evaluateNetworkConditions(conditions.getNetworkConditions(), request);
            boolean environmentResult = evaluateEnvironmentConditions(conditions.getEnvironmentConditions(), request);
            
            // 평가 모드에 따른 최종 결과 결정
            if (conditions.getEvaluationMode() == PolicyConditions.ConditionEvaluationMode.ALL) {
                return timeResult && ipResult && userResult && resourceResult && networkResult && environmentResult;
            } else {
                return timeResult || ipResult || userResult || resourceResult || networkResult || environmentResult;
            }
            
        } catch (Exception e) {
            log.error("조건 평가 중 오류 발생: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 시간 조건 평가
     * 
     * @param timeConditions 시간 조건
     * @return 조건 만족 여부
     */
    private boolean evaluateTimeConditions(TimeConditions timeConditions) {
        if (timeConditions == null) {
            return true; // 시간 조건이 없으면 항상 만족
        }
        
        try {
            boolean timeAllowed = timeConditions.isCurrentTimeAllowed();
            boolean dayAllowed = timeConditions.isCurrentDayAllowed();
            
            log.debug("시간 조건 평가: timeAllowed={}, dayAllowed={}", timeAllowed, dayAllowed);
            return timeAllowed && dayAllowed;
            
        } catch (Exception e) {
            log.error("시간 조건 평가 중 오류 발생: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * IP 조건 평가
     * 
     * @param ipConditions IP 조건
     * @param clientIp 클라이언트 IP
     * @return 조건 만족 여부
     */
    private boolean evaluateIpConditions(IpConditions ipConditions, String clientIp) {
        if (ipConditions == null) {
            return true; // IP 조건이 없으면 항상 만족
        }
        
        if (clientIp == null || clientIp.isEmpty()) {
            log.debug("클라이언트 IP가 없어 IP 조건 평가를 건너뜁니다");
            return true;
        }
        
        try {
            boolean ipAllowed = ipConditions.isIpAllowed(clientIp);
            log.debug("IP 조건 평가: clientIp={}, allowed={}", clientIp, ipAllowed);
            return ipAllowed;
            
        } catch (Exception e) {
            log.error("IP 조건 평가 중 오류 발생: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 사용자 조건 평가
     * 
     * @param userConditions 사용자 조건
     * @param request 정책 평가 요청
     * @return 조건 만족 여부
     */
    private boolean evaluateUserConditions(UserConditions userConditions, PolicyEvaluationRequest request) {
        if (userConditions == null) {
            return true; // 사용자 조건이 없으면 항상 만족
        }
        
        try {
            boolean userIdAllowed = userConditions.isUserIdAllowed(request.getUserId());
            
            // 사용자 역할 확인 (컨텍스트에서 가져옴)
            String userRole = (String) request.getContextValue("userRole");
            boolean roleAllowed = userRole == null || userConditions.isRoleAllowed(userRole);
            
            // 사용자 그룹 확인 (컨텍스트에서 가져옴)
            String userGroup = (String) request.getContextValue("userGroup");
            boolean groupAllowed = userGroup == null || userConditions.isUserGroupAllowed(userGroup);
            
            log.debug("사용자 조건 평가: userId={}, role={}, group={}, userIdAllowed={}, roleAllowed={}, groupAllowed={}", 
                request.getUserId(), userRole, userGroup, userIdAllowed, roleAllowed, groupAllowed);
            
            return userIdAllowed && roleAllowed && groupAllowed;
            
        } catch (Exception e) {
            log.error("사용자 조건 평가 중 오류 발생: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 리소스 조건 평가
     * 
     * @param resourceConditions 리소스 조건
     * @param request 정책 평가 요청
     * @return 조건 만족 여부
     */
    private boolean evaluateResourceConditions(ResourceConditions resourceConditions, PolicyEvaluationRequest request) {
        if (resourceConditions == null) {
            return true; // 리소스 조건이 없으면 항상 만족
        }
        
        try {
            boolean resourceTypeAllowed = resourceConditions.isResourceTypeAllowed(request.getResourceType());
            boolean resourceIdAllowed = resourceConditions.isResourceIdAllowed(request.getResourceId());
            
            log.debug("리소스 조건 평가: resourceType={}, resourceId={}, typeAllowed={}, idAllowed={}", 
                request.getResourceType(), request.getResourceId(), resourceTypeAllowed, resourceIdAllowed);
            
            return resourceTypeAllowed && resourceIdAllowed;
            
        } catch (Exception e) {
            log.error("리소스 조건 평가 중 오류 발생: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 네트워크 조건 평가
     * 
     * @param networkConditions 네트워크 조건
     * @param request 정책 평가 요청
     * @return 조건 만족 여부
     */
    private boolean evaluateNetworkConditions(NetworkConditions networkConditions, PolicyEvaluationRequest request) {
        if (networkConditions == null) {
            return true; // 네트워크 조건이 없으면 항상 만족
        }
        
        try {
            // 프로토콜 확인 (컨텍스트에서 가져옴)
            String protocol = (String) request.getContextValue("protocol");
            boolean protocolAllowed = protocol == null || networkConditions.isProtocolAllowed(protocol);
            
            // 포트 확인 (컨텍스트에서 가져옴)
            Integer port = (Integer) request.getContextValue("port");
            boolean portAllowed = port == null || networkConditions.isPortAllowed(port);
            
            log.debug("네트워크 조건 평가: protocol={}, port={}, protocolAllowed={}, portAllowed={}", 
                protocol, port, protocolAllowed, portAllowed);
            
            return protocolAllowed && portAllowed;
            
        } catch (Exception e) {
            log.error("네트워크 조건 평가 중 오류 발생: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 환경 조건 평가
     * 
     * @param environmentConditions 환경 조건
     * @param request 정책 평가 요청
     * @return 조건 만족 여부
     */
    private boolean evaluateEnvironmentConditions(EnvironmentConditions environmentConditions, PolicyEvaluationRequest request) {
        if (environmentConditions == null) {
            return true; // 환경 조건이 없으면 항상 만족
        }
        
        try {
            boolean tenantAllowed = environmentConditions.isTenantAllowed(request.getTenantKey());
            
            // 환경 확인 (컨텍스트에서 가져옴)
            String environment = (String) request.getContextValue("environment");
            boolean environmentAllowed = environment == null || environmentConditions.isEnvironmentAllowed(environment);
            
            // 리전 확인 (컨텍스트에서 가져옴)
            String region = (String) request.getContextValue("region");
            boolean regionAllowed = region == null || environmentConditions.isRegionAllowed(region);
            
            log.debug("환경 조건 평가: tenant={}, environment={}, region={}, tenantAllowed={}, environmentAllowed={}, regionAllowed={}", 
                request.getTenantKey(), environment, region, tenantAllowed, environmentAllowed, regionAllowed);
            
            return tenantAllowed && environmentAllowed && regionAllowed;
            
        } catch (Exception e) {
            log.error("환경 조건 평가 중 오류 발생: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 정책 규칙 평가
     * 
     * @param rulesJson 규칙 JSON
     * @param request 정책 평가 요청
     * @return 정책 결정
     */
    private PolicyDecision evaluateRules(String rulesJson, PolicyEvaluationRequest request) {
        if (rulesJson == null || rulesJson.isEmpty()) {
            return PolicyDecision.INCONCLUSIVE;
        }
        
        try {
            // JSON 파싱
            PolicyRules rules = policyJsonParser.parsePolicyRules(rulesJson);
            if (rules.isEmpty()) {
                // 기본 액션이 있으면 반환
                if (rules.getDefaultAction() != null) {
                    return PolicyDecision.fromCode(rules.getDefaultAction());
                }
                return PolicyDecision.INCONCLUSIVE;
            }
            
            // 규칙 평가 모드에 따른 처리
            if (rules.getEvaluationMode() == PolicyRules.RuleEvaluationMode.ALL) {
                return evaluateAllRules(rules.getRules(), request);
            } else if (rules.getEvaluationMode() == PolicyRules.RuleEvaluationMode.ANY) {
                return evaluateAnyRules(rules.getRules(), request);
            } else {
                return evaluateFirstRule(rules.getRules(), request);
            }
            
        } catch (Exception e) {
            log.error("규칙 평가 중 오류 발생: {}", e.getMessage(), e);
            return PolicyDecision.INCONCLUSIVE;
        }
    }
    
    /**
     * 모든 규칙 평가 (ALL 모드)
     * 
     * @param rules 규칙 목록
     * @param request 정책 평가 요청
     * @return 정책 결정
     */
    private PolicyDecision evaluateAllRules(List<PolicyRule> rules, PolicyEvaluationRequest request) {
        boolean allRulesMatched = true;
        PolicyDecision lastDecision = PolicyDecision.INCONCLUSIVE;
        
        for (PolicyRule rule : rules) {
            if (!rule.isEnabled()) {
                continue;
            }
            
            if (evaluateRuleCondition(rule, request)) {
                lastDecision = PolicyDecision.fromCode(rule.getAction());
                log.debug("규칙 매칭: ruleId={}, action={}", rule.getRuleId(), rule.getAction());
            } else {
                allRulesMatched = false;
                break;
            }
        }
        
        return allRulesMatched ? lastDecision : PolicyDecision.INCONCLUSIVE;
    }
    
    /**
     * 하나의 규칙 평가 (ANY 모드)
     * 
     * @param rules 규칙 목록
     * @param request 정책 평가 요청
     * @return 정책 결정
     */
    private PolicyDecision evaluateAnyRules(List<PolicyRule> rules, PolicyEvaluationRequest request) {
        for (PolicyRule rule : rules) {
            if (!rule.isEnabled()) {
                continue;
            }
            
            if (evaluateRuleCondition(rule, request)) {
                PolicyDecision decision = PolicyDecision.fromCode(rule.getAction());
                log.debug("규칙 매칭: ruleId={}, action={}", rule.getRuleId(), rule.getAction());
                return decision;
            }
        }
        
        return PolicyDecision.INCONCLUSIVE;
    }
    
    /**
     * 첫 번째 규칙 평가 (FIRST 모드)
     * 
     * @param rules 규칙 목록
     * @param request 정책 평가 요청
     * @return 정책 결정
     */
    private PolicyDecision evaluateFirstRule(List<PolicyRule> rules, PolicyEvaluationRequest request) {
        // 우선순위별 정렬
        List<PolicyRule> sortedRules = rules.stream()
                .filter(PolicyRule::isEnabled)
                .sorted((r1, r2) -> Integer.compare(r2.getPriority(), r1.getPriority()))
                .toList();
        
        for (PolicyRule rule : sortedRules) {
            if (evaluateRuleCondition(rule, request)) {
                PolicyDecision decision = PolicyDecision.fromCode(rule.getAction());
                log.debug("규칙 매칭: ruleId={}, action={}, priority={}", 
                    rule.getRuleId(), rule.getAction(), rule.getPriority());
                return decision;
            }
        }
        
        return PolicyDecision.INCONCLUSIVE;
    }
    
    /**
     * 개별 규칙 조건 평가
     * 
     * @param rule 규칙
     * @param request 정책 평가 요청
     * @return 조건 만족 여부
     */
    private boolean evaluateRuleCondition(PolicyRule rule, PolicyEvaluationRequest request) {
        if (rule.getCondition() == null || rule.getCondition().isEmpty()) {
            return true; // 조건이 없으면 항상 만족
        }
        
        try {
            // 간단한 조건 평가 (실제 구현에서는 더 정교한 표현식 파서 필요)
            String condition = rule.getCondition();
            
            // 사용자 역할 조건 예시: "user.role == 'ADMIN'"
            if (condition.contains("user.role")) {
                String userRole = (String) request.getContextValue("userRole");
                if (condition.contains("==")) {
                    String expectedRole = extractStringValue(condition, "==");
                    return expectedRole.equals(userRole);
                } else if (condition.contains("!=")) {
                    String expectedRole = extractStringValue(condition, "!=");
                    return !expectedRole.equals(userRole);
                }
            }
            
            // 리소스 타입 조건 예시: "resource.type == 'EC2'"
            if (condition.contains("resource.type")) {
                if (condition.contains("==")) {
                    String expectedType = extractStringValue(condition, "==");
                    return expectedType.equals(request.getResourceType());
                } else if (condition.contains("!=")) {
                    String expectedType = extractStringValue(condition, "!=");
                    return !expectedType.equals(request.getResourceType());
                }
            }
            
            // 액션 조건 예시: "action == 'CREATE'"
            if (condition.contains("action")) {
                if (condition.contains("==")) {
                    String expectedAction = extractStringValue(condition, "==");
                    return expectedAction.equals(request.getAction());
                } else if (condition.contains("!=")) {
                    String expectedAction = extractStringValue(condition, "!=");
                    return !expectedAction.equals(request.getAction());
                }
            }
            
            // TODO: 더 복잡한 조건 평가 로직 구현
            log.debug("규칙 조건 평가: condition={}, result=false (미구현)", condition);
            return false;
            
        } catch (Exception e) {
            log.error("규칙 조건 평가 중 오류 발생: ruleId={}, condition={}, error={}", 
                rule.getRuleId(), rule.getCondition(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 조건 문자열에서 값 추출
     * 
     * @param condition 조건 문자열
     * @param operator 연산자
     * @return 추출된 값
     */
    private String extractStringValue(String condition, String operator) {
        try {
            String[] parts = condition.split(operator);
            if (parts.length >= 2) {
                String value = parts[1].trim();
                // 따옴표 제거
                if (value.startsWith("'") && value.endsWith("'")) {
                    return value.substring(1, value.length() - 1);
                } else if (value.startsWith("\"") && value.endsWith("\"")) {
                    return value.substring(1, value.length() - 1);
                }
                return value;
            }
        } catch (Exception e) {
            log.debug("값 추출 중 오류 발생: condition={}, operator={}, error={}", 
                condition, operator, e.getMessage());
        }
        return "";
    }
    
    /**
     * 정책이 적용 가능한지 확인
     * 
     * @param policy 정책
     * @param request 정책 평가 요청
     * @return 적용 가능 여부
     */
    private boolean isPolicyApplicable(SecurityPolicy policy, PolicyEvaluationRequest request) {
        // 정책이 활성화되어 있는지 확인
        if (!policy.getIsEnabled()) {
            return false;
        }
        
        // 정책 상태 확인
        if (policy.getStatus() != com.agenticcp.core.common.enums.Status.ACTIVE) {
            return false;
        }
        
        // 유효 기간 확인
        LocalDateTime now = LocalDateTime.now();
        if (policy.getEffectiveFrom() != null && now.isBefore(policy.getEffectiveFrom())) {
            return false;
        }
        if (policy.getEffectiveUntil() != null && now.isAfter(policy.getEffectiveUntil())) {
            return false;
        }
        
        // 리소스 타입 확인
        if (policy.getTargetResources() != null) {
            // TODO: 리소스 타입 매칭 로직 구현
        }
        
        return true;
    }
    
    /**
     * 정책이 유효한지 확인
     * 
     * @param policy 정책
     * @return 유효 여부
     */
    private boolean isPolicyEffective(SecurityPolicy policy) {
        // 정책이 활성화되어 있고 유효한 상태인지 확인
        return policy.getIsEnabled() && 
               policy.getStatus() == com.agenticcp.core.common.enums.Status.ACTIVE;
    }
    
    /**
     * 액션 파싱
     * 
     * @param actionsJson 액션 JSON
     * @return 액션 목록
     */
    private List<com.agenticcp.core.domain.security.dto.PolicyAction> parseActions(String actionsJson) {
        // TODO: JSON에서 액션 목록 파싱 로직 구현
        return new ArrayList<>();
    }
    
    /**
     * 요청 유효성 검증
     * 
     * @param request 정책 평가 요청
     */
    private void validateRequest(PolicyEvaluationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("정책 평가 요청이 null입니다");
        }
        if (request.getResourceType() == null || request.getResourceType().isEmpty()) {
            throw new IllegalArgumentException("리소스 타입이 필요합니다");
        }
        if (request.getAction() == null || request.getAction().isEmpty()) {
            throw new IllegalArgumentException("액션이 필요합니다");
        }
        if (request.getUserId() == null || request.getUserId().isEmpty()) {
            throw new IllegalArgumentException("사용자 ID가 필요합니다");
        }
    }
    
    /**
     * 캐시에서 결과 조회
     * 
     * @param request 정책 평가 요청
     * @return 캐시된 결과
     */
    private PolicyEvaluationResult getCachedResult(PolicyEvaluationRequest request) {
        String cacheKey = generateCacheKey(request);
        return (PolicyEvaluationResult) redisTemplate.opsForValue().get(cacheKey);
    }
    
    /**
     * 결과 캐싱
     * 
     * @param request 정책 평가 요청
     * @param result 정책 평가 결과
     */
    private void cacheResult(PolicyEvaluationRequest request, PolicyEvaluationResult result) {
        String cacheKey = generateCacheKey(request);
        result.setExpirationMinutes(CACHE_TTL_MINUTES);
        redisTemplate.opsForValue().set(cacheKey, result, Duration.ofMinutes(CACHE_TTL_MINUTES));
    }
    
    /**
     * 캐시 키 생성
     * 
     * @param request 정책 평가 요청
     * @return 캐시 키
     */
    private String generateCacheKey(PolicyEvaluationRequest request) {
        return CACHE_KEY_PREFIX + 
               request.getResourceType() + ":" + 
               request.getAction() + ":" + 
               request.getUserId() + ":" + 
               (request.getTenantKey() != null ? request.getTenantKey() : "global");
    }
    
    /**
     * 정책 캐시 무효화
     * 
     * @param resourceType 리소스 타입
     * @param action 액션
     */
    public void evictPolicyCache(String resourceType, String action) {
        String cacheKey = CACHE_KEY_POLICIES + resourceType + ":" + action;
        redisTemplate.delete(cacheKey);
        
        // 관련된 모든 평가 결과 캐시도 무효화
        Set<String> keys = redisTemplate.keys(CACHE_KEY_PREFIX + resourceType + ":" + action + ":*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
    
    /**
     * 모든 정책 캐시 무효화
     */
    public void evictAllPolicyCache() {
        Set<String> keys = redisTemplate.keys(CACHE_KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        
        keys = redisTemplate.keys(CACHE_KEY_POLICIES + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
