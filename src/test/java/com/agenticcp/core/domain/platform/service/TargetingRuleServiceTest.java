package com.agenticcp.core.domain.platform.service;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.platform.dto.targeting.CreateTargetRuleRequest;
import com.agenticcp.core.domain.platform.dto.targeting.TargetRuleResponse;
import com.agenticcp.core.domain.platform.enums.TargetingRuleErrorCode;
import com.agenticcp.core.domain.platform.entity.FeatureFlag;
import com.agenticcp.core.domain.platform.entity.FeatureFlagTargetRule;
import com.agenticcp.core.domain.platform.repository.FeatureFlagRepository;
import com.agenticcp.core.domain.platform.repository.FeatureFlagTargetRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * 타겟팅 규칙 서비스 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("타겟팅 규칙 서비스 테스트")
class TargetingRuleServiceTest {

    @Mock
    private FeatureFlagTargetRuleRepository targetingRuleRepository;

    @Mock
    private FeatureFlagRepository featureFlagRepository;

    @InjectMocks
    private TargetingRuleService targetingRuleService;

    private FeatureFlag testFeatureFlag;
    private FeatureFlagTargetRule testTargetRule;
    private CreateTargetRuleRequest testCreateRequest;

    @BeforeEach
    void setUp() {
        testFeatureFlag = FeatureFlag.builder()
                .flagKey("test-feature")
                .flagName("테스트 기능")
                .isEnabled(true)
                .build();
        testFeatureFlag.setId(1L);

        testTargetRule = FeatureFlagTargetRule.builder()
                .featureFlag(testFeatureFlag)
                .ruleName("테스트 규칙")
                .ruleDescription("테스트 규칙 설명")
                .ruleType(FeatureFlagTargetRule.RuleType.CLOUD_PROVIDER)
                .ruleCondition("{\"operator\":\"equals\"}")
                .ruleValue("[\"aws\",\"azure\"]")
                .priority(100)
                .isEnabled(true)
                .build();
        testTargetRule.setId(1L);
        testTargetRule.setCreatedAt(LocalDateTime.now());
        testTargetRule.setUpdatedAt(LocalDateTime.now());

        testCreateRequest = CreateTargetRuleRequest.builder()
                .ruleName("새로운 규칙")
                .ruleDescription("새로운 규칙 설명")
                .ruleType(FeatureFlagTargetRule.RuleType.CLOUD_PROVIDER)
                .ruleCondition("{\"operator\":\"equals\"}")
                .ruleValue("[\"aws\"]")
                .priority(50)
                .isEnabled(true)
                .build();
    }

    @Test
    @DisplayName("타겟팅 규칙 생성 성공")
    void createTargetingRule_Success() {
        // given
        given(featureFlagRepository.findByFlagKey("test-feature"))
                .willReturn(Optional.of(testFeatureFlag));
        given(targetingRuleRepository.existsByFeatureFlagIdAndRuleName(1L, "새로운 규칙", false))
                .willReturn(false);
        given(targetingRuleRepository.save(any(FeatureFlagTargetRule.class)))
                .willReturn(testTargetRule);

        // when
        TargetRuleResponse result = targetingRuleService.createTargetingRule("test-feature", testCreateRequest);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getRuleName()).isEqualTo("테스트 규칙");
        assertThat(result.getRuleType()).isEqualTo(FeatureFlagTargetRule.RuleType.CLOUD_PROVIDER);
        
        verify(featureFlagRepository).findByFlagKey("test-feature");
        verify(targetingRuleRepository).existsByFeatureFlagIdAndRuleName(1L, "새로운 규칙", false);
        verify(targetingRuleRepository).save(any(FeatureFlagTargetRule.class));
    }

    @Test
    @DisplayName("중복된 규칙 이름으로 인한 타겟팅 규칙 생성 실패")
    void createTargetingRule_DuplicateRuleName_Failure() {
        // given
        given(featureFlagRepository.findByFlagKey("test-feature"))
                .willReturn(Optional.of(testFeatureFlag));
        given(targetingRuleRepository.existsByFeatureFlagIdAndRuleName(1L, "새로운 규칙", false))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() -> targetingRuleService.createTargetingRule("test-feature", testCreateRequest))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", TargetingRuleErrorCode.TARGETING_RULE_NAME_DUPLICATE)
                .hasMessageContaining("이미 존재하는 규칙 이름입니다");
        
        verify(featureFlagRepository).findByFlagKey("test-feature");
        verify(targetingRuleRepository).existsByFeatureFlagIdAndRuleName(1L, "새로운 규칙", false);
        verify(targetingRuleRepository, never()).save(any(FeatureFlagTargetRule.class));
    }

    @Test
    @DisplayName("존재하지 않는 기능 플래그로 인한 타겟팅 규칙 생성 실패")
    void createTargetingRule_FeatureFlagNotFound_Failure() {
        // given
        given(featureFlagRepository.findByFlagKey("non-existent-feature"))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> targetingRuleService.createTargetingRule("non-existent-feature", testCreateRequest))
                .isInstanceOf(Exception.class);
        
        verify(featureFlagRepository).findByFlagKey("non-existent-feature");
        verify(targetingRuleRepository, never()).existsByFeatureFlagIdAndRuleName(any(), any(), any());
        verify(targetingRuleRepository, never()).save(any(FeatureFlagTargetRule.class));
    }

    @Test
    @DisplayName("타겟팅 규칙 목록 조회 성공")
    void getTargetingRules_Success() {
        // given
        given(featureFlagRepository.findByFlagKey("test-feature"))
                .willReturn(Optional.of(testFeatureFlag));
        given(targetingRuleRepository.findAllByFeatureFlagId(1L, false))
                .willReturn(List.of(testTargetRule));

        // when
        var result = targetingRuleService.getTargetingRules("test-feature");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getRules()).hasSize(1);
        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getActiveCount()).isEqualTo(1);
        assertThat(result.getInactiveCount()).isEqualTo(0);
        
        verify(featureFlagRepository).findByFlagKey("test-feature");
        verify(targetingRuleRepository).findAllByFeatureFlagId(1L, false);
    }

    @Test
    @DisplayName("타겟팅 규칙 활성화 성공")
    void activateTargetingRule_Success() {
        // given
        FeatureFlagTargetRule inactiveRule = FeatureFlagTargetRule.builder()
                .featureFlag(testFeatureFlag)
                .ruleName("비활성화된 규칙")
                .isEnabled(false)
                .build();
        inactiveRule.setId(1L);
        
        given(targetingRuleRepository.findByIdAndNotDeleted(1L, false))
                .willReturn(Optional.of(inactiveRule));
        given(targetingRuleRepository.save(any(FeatureFlagTargetRule.class)))
                .willReturn(inactiveRule);

        // when
        TargetRuleResponse result = targetingRuleService.activateTargetingRule(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        
        verify(targetingRuleRepository).findByIdAndNotDeleted(1L, false);
        verify(targetingRuleRepository).save(any(FeatureFlagTargetRule.class));
    }

    @Test
    @DisplayName("존재하지 않는 타겟팅 규칙 활성화 실패")
    void activateTargetingRule_RuleNotFound_Failure() {
        // given
        given(targetingRuleRepository.findByIdAndNotDeleted(999L, false))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> targetingRuleService.activateTargetingRule(999L))
                .isInstanceOf(Exception.class);
        
        verify(targetingRuleRepository).findByIdAndNotDeleted(999L, false);
        verify(targetingRuleRepository, never()).save(any(FeatureFlagTargetRule.class));
    }
}
