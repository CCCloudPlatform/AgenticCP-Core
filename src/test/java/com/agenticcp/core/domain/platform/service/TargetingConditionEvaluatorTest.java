package com.agenticcp.core.domain.platform.service;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.platform.dto.targeting.TargetRuleEvaluationRequest;
import com.agenticcp.core.domain.platform.dto.targeting.TargetRuleEvaluationResponse;
import com.agenticcp.core.domain.platform.enums.TargetingRuleErrorCode;
import com.agenticcp.core.domain.platform.entity.FeatureFlag;
import com.agenticcp.core.domain.platform.entity.FeatureFlagTargetRule;
import com.agenticcp.core.domain.platform.repository.FeatureFlagTargetRuleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * 타겟팅 조건 평가기 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("타겟팅 조건 평가기 테스트")
class TargetingConditionEvaluatorTest {

    @Mock
    private FeatureFlagTargetRuleRepository targetingRuleRepository;

    private ObjectMapper objectMapper;
    private TargetingConditionEvaluator targetingConditionEvaluator;

    private FeatureFlag testFeatureFlag;
    private FeatureFlagTargetRule testCloudProviderRule;
    private TargetRuleEvaluationRequest testEvaluationRequest;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        targetingConditionEvaluator = new TargetingConditionEvaluator(targetingRuleRepository, objectMapper);
        testFeatureFlag = FeatureFlag.builder()
                .flagKey("test-feature")
                .flagName("테스트 기능")
                .isEnabled(true)
                .startDate(null)
                .endDate(null)
                .rolloutPercentage(0)
                .build();
        testFeatureFlag.setId(1L);

        testCloudProviderRule = FeatureFlagTargetRule.builder()
                .featureFlag(testFeatureFlag)
                .ruleName("AWS 프로바이더 규칙")
                .ruleType(FeatureFlagTargetRule.RuleType.CLOUD_PROVIDER)
                .ruleCondition("{\"operator\":\"equals\"}")
                .ruleValue("[\"aws\",\"azure\"]")
                .priority(100)
                .isEnabled(true)
                .build();
        testCloudProviderRule.setId(1L);

        testEvaluationRequest = TargetRuleEvaluationRequest.builder()
                .userId("user123")
                .tenantId("tenant123")
                .cloudProvider("aws")
                .region("us-east-1")
                .tenantType("enterprise")
                .tenantTier("premium")
                .userRole("admin")
                .userAttributes(Map.of("department", "engineering"))
                .customAttributes(Map.of("beta_tester", true))
                .build();
    }

    @Test
    @DisplayName("클라우드 프로바이더 규칙 매칭 성공")
    void evaluateTargeting_CloudProviderRuleMatch_Success() {
        // given
        given(targetingRuleRepository.findActiveRulesByFeatureFlagId(1L, false))
                .willReturn(List.of(testCloudProviderRule));

        // when
        TargetRuleEvaluationResponse result = targetingConditionEvaluator.evaluateTargeting(
                testFeatureFlag, testEvaluationRequest);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getResult()).isTrue();
        assertThat(result.getMatchedRuleId()).isEqualTo(1L);
        assertThat(result.getMatchedRuleName()).isEqualTo("AWS 프로바이더 규칙");
        assertThat(result.getMatchedRuleType()).isEqualTo("CLOUD_PROVIDER");
        assertThat(result.getEvaluationDetails()).hasSize(1);
        
        verify(targetingRuleRepository).findActiveRulesByFeatureFlagId(1L, false);
    }

    @Test
    @DisplayName("클라우드 프로바이더 규칙 매칭 실패")
    void evaluateTargeting_CloudProviderRuleNoMatch_Failure() {
        // given
        TargetRuleEvaluationRequest requestWithGCP = TargetRuleEvaluationRequest.builder()
                .userId("user123")
                .tenantId("tenant123")
                .cloudProvider("gcp") // GCP는 규칙에 없음
                .build();
        
        given(targetingRuleRepository.findActiveRulesByFeatureFlagId(1L, false))
                .willReturn(List.of(testCloudProviderRule));

        // when
        TargetRuleEvaluationResponse result = targetingConditionEvaluator.evaluateTargeting(
                testFeatureFlag, requestWithGCP);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getResult()).isFalse();
        assertThat(result.getMatchedRuleId()).isNull();
        assertThat(result.getEvaluationDetails()).hasSize(1);
        assertThat(result.getEvaluationDetails().get(0).getMatched()).isFalse();
        
        verify(targetingRuleRepository).findActiveRulesByFeatureFlagId(1L, false);
    }

    @Test
    @DisplayName("비활성화된 기능 플래그 평가")
    void evaluateTargeting_DisabledFeatureFlag_Failure() {
        // given
        FeatureFlag disabledFlag = FeatureFlag.builder()
                .flagKey("disabled-feature")
                .isEnabled(false) // 비활성화됨
                .build();
        disabledFlag.setId(1L);

        // when & then
        assertThatThrownBy(() -> targetingConditionEvaluator.evaluateTargeting(disabledFlag, testEvaluationRequest))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", TargetingRuleErrorCode.FEATURE_FLAG_DISABLED);
        
        verify(targetingRuleRepository, never()).findActiveRulesByFeatureFlagId(any(), any());
    }

    @Test
    @DisplayName("유효 기간을 벗어난 기능 플래그 평가")
    void evaluateTargeting_ExpiredFeatureFlag_Failure() {
        // given
        FeatureFlag expiredFlag = FeatureFlag.builder()
                .flagKey("expired-feature")
                .isEnabled(true)
                .startDate(LocalDateTime.now().minusDays(10))
                .endDate(LocalDateTime.now().minusDays(1)) // 어제 만료
                .build();
        expiredFlag.setId(1L);

        // when & then
        assertThatThrownBy(() -> targetingConditionEvaluator.evaluateTargeting(expiredFlag, testEvaluationRequest))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", TargetingRuleErrorCode.FEATURE_FLAG_EXPIRED);
        
        verify(targetingRuleRepository, never()).findActiveRulesByFeatureFlagId(any(), any());
    }

    @Test
    @DisplayName("타겟팅 규칙이 없는 경우 롤아웃 비율로 평가")
    void evaluateTargeting_NoTargetingRules_UsesRolloutPercentage() {
        // given
        FeatureFlag flagWithRollout = FeatureFlag.builder()
                .flagKey("rollout-feature")
                .isEnabled(true)
                .rolloutPercentage(50) // 50% 롤아웃
                .build();
        flagWithRollout.setId(1L);
        
        given(targetingRuleRepository.findActiveRulesByFeatureFlagId(1L, false))
                .willReturn(List.of()); // 타겟팅 규칙 없음

        // when
        TargetRuleEvaluationResponse result = targetingConditionEvaluator.evaluateTargeting(
                flagWithRollout, testEvaluationRequest);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getMessage()).contains("타겟팅 규칙이 없어 롤아웃 비율로 평가했습니다");
        
        verify(targetingRuleRepository).findActiveRulesByFeatureFlagId(1L, false);
    }

    @Test
    @DisplayName("우선순위가 높은 규칙이 먼저 평가됨")
    void evaluateTargeting_PriorityOrder_Correct() {
        // given
        FeatureFlagTargetRule lowPriorityRule = FeatureFlagTargetRule.builder()
                .ruleName("낮은 우선순위 규칙")
                .ruleType(FeatureFlagTargetRule.RuleType.CLOUD_PROVIDER)
                .ruleValue("[\"gcp\"]") // GCP만 매칭
                .priority(50) // 낮은 우선순위
                .isEnabled(true)
                .build();
        lowPriorityRule.setId(2L);
        
        FeatureFlagTargetRule highPriorityRule = FeatureFlagTargetRule.builder()
                .ruleName("높은 우선순위 규칙")
                .ruleType(FeatureFlagTargetRule.RuleType.CLOUD_PROVIDER)
                .ruleValue("[\"aws\"]") // AWS 매칭
                .priority(100) // 높은 우선순위
                .isEnabled(true)
                .build();
        highPriorityRule.setId(3L);

        given(targetingRuleRepository.findActiveRulesByFeatureFlagId(1L, false))
                .willReturn(List.of(lowPriorityRule, highPriorityRule)); // 낮은 우선순위가 먼저

        // when
        TargetRuleEvaluationResponse result = targetingConditionEvaluator.evaluateTargeting(
                testFeatureFlag, testEvaluationRequest); // AWS 사용자

        // then
        assertThat(result).isNotNull();
        assertThat(result.getResult()).isTrue();
        assertThat(result.getMatchedRuleId()).isEqualTo(3L); // 높은 우선순위 규칙이 매칭됨
        assertThat(result.getMatchedRuleName()).isEqualTo("높은 우선순위 규칙");
        
        verify(targetingRuleRepository).findActiveRulesByFeatureFlagId(1L, false);
    }
}
