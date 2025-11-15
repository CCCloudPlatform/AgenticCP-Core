package com.agenticcp.core.domain.platform.service;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.platform.dto.targeting.TargetRuleEvaluationRequest;
import com.agenticcp.core.domain.platform.dto.targeting.TargetRuleEvaluationResponse;
import com.agenticcp.core.domain.platform.enums.MultiCloudEnvironment;
import com.agenticcp.core.domain.platform.enums.TargetingRuleErrorCode;
import com.agenticcp.core.domain.platform.entity.FeatureFlag;
import com.agenticcp.core.domain.platform.entity.FeatureFlagTargetRule;
import com.agenticcp.core.domain.platform.repository.FeatureFlagTargetRuleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
 * @since 2025-11-15
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("타겟팅 조건 평가기 테스트")
class TargetingConditionEvaluatorTest {

    @Mock
    private FeatureFlagTargetRuleRepository targetingRuleRepository;
    
    @Mock
    private MultiCloudEnvironmentService multiCloudEnvironmentService;

    private ObjectMapper objectMapper;
    private TargetingConditionEvaluator targetingConditionEvaluator;

    private FeatureFlag testFeatureFlag;
    private FeatureFlagTargetRule testCloudProviderRule;
    private TargetRuleEvaluationRequest testEvaluationRequest;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        targetingConditionEvaluator = new TargetingConditionEvaluator(
                targetingRuleRepository, objectMapper, multiCloudEnvironmentService);
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
    
    @Test
    @DisplayName("클라우드 환경 규칙 매칭 성공 - MULTI_CLOUD")
    void evaluateTargeting_CloudEnvironmentRule_MultiCloud_Match() {
        // given
        FeatureFlagTargetRule cloudEnvRule = FeatureFlagTargetRule.builder()
                .featureFlag(testFeatureFlag)
                .ruleName("멀티클라우드 전용")
                .ruleType(FeatureFlagTargetRule.RuleType.CLOUD_ENVIRONMENT)
                .ruleValue("[\"multi_cloud\", \"hybrid\"]")
                .priority(100)
                .isEnabled(true)
                .build();
        cloudEnvRule.setId(10L);
        
        given(targetingRuleRepository.findActiveRulesByFeatureFlagId(1L, false))
                .willReturn(List.of(cloudEnvRule));
        given(multiCloudEnvironmentService.detectEnvironment("tenant123"))
                .willReturn(MultiCloudEnvironment.MULTI_CLOUD);
        
        // when
        TargetRuleEvaluationResponse result = targetingConditionEvaluator.evaluateTargeting(
                testFeatureFlag, testEvaluationRequest);
        
        // then
        assertThat(result).isNotNull();
        assertThat(result.getResult()).isTrue();
        assertThat(result.getMatchedRuleId()).isEqualTo(10L);
        assertThat(result.getMatchedRuleName()).isEqualTo("멀티클라우드 전용");
        assertThat(result.getMatchedRuleType()).isEqualTo("CLOUD_ENVIRONMENT");
        
        verify(multiCloudEnvironmentService).detectEnvironment("tenant123");
        verify(targetingRuleRepository).findActiveRulesByFeatureFlagId(1L, false);
    }
    
    @Test
    @DisplayName("클라우드 환경 규칙 매칭 성공 - HYBRID")
    void evaluateTargeting_CloudEnvironmentRule_Hybrid_Match() {
        // given
        FeatureFlagTargetRule cloudEnvRule = FeatureFlagTargetRule.builder()
                .featureFlag(testFeatureFlag)
                .ruleName("하이브리드 환경 전용")
                .ruleType(FeatureFlagTargetRule.RuleType.CLOUD_ENVIRONMENT)
                .ruleValue("[\"multi_cloud\", \"hybrid\"]")
                .priority(100)
                .isEnabled(true)
                .build();
        cloudEnvRule.setId(11L);
        
        given(targetingRuleRepository.findActiveRulesByFeatureFlagId(1L, false))
                .willReturn(List.of(cloudEnvRule));
        given(multiCloudEnvironmentService.detectEnvironment("tenant123"))
                .willReturn(MultiCloudEnvironment.HYBRID);
        
        // when
        TargetRuleEvaluationResponse result = targetingConditionEvaluator.evaluateTargeting(
                testFeatureFlag, testEvaluationRequest);
        
        // then
        assertThat(result).isNotNull();
        assertThat(result.getResult()).isTrue();
        assertThat(result.getMatchedRuleId()).isEqualTo(11L);
        assertThat(result.getMatchedRuleName()).isEqualTo("하이브리드 환경 전용");
        
        verify(multiCloudEnvironmentService).detectEnvironment("tenant123");
    }
    
    @Test
    @DisplayName("클라우드 환경 규칙 매칭 실패 - SINGLE_CLOUD는 제외")
    void evaluateTargeting_CloudEnvironmentRule_SingleCloud_NoMatch() {
        // given
        FeatureFlagTargetRule cloudEnvRule = FeatureFlagTargetRule.builder()
                .featureFlag(testFeatureFlag)
                .ruleName("멀티클라우드 전용")
                .ruleType(FeatureFlagTargetRule.RuleType.CLOUD_ENVIRONMENT)
                .ruleValue("[\"multi_cloud\", \"hybrid\"]")
                .priority(100)
                .isEnabled(true)
                .build();
        cloudEnvRule.setId(12L);
        
        given(targetingRuleRepository.findActiveRulesByFeatureFlagId(1L, false))
                .willReturn(List.of(cloudEnvRule));
        given(multiCloudEnvironmentService.detectEnvironment("tenant123"))
                .willReturn(MultiCloudEnvironment.SINGLE_CLOUD); // 단일 클라우드
        
        // when
        TargetRuleEvaluationResponse result = targetingConditionEvaluator.evaluateTargeting(
                testFeatureFlag, testEvaluationRequest);
        
        // then
        assertThat(result).isNotNull();
        assertThat(result.getResult()).isFalse();
        assertThat(result.getMatchedRuleId()).isNull();
        assertThat(result.getEvaluationDetails()).hasSize(1);
        assertThat(result.getEvaluationDetails().get(0).getMatched()).isFalse();
        
        verify(multiCloudEnvironmentService).detectEnvironment("tenant123");
    }
    
    @Test
    @DisplayName("클라우드 환경 규칙 평가 - 모든 클라우드 환경 허용")
    void evaluateTargeting_CloudEnvironmentRule_AllEnvironments_Match() {
        // given
        FeatureFlagTargetRule cloudEnvRule = FeatureFlagTargetRule.builder()
                .featureFlag(testFeatureFlag)
                .ruleName("모든 클라우드 환경")
                .ruleType(FeatureFlagTargetRule.RuleType.CLOUD_ENVIRONMENT)
                .ruleValue("[\"single_cloud\", \"multi_cloud\", \"hybrid\", \"on_premise\"]")
                .priority(100)
                .isEnabled(true)
                .build();
        cloudEnvRule.setId(13L);
        
        given(targetingRuleRepository.findActiveRulesByFeatureFlagId(1L, false))
                .willReturn(List.of(cloudEnvRule));
        given(multiCloudEnvironmentService.detectEnvironment("tenant123"))
                .willReturn(MultiCloudEnvironment.SINGLE_CLOUD);
        
        // when
        TargetRuleEvaluationResponse result = targetingConditionEvaluator.evaluateTargeting(
                testFeatureFlag, testEvaluationRequest);
        
        // then
        assertThat(result).isNotNull();
        assertThat(result.getResult()).isTrue();
        assertThat(result.getMatchedRuleId()).isEqualTo(13L);
        
        verify(multiCloudEnvironmentService).detectEnvironment("tenant123");
    }
    
    @Test
    @DisplayName("클라우드 환경 규칙 평가 - tenantId 없음 시 예외 발생")
    void evaluateTargeting_CloudEnvironmentRule_NoTenantId_ThrowsException() {
        // given
        FeatureFlagTargetRule cloudEnvRule = FeatureFlagTargetRule.builder()
                .featureFlag(testFeatureFlag)
                .ruleName("멀티클라우드 전용")
                .ruleType(FeatureFlagTargetRule.RuleType.CLOUD_ENVIRONMENT)
                .ruleValue("[\"multi_cloud\"]")
                .priority(100)
                .isEnabled(true)
                .build();
        cloudEnvRule.setId(14L);
        
        TargetRuleEvaluationRequest requestWithoutTenantId = TargetRuleEvaluationRequest.builder()
                .userId("user123")
                .tenantId(null) // tenantId 없음
                .build();
        
        given(targetingRuleRepository.findActiveRulesByFeatureFlagId(1L, false))
                .willReturn(List.of(cloudEnvRule));
        
        // when & then
        assertThatThrownBy(() -> targetingConditionEvaluator.evaluateTargeting(
                testFeatureFlag, requestWithoutTenantId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", 
                        TargetingRuleErrorCode.TENANT_ID_REQUIRED_FOR_ENVIRONMENT_RULE);
        
        // multiCloudEnvironmentService는 호출되지 않아야 함
        verify(multiCloudEnvironmentService, never()).detectEnvironment(any());
    }
    
    @Test
    @DisplayName("클라우드 환경 규칙 평가 - 빈 ruleValue 배열 시 예외 발생")
    void evaluateTargeting_CloudEnvironmentRule_EmptyRuleValue_ThrowsException() {
        // given
        FeatureFlagTargetRule cloudEnvRule = FeatureFlagTargetRule.builder()
                .featureFlag(testFeatureFlag)
                .ruleName("빈 환경 규칙")
                .ruleType(FeatureFlagTargetRule.RuleType.CLOUD_ENVIRONMENT)
                .ruleValue("[]")  // 빈 배열
                .priority(100)
                .isEnabled(true)
                .build();
        cloudEnvRule.setId(15L);
        
        given(targetingRuleRepository.findActiveRulesByFeatureFlagId(1L, false))
                .willReturn(List.of(cloudEnvRule));
        given(multiCloudEnvironmentService.detectEnvironment("tenant123"))
                .willReturn(MultiCloudEnvironment.MULTI_CLOUD);
        
        // when & then
        assertThatThrownBy(() -> targetingConditionEvaluator.evaluateTargeting(
                testFeatureFlag, testEvaluationRequest))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", 
                        TargetingRuleErrorCode.RULE_VALUE_EMPTY_ARRAY);
        
        verify(multiCloudEnvironmentService).detectEnvironment("tenant123");
    }
    
    @Test
    @DisplayName("클라우드 환경 규칙 평가 - 환경 감지 실패 시 예외 발생")
    void evaluateTargeting_CloudEnvironmentRule_DetectionFailed_ThrowsException() {
        // given
        FeatureFlagTargetRule cloudEnvRule = FeatureFlagTargetRule.builder()
                .featureFlag(testFeatureFlag)
                .ruleName("멀티클라우드 전용")
                .ruleType(FeatureFlagTargetRule.RuleType.CLOUD_ENVIRONMENT)
                .ruleValue("[\"multi_cloud\"]")
                .priority(100)
                .isEnabled(true)
                .build();
        cloudEnvRule.setId(16L);
        
        given(targetingRuleRepository.findActiveRulesByFeatureFlagId(1L, false))
                .willReturn(List.of(cloudEnvRule));
        given(multiCloudEnvironmentService.detectEnvironment("tenant123"))
                .willThrow(new BusinessException(
                        TargetingRuleErrorCode.CLOUD_ENVIRONMENT_DETECTION_FAILED,
                        "환경 감지 실패"));
        
        // when & then
        assertThatThrownBy(() -> targetingConditionEvaluator.evaluateTargeting(
                testFeatureFlag, testEvaluationRequest))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", 
                        TargetingRuleErrorCode.CLOUD_ENVIRONMENT_DETECTION_FAILED);
        
        verify(multiCloudEnvironmentService).detectEnvironment("tenant123");
    }
}
