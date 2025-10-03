package com.agenticcp.core.domain.security.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PolicyRules DTO 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@DisplayName("PolicyRules DTO 테스트")
class PolicyRulesTest {
    
    @Nested
    @DisplayName("빌더 테스트")
    class BuilderTest {
        
        @Test
        @DisplayName("기본 필드로 객체 생성")
        void builder_BasicFields_CreatesObject() {
            // Given
            List<PolicyRule> rules = List.of(
                    PolicyRule.builder()
                            .ruleId("rule-1")
                            .action("ALLOW")
                            .build()
            );
            
            // When
            PolicyRules policyRules = PolicyRules.builder()
                    .defaultAction("DENY")
                    .rules(rules)
                    .build();
            
            // Then
            assertThat(policyRules.getDefaultAction()).isEqualTo("DENY");
            assertThat(policyRules.getRules()).isEqualTo(rules);
            assertThat(policyRules.getEvaluationMode()).isEqualTo(PolicyRules.RuleEvaluationMode.FIRST); // 기본값
        }
        
        @Test
        @DisplayName("모든 필드로 객체 생성")
        void builder_AllFields_CreatesCompleteObject() {
            // Given
            List<PolicyRule> rules = List.of(
                    PolicyRule.builder().ruleId("rule-1").action("ALLOW").build(),
                    PolicyRule.builder().ruleId("rule-2").action("DENY").build()
            );
            
            // When
            PolicyRules policyRules = PolicyRules.builder()
                    .defaultAction("INCONCLUSIVE")
                    .rules(rules)
                    .evaluationMode(PolicyRules.RuleEvaluationMode.ALL)
                    .build();
            
            // Then
            assertThat(policyRules.getDefaultAction()).isEqualTo("INCONCLUSIVE");
            assertThat(policyRules.getRules()).isEqualTo(rules);
            assertThat(policyRules.getEvaluationMode()).isEqualTo(PolicyRules.RuleEvaluationMode.ALL);
        }
    }
    
    @Nested
    @DisplayName("비어있음 확인 테스트")
    class EmptyCheckTest {
        
        @Test
        @DisplayName("규칙이 없으면 비어있음")
        void isEmpty_NoRules_ReturnsTrue() {
            // Given
            PolicyRules policyRules = PolicyRules.builder()
                    .rules(null)
                    .build();
            
            // When
            boolean result = policyRules.isEmpty();
            
            // Then
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("빈 규칙 리스트면 비어있음")
        void isEmpty_EmptyRulesList_ReturnsTrue() {
            // Given
            PolicyRules policyRules = PolicyRules.builder()
                    .rules(new ArrayList<>())
                    .build();
            
            // When
            boolean result = policyRules.isEmpty();
            
            // Then
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("규칙이 있으면 비어있지 않음")
        void isEmpty_HasRules_ReturnsFalse() {
            // Given
            PolicyRules policyRules = PolicyRules.builder()
                    .rules(List.of(
                            PolicyRule.builder().ruleId("rule-1").build()
                    ))
                    .build();
            
            // When
            boolean result = policyRules.isEmpty();
            
            // Then
            assertThat(result).isFalse();
        }
    }
    
    @Nested
    @DisplayName("규칙 개수 확인 테스트")
    class RuleCountTest {
        
        @Test
        @DisplayName("규칙이 없으면 개수 0")
        void getRuleCount_NoRules_ReturnsZero() {
            // Given
            PolicyRules policyRules = PolicyRules.builder()
                    .rules(null)
                    .build();
            
            // When
            int count = policyRules.getRuleCount();
            
            // Then
            assertThat(count).isZero();
        }
        
        @Test
        @DisplayName("규칙 개수 반환")
        void getRuleCount_HasRules_ReturnsCount() {
            // Given
            PolicyRules policyRules = PolicyRules.builder()
                    .rules(List.of(
                            PolicyRule.builder().ruleId("rule-1").build(),
                            PolicyRule.builder().ruleId("rule-2").build(),
                            PolicyRule.builder().ruleId("rule-3").build()
                    ))
                    .build();
            
            // When
            int count = policyRules.getRuleCount();
            
            // Then
            assertThat(count).isEqualTo(3);
        }
    }
    
    @Nested
    @DisplayName("액션별 규칙 조회 테스트")
    class GetRulesByActionTest {
        
        @Test
        @DisplayName("특정 액션을 가진 규칙들 조회")
        void getRulesByAction_MatchingAction_ReturnsMatchingRules() {
            // Given
            PolicyRule allowRule1 = PolicyRule.builder().ruleId("rule-1").action("ALLOW").build();
            PolicyRule denyRule = PolicyRule.builder().ruleId("rule-2").action("DENY").build();
            PolicyRule allowRule2 = PolicyRule.builder().ruleId("rule-3").action("ALLOW").build();
            
            PolicyRules policyRules = PolicyRules.builder()
                    .rules(List.of(allowRule1, denyRule, allowRule2))
                    .build();
            
            // When
            List<PolicyRule> result = policyRules.getRulesByAction("ALLOW");
            
            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactly(allowRule1, allowRule2);
        }
        
        @Test
        @DisplayName("매칭되는 규칙이 없으면 빈 리스트")
        void getRulesByAction_NoMatchingAction_ReturnsEmptyList() {
            // Given
            PolicyRules policyRules = PolicyRules.builder()
                    .rules(List.of(
                            PolicyRule.builder().ruleId("rule-1").action("ALLOW").build()
                    ))
                    .build();
            
            // When
            List<PolicyRule> result = policyRules.getRulesByAction("DENY");
            
            // Then
            assertThat(result).isEmpty();
        }
        
        @Test
        @DisplayName("규칙이 없으면 빈 리스트")
        void getRulesByAction_NoRules_ReturnsEmptyList() {
            // Given
            PolicyRules policyRules = PolicyRules.builder()
                    .rules(null)
                    .build();
            
            // When
            List<PolicyRule> result = policyRules.getRulesByAction("ALLOW");
            
            // Then
            assertThat(result).isEmpty();
        }
    }
    
    @Nested
    @DisplayName("평가 모드 테스트")
    class EvaluationModeTest {
        
        @Test
        @DisplayName("ALL 모드 - 모든 규칙 만족")
        void evaluationMode_All_HasCorrectDescription() {
            // Given
            PolicyRules.RuleEvaluationMode mode = PolicyRules.RuleEvaluationMode.ALL;
            
            // When & Then
            assertThat(mode.getDescription()).isEqualTo("모든 규칙 만족");
        }
        
        @Test
        @DisplayName("ANY 모드 - 하나의 규칙 만족")
        void evaluationMode_Any_HasCorrectDescription() {
            // Given
            PolicyRules.RuleEvaluationMode mode = PolicyRules.RuleEvaluationMode.ANY;
            
            // When & Then
            assertThat(mode.getDescription()).isEqualTo("하나의 규칙 만족");
        }
        
        @Test
        @DisplayName("FIRST 모드 - 첫 번째 규칙 적용")
        void evaluationMode_First_HasCorrectDescription() {
            // Given
            PolicyRules.RuleEvaluationMode mode = PolicyRules.RuleEvaluationMode.FIRST;
            
            // When & Then
            assertThat(mode.getDescription()).isEqualTo("첫 번째 규칙 적용");
        }
    }
}

