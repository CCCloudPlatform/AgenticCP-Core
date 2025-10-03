package com.agenticcp.core.domain.security.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PolicyDecision enum 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@DisplayName("PolicyDecision Enum 테스트")
class PolicyDecisionTest {
    
    @Nested
    @DisplayName("코드 변환 테스트")
    class FromCodeTest {
        
        @Test
        @DisplayName("ALLOW 코드로 변환")
        void fromCode_Allow_ReturnsAllow() {
            // When
            PolicyDecision result = PolicyDecision.fromCode("ALLOW");
            
            // Then
            assertThat(result).isEqualTo(PolicyDecision.ALLOW);
        }
        
        @Test
        @DisplayName("DENY 코드로 변환")
        void fromCode_Deny_ReturnsDeny() {
            // When
            PolicyDecision result = PolicyDecision.fromCode("DENY");
            
            // Then
            assertThat(result).isEqualTo(PolicyDecision.DENY);
        }
        
        @Test
        @DisplayName("INCONCLUSIVE 코드로 변환")
        void fromCode_Inconclusive_ReturnsInconclusive() {
            // When
            PolicyDecision result = PolicyDecision.fromCode("INCONCLUSIVE");
            
            // Then
            assertThat(result).isEqualTo(PolicyDecision.INCONCLUSIVE);
        }
        
        @Test
        @DisplayName("소문자 코드로 변환")
        void fromCode_LowerCase_ReturnsCorrectDecision() {
            // When
            PolicyDecision allowResult = PolicyDecision.fromCode("allow");
            PolicyDecision denyResult = PolicyDecision.fromCode("deny");
            PolicyDecision inconclusiveResult = PolicyDecision.fromCode("inconclusive");
            
            // Then
            assertThat(allowResult).isEqualTo(PolicyDecision.ALLOW);
            assertThat(denyResult).isEqualTo(PolicyDecision.DENY);
            assertThat(inconclusiveResult).isEqualTo(PolicyDecision.INCONCLUSIVE);
        }
        
        @Test
        @DisplayName("null 코드로 변환")
        void fromCode_Null_ReturnsNull() {
            // When
            PolicyDecision result = PolicyDecision.fromCode(null);
            
            // Then
            assertThat(result).isNull();
        }
        
        @Test
        @DisplayName("잘못된 코드로 변환")
        void fromCode_InvalidCode_ReturnsNull() {
            // When
            PolicyDecision result = PolicyDecision.fromCode("INVALID");
            
            // Then
            assertThat(result).isNull();
        }
    }
    
    @Nested
    @DisplayName("상태 확인 테스트")
    class StatusCheckTest {
        
        @Test
        @DisplayName("ALLOW 상태 확인")
        void isAllow_AllowDecision_ReturnsTrue() {
            // Given
            PolicyDecision decision = PolicyDecision.ALLOW;
            
            // When & Then
            assertThat(decision.isAllow()).isTrue();
            assertThat(decision.isDeny()).isFalse();
            assertThat(decision.isInconclusive()).isFalse();
        }
        
        @Test
        @DisplayName("DENY 상태 확인")
        void isDeny_DenyDecision_ReturnsTrue() {
            // Given
            PolicyDecision decision = PolicyDecision.DENY;
            
            // When & Then
            assertThat(decision.isAllow()).isFalse();
            assertThat(decision.isDeny()).isTrue();
            assertThat(decision.isInconclusive()).isFalse();
        }
        
        @Test
        @DisplayName("INCONCLUSIVE 상태 확인")
        void isInconclusive_InconclusiveDecision_ReturnsTrue() {
            // Given
            PolicyDecision decision = PolicyDecision.INCONCLUSIVE;
            
            // When & Then
            assertThat(decision.isAllow()).isFalse();
            assertThat(decision.isDeny()).isFalse();
            assertThat(decision.isInconclusive()).isTrue();
        }
    }
    
    @Nested
    @DisplayName("속성 테스트")
    class AttributeTest {
        
        @Test
        @DisplayName("ALLOW 속성")
        void attributes_Allow_ReturnsCorrectValues() {
            // Given
            PolicyDecision decision = PolicyDecision.ALLOW;
            
            // When & Then
            assertThat(decision.getCode()).isEqualTo("ALLOW");
            assertThat(decision.getDescription()).isEqualTo("접근 허용");
        }
        
        @Test
        @DisplayName("DENY 속성")
        void attributes_Deny_ReturnsCorrectValues() {
            // Given
            PolicyDecision decision = PolicyDecision.DENY;
            
            // When & Then
            assertThat(decision.getCode()).isEqualTo("DENY");
            assertThat(decision.getDescription()).isEqualTo("접근 거부");
        }
        
        @Test
        @DisplayName("INCONCLUSIVE 속성")
        void attributes_Inconclusive_ReturnsCorrectValues() {
            // Given
            PolicyDecision decision = PolicyDecision.INCONCLUSIVE;
            
            // When & Then
            assertThat(decision.getCode()).isEqualTo("INCONCLUSIVE");
            assertThat(decision.getDescription()).isEqualTo("결정할 수 없음");
        }
        
        @Test
        @DisplayName("toString 테스트")
        void toString_ReturnsFormattedString() {
            // Given
            PolicyDecision allow = PolicyDecision.ALLOW;
            PolicyDecision deny = PolicyDecision.DENY;
            PolicyDecision inconclusive = PolicyDecision.INCONCLUSIVE;
            
            // When & Then
            assertThat(allow.toString()).isEqualTo("ALLOW (접근 허용)");
            assertThat(deny.toString()).isEqualTo("DENY (접근 거부)");
            assertThat(inconclusive.toString()).isEqualTo("INCONCLUSIVE (결정할 수 없음)");
        }
    }
    
    @Nested
    @DisplayName("모든 enum 값 테스트")
    class AllValuesTest {
        
        @Test
        @DisplayName("모든 enum 값 존재")
        void values_ReturnsAllDecisions() {
            // When
            PolicyDecision[] values = PolicyDecision.values();
            
            // Then
            assertThat(values).hasSize(3);
            assertThat(values).containsExactlyInAnyOrder(
                PolicyDecision.ALLOW,
                PolicyDecision.DENY,
                PolicyDecision.INCONCLUSIVE
            );
        }
    }
}
