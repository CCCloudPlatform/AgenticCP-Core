package com.agenticcp.core.domain.platform.service;

import com.agenticcp.core.domain.platform.dto.targeting.TargetRuleEvaluationRequest;
import com.agenticcp.core.domain.platform.dto.targeting.TargetRuleEvaluationResponse;
import com.agenticcp.core.domain.platform.entity.FeatureFlag;
import com.agenticcp.core.domain.platform.entity.FeatureFlagTargetRule;
import com.agenticcp.core.domain.platform.repository.FeatureFlagTargetRuleRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.platform.enums.TargetingRuleErrorCode;
import com.agenticcp.core.common.util.LogMaskingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 타겟팅 조건 평가 서비스
 * 
 * 기능 플래그의 타겟팅 규칙을 평가하여 특정 사용자/테넌트에 대해 
 * 기능 플래그가 활성화되어야 하는지 판단합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TargetingConditionEvaluator {

    private final FeatureFlagTargetRuleRepository targetingRuleRepository;
    private final ObjectMapper objectMapper;

    /**
     * 타겟팅 규칙 평가
     * 
     * @param featureFlag 평가할 기능 플래그
     * @param request 평가 요청 정보
     * @return 평가 결과
     */
    public TargetRuleEvaluationResponse evaluateTargeting(FeatureFlag featureFlag, 
                                                           TargetRuleEvaluationRequest request) {
        long startTime = System.currentTimeMillis();
        
        log.info("[TargetingConditionEvaluator] evaluateTargeting - flagKey={} userId={}", 
                LogMaskingUtils.mask(featureFlag.getFlagKey(), 2, 2),
                LogMaskingUtils.mask(request.getUserId(), 2, 2));
        
        // 1. 기본 활성화 상태 확인
        if (!featureFlag.getIsEnabled()) {
            throw new BusinessException(TargetingRuleErrorCode.FEATURE_FLAG_DISABLED);
        }
        
        // 2. 유효 기간 확인
        if (!isWithinValidPeriod(featureFlag)) {
            throw new BusinessException(TargetingRuleErrorCode.FEATURE_FLAG_EXPIRED);
        }
        
        // 3. 활성화된 타겟팅 규칙 조회
        List<FeatureFlagTargetRule> activeRules = targetingRuleRepository
                .findActiveRulesByFeatureFlagId(featureFlag.getId(), false);
        
        if (activeRules.isEmpty()) {
            // 타겟팅 규칙이 없으면 기본값 반환 (rolloutPercentage 체크)
            boolean result = evaluateRolloutPercentage(featureFlag, request.getUserId());
            return createEvaluationResponse(result, null, null, null, 
                    "타겟팅 규칙이 없어 롤아웃 비율로 평가했습니다", startTime);
        }
        
        // 4. 타겟팅 규칙 평가 (우선순위 순으로)
        List<TargetRuleEvaluationResponse.RuleEvaluationDetailDto> evaluationDetails = new ArrayList<>();
        
        for (FeatureFlagTargetRule rule : activeRules) {
            boolean matched = evaluateRule(rule, request);
            String reason = matched ? "규칙 조건에 매칭됨" : "규칙 조건에 매칭되지 않음";
            
            evaluationDetails.add(TargetRuleEvaluationResponse.RuleEvaluationDetailDto.builder()
                    .ruleId(rule.getId())
                    .ruleName(rule.getRuleName())
                    .ruleType(rule.getRuleType().name())
                    .matched(matched)
                    .reason(reason)
                    .priority(rule.getPriority())
                    .build());
            
            // 첫 번째로 매칭되는 규칙에서 평가 종료
            if (matched) {
                long evaluationTime = System.currentTimeMillis() - startTime;
                return TargetRuleEvaluationResponse.builder()
                        .result(true)
                        .matchedRuleId(rule.getId())
                        .matchedRuleName(rule.getRuleName())
                        .matchedRuleType(rule.getRuleType().name())
                        .evaluationDetails(evaluationDetails)
                        .evaluationTimeMs(evaluationTime)
                        .message("타겟팅 규칙 평가 완료")
                        .build();
            }
        }
        
        long evaluationTime = System.currentTimeMillis() - startTime;
        return TargetRuleEvaluationResponse.builder()
                .result(false)
                .evaluationDetails(evaluationDetails)
                .evaluationTimeMs(evaluationTime)
                .message("모든 타겟팅 규칙을 평가했지만 매칭되지 않았습니다")
                .build();
    }

    /**
     * 개별 규칙 평가
     * 
     * @param rule 평가할 규칙
     * @param request 평가 요청 정보
     * @return 규칙 매칭 여부
     */
    private boolean evaluateRule(FeatureFlagTargetRule rule, TargetRuleEvaluationRequest request) {
        try {
            switch (rule.getRuleType()) {
                case CLOUD_PROVIDER:
                    return evaluateCloudProviderRule(rule, request.getCloudProvider());
                case CLOUD_REGION:
                    return evaluateRegionRule(rule, request.getRegion());
                case TENANT_TYPE:
                    return evaluateTenantTypeRule(rule, request.getTenantType());
                case TENANT_TIER:
                    return evaluateTenantTierRule(rule, request.getTenantTier());
                case USER_ROLE:
                    return evaluateUserRoleRule(rule, request.getUserRole());
                case USER_ATTRIBUTE:
                    return evaluateUserAttributeRule(rule, request.getUserAttributes());
                case CUSTOM_ATTRIBUTE:
                    return evaluateCustomAttributeRule(rule, request.getCustomAttributes());
                case PERCENTAGE_ROLLOUT:
                    return evaluatePercentageRolloutRule(rule, request.getUserId());
                default:
                    log.warn("[TargetingConditionEvaluator] Unknown rule type: {}", rule.getRuleType());
                    return false;
            }
        } catch (BusinessException e) {
            // 비즈니스 예외는 그대로 전파
            throw e;
        } catch (Exception e) {
            log.error("[TargetingConditionEvaluator] Error evaluating rule: {}", rule.getId(), e);
            throw new BusinessException(TargetingRuleErrorCode.TARGETING_EVALUATION_ERROR, 
                    "규칙 평가 중 오류가 발생했습니다: " + rule.getId());
        }
    }

    /**
     * 클라우드 프로바이더 규칙 평가
     */
    private boolean evaluateCloudProviderRule(FeatureFlagTargetRule rule, String cloudProvider) {
        if (cloudProvider == null) return false;
        
        List<String> targetProviders = parseJsonArray(rule.getRuleValue());
        return targetProviders.contains(cloudProvider.toLowerCase());
    }

    /**
     * 클라우드 리전 규칙 평가
     */
    private boolean evaluateRegionRule(FeatureFlagTargetRule rule, String region) {
        if (region == null) return false;
        
        List<String> targetRegions = parseJsonArray(rule.getRuleValue());
        return targetRegions.contains(region.toLowerCase());
    }

    /**
     * 테넌트 타입 규칙 평가
     */
    private boolean evaluateTenantTypeRule(FeatureFlagTargetRule rule, String tenantType) {
        if (tenantType == null) return false;
        
        List<String> targetTypes = parseJsonArray(rule.getRuleValue());
        return targetTypes.contains(tenantType.toLowerCase());
    }

    /**
     * 테넌트 등급 규칙 평가
     */
    private boolean evaluateTenantTierRule(FeatureFlagTargetRule rule, String tenantTier) {
        if (tenantTier == null) return false;
        
        List<String> targetTiers = parseJsonArray(rule.getRuleValue());
        return targetTiers.contains(tenantTier.toLowerCase());
    }

    /**
     * 사용자 역할 규칙 평가
     */
    private boolean evaluateUserRoleRule(FeatureFlagTargetRule rule, String userRole) {
        if (userRole == null) return false;
        
        List<String> targetRoles = parseJsonArray(rule.getRuleValue());
        return targetRoles.contains(userRole.toLowerCase());
    }

    /**
     * 사용자 속성 규칙 평가
     */
    private boolean evaluateUserAttributeRule(FeatureFlagTargetRule rule, Map<String, Object> userAttributes) {
        if (userAttributes == null || userAttributes.isEmpty()) return false;
        
        try {
            Map<String, Object> conditions = objectMapper.readValue(
                    rule.getRuleCondition(), new TypeReference<Map<String, Object>>() {});
            
            for (Map.Entry<String, Object> condition : conditions.entrySet()) {
                String attributeKey = condition.getKey();
                Object expectedValue = condition.getValue();
                Object actualValue = userAttributes.get(attributeKey);
                
                if (!expectedValue.equals(actualValue)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.error("[TargetingConditionEvaluator] Error parsing user attribute conditions", e);
            throw new BusinessException(TargetingRuleErrorCode.USER_ATTRIBUTE_INVALID, 
                    "사용자 속성 조건 파싱 오류");
        }
    }

    /**
     * 커스텀 속성 규칙 평가
     */
    private boolean evaluateCustomAttributeRule(FeatureFlagTargetRule rule, Map<String, Object> customAttributes) {
        if (customAttributes == null || customAttributes.isEmpty()) return false;
        
        try {
            Map<String, Object> conditions = objectMapper.readValue(
                    rule.getRuleCondition(), new TypeReference<Map<String, Object>>() {});
            
            for (Map.Entry<String, Object> condition : conditions.entrySet()) {
                String attributeKey = condition.getKey();
                Object expectedValue = condition.getValue();
                Object actualValue = customAttributes.get(attributeKey);
                
                if (!expectedValue.equals(actualValue)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.error("[TargetingConditionEvaluator] Error parsing custom attribute conditions", e);
            throw new BusinessException(TargetingRuleErrorCode.CUSTOM_ATTRIBUTE_INVALID, 
                    "커스텀 속성 조건 파싱 오류");
        }
    }

    /**
     * 퍼센트 롤아웃 규칙 평가 (설정만 지원)
     */
    private boolean evaluatePercentageRolloutRule(FeatureFlagTargetRule rule, String userId) {
        // 이번에는 롤아웃 비율 설정만 지원하므로 항상 false 반환
        log.info("[TargetingConditionEvaluator] Percentage rollout rule evaluation not implemented yet");
        return false;
    }

    /**
     * 기능 플래그의 롤아웃 비율 평가
     */
    private boolean evaluateRolloutPercentage(FeatureFlag featureFlag, String userId) {
        Integer rolloutPercentage = featureFlag.getRolloutPercentage();
        if (rolloutPercentage == null || rolloutPercentage <= 0) {
            return false;
        }
        
        // 사용자 ID 기반 해시 계산으로 일관된 롤아웃 결정
        int hash = Math.abs(userId.hashCode());
        int bucket = hash % 100;
        
        return bucket < rolloutPercentage;
    }

    /**
     * 기능 플래그 유효 기간 확인
     */
    private boolean isWithinValidPeriod(FeatureFlag featureFlag) {
        LocalDateTime now = LocalDateTime.now();
        
        if (featureFlag.getStartDate() != null && now.isBefore(featureFlag.getStartDate())) {
            return false;
        }
        
        if (featureFlag.getEndDate() != null && now.isAfter(featureFlag.getEndDate())) {
            return false;
        }
        
        return true;
    }

    /**
     * JSON 배열 파싱
     */
    private List<String> parseJsonArray(String jsonValue) {
        try {
            if (jsonValue == null || jsonValue.trim().isEmpty()) {
                return List.of();
            }
            
            List<String> result = objectMapper.readValue(jsonValue, new TypeReference<List<String>>() {});
            return result.stream()
                    .map(String::toLowerCase)
                    .toList();
        } catch (Exception e) {
            log.error("[TargetingConditionEvaluator] Error parsing JSON array: {}", jsonValue, e);
            throw new BusinessException(TargetingRuleErrorCode.RULE_VALUE_INVALID_JSON, 
                    "규칙 값 JSON 파싱 오류: " + jsonValue);
        }
    }

    /**
     * 평가 결과 생성 헬퍼 메서드
     */
    private TargetRuleEvaluationResponse createEvaluationResponse(boolean result, Long matchedRuleId, 
                                                                   String matchedRuleName, String matchedRuleType,
                                                                   String message, long startTime) {
        long evaluationTime = System.currentTimeMillis() - startTime;
        
        return TargetRuleEvaluationResponse.builder()
                .result(result)
                .matchedRuleId(matchedRuleId)
                .matchedRuleName(matchedRuleName)
                .matchedRuleType(matchedRuleType)
                .evaluationDetails(List.of())
                .evaluationTimeMs(evaluationTime)
                .message(message)
                .build();
    }
}
