package com.agenticcp.core.domain.security.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PolicyDecision 열거형 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@DisplayName("PolicyDecision 테스트")
class PolicyDecisionTest {
    
    @Nested
    @DisplayName("기본 기능 테스트")
    class BasicFunctionalityTest {
        
        @Test
        @DisplayName("정책 결정 코드 반환")
        void getCode_ReturnsCorrectCode() {
            // Given & When & Then
            assertThat(PolicyDecision.ALLOW.getCode()).isEqualTo("ALLOW");
            assertThat(PolicyDecision.DENY.getCode()).isEqualTo("DENY");
            assertThat(PolicyDecision.INCONCLUSIVE.getCode()).isEqualTo("INCONCLUSIVE");
        }
        
        @Test
        @DisplayName("정책 결정 설명 반환")
        void getDescription_ReturnsCorrectDescription() {
            // Given & When & Then
            assertThat(PolicyDecision.ALLOW.getDescription()).isEqualTo("접근 허용");
            assertThat(PolicyDecision.DENY.getDescription()).isEqualTo("접근 거부");
            assertThat(PolicyDecision.INCONCLUSIVE.getDescription()).isEqualTo("결정할 수 없음");
        }
        
        @Test
        @DisplayName("toString 메서드 테스트")
        void toString_ReturnsFormattedString() {
            // Given & When & Then
            assertThat(PolicyDecision.ALLOW.toString()).isEqualTo("ALLOW (접근 허용)");
            assertThat(PolicyDecision.DENY.toString()).isEqualTo("DENY (접근 거부)");
            assertThat(PolicyDecision.INCONCLUSIVE.toString()).isEqualTo("INCONCLUSIVE (결정할 수 없음)");
        }
    }
    
    @Nested
    @DisplayName("코드로부터 PolicyDecision 찾기 테스트")
    class FromCodeTest {
        
        @Test
        @DisplayName("유효한 코드로 PolicyDecision 찾기")
        void fromCode_ValidCode_ReturnsCorrectDecision() {
            // Given & When & Then
            assertThat(PolicyDecision.fromCode("ALLOW")).isEqualTo(PolicyDecision.ALLOW);
            assertThat(PolicyDecision.fromCode("DENY")).isEqualTo(PolicyDecision.DENY);
            assertThat(PolicyDecision.fromCode("INCONCLUSIVE")).isEqualTo(PolicyDecision.INCONCLUSIVE);
        }
        
        @Test
        @DisplayName("대소문자 구분 없이 PolicyDecision 찾기")
        void fromCode_CaseInsensitive_ReturnsCorrectDecision() {
            // Given & When & Then
            assertThat(PolicyDecision.fromCode("allow")).isEqualTo(PolicyDecision.ALLOW);
            assertThat(PolicyDecision.fromCode("deny")).isEqualTo(PolicyDecision.DENY);
            assertThat(PolicyDecision.fromCode("inconclusive")).isEqualTo(PolicyDecision.INCONCLUSIVE);
        }
        
        @Test
        @DisplayName("null 코드로 PolicyDecision 찾기")
        void fromCode_NullCode_ReturnsNull() {
            // Given & When & Then
            assertThat(PolicyDecision.fromCode(null)).isNull();
        }
        
        @Test
        @DisplayName("유효하지 않은 코드로 PolicyDecision 찾기")
        void fromCode_InvalidCode_ReturnsNull() {
            // Given & When & Then
            assertThat(PolicyDecision.fromCode("INVALID")).isNull();
            assertThat(PolicyDecision.fromCode("")).isNull();
        }
    }
    
    @Nested
    @DisplayName("정책 결정 타입 확인 테스트")
    class DecisionTypeTest {
        
        @Test
        @DisplayName("ALLOW 타입 확인")
        void isAllow_AllowDecision_ReturnsTrue() {
            // Given & When & Then
            assertThat(PolicyDecision.ALLOW.isAllow()).isTrue();
            assertThat(PolicyDecision.DENY.isAllow()).isFalse();
            assertThat(PolicyDecision.INCONCLUSIVE.isAllow()).isFalse();
        }
        
        @Test
        @DisplayName("DENY 타입 확인")
        void isDeny_DenyDecision_ReturnsTrue() {
            // Given & When & Then
            assertThat(PolicyDecision.DENY.isDeny()).isTrue();
            assertThat(PolicyDecision.ALLOW.isDeny()).isFalse();
            assertThat(PolicyDecision.INCONCLUSIVE.isDeny()).isFalse();
        }
        
        @Test
        @DisplayName("INCONCLUSIVE 타입 확인")
        void isInconclusive_InconclusiveDecision_ReturnsTrue() {
            // Given & When & Then
            assertThat(PolicyDecision.INCONCLUSIVE.isInconclusive()).isTrue();
            assertThat(PolicyDecision.ALLOW.isInconclusive()).isFalse();
            assertThat(PolicyDecision.DENY.isInconclusive()).isFalse();
        }
    }
}
