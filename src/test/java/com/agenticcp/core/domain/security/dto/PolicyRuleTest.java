package com.agenticcp.core.domain.security.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PolicyRule DTO 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@DisplayName("PolicyRule DTO 테스트")
class PolicyRuleTest {
    
    @Nested
    @DisplayName("빌더 테스트")
    class BuilderTest {
        
        @Test
        @DisplayName("기본 필드로 객체 생성")
        void builder_BasicFields_CreatesObject() {
            // When
            PolicyRule rule = PolicyRule.builder()
                    .ruleId("rule-001")
                    .ruleName("관리자 규칙")
                    .description("관리자만 접근 가능")
                    .condition("user.role == 'ADMIN'")
                    .action("ALLOW")
                    .build();
            
            // Then
            assertThat(rule.getRuleId()).isEqualTo("rule-001");
            assertThat(rule.getRuleName()).isEqualTo("관리자 규칙");
            assertThat(rule.getDescription()).isEqualTo("관리자만 접근 가능");
            assertThat(rule.getCondition()).isEqualTo("user.role == 'ADMIN'");
            assertThat(rule.getAction()).isEqualTo("ALLOW");
            assertThat(rule.getPriority()).isEqualTo(0); // 기본값
            assertThat(rule.getEnabled()).isTrue(); // 기본값
        }
        
        @Test
        @DisplayName("모든 필드로 객체 생성")
        void builder_AllFields_CreatesCompleteObject() {
            // Given
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("timeout", 300);
            
            List<String> tags = List.of("security", "admin");
            
            // When
            PolicyRule rule = PolicyRule.builder()
                    .ruleId("rule-002")
                    .ruleName("복잡한 규칙")
                    .description("모든 필드가 있는 규칙")
                    .condition("resource.type == 'EC2'")
                    .action("DENY")
                    .priority(100)
                    .enabled(false)
                    .parameters(parameters)
                    .tags(tags)
                    .createdAt("2024-01-01T00:00:00")
                    .updatedAt("2024-01-02T00:00:00")
                    .version("2.0")
                    .build();
            
            // Then
            assertThat(rule.getRuleId()).isEqualTo("rule-002");
            assertThat(rule.getRuleName()).isEqualTo("복잡한 규칙");
            assertThat(rule.getDescription()).isEqualTo("모든 필드가 있는 규칙");
            assertThat(rule.getCondition()).isEqualTo("resource.type == 'EC2'");
            assertThat(rule.getAction()).isEqualTo("DENY");
            assertThat(rule.getPriority()).isEqualTo(100);
            assertThat(rule.getEnabled()).isFalse();
            assertThat(rule.getParameters()).isEqualTo(parameters);
            assertThat(rule.getTags()).isEqualTo(tags);
            assertThat(rule.getCreatedAt()).isEqualTo("2024-01-01T00:00:00");
            assertThat(rule.getUpdatedAt()).isEqualTo("2024-01-02T00:00:00");
            assertThat(rule.getVersion()).isEqualTo("2.0");
        }
    }
    
    @Nested
    @DisplayName("활성화 상태 확인 테스트")
    class EnabledStateTest {
        
        @Test
        @DisplayName("enabled가 true면 활성화")
        void isEnabled_TrueValue_ReturnsTrue() {
            // Given
            PolicyRule rule = PolicyRule.builder()
                    .enabled(true)
                    .build();
            
            // When
            boolean result = rule.isEnabled();
            
            // Then
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("enabled가 false면 비활성화")
        void isEnabled_FalseValue_ReturnsFalse() {
            // Given
            PolicyRule rule = PolicyRule.builder()
                    .enabled(false)
                    .build();
            
            // When
            boolean result = rule.isEnabled();
            
            // Then
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("enabled가 null이면 비활성화")
        void isEnabled_NullValue_ReturnsFalse() {
            // Given
            PolicyRule rule = PolicyRule.builder()
                    .enabled(null)
                    .build();
            
            // When
            boolean result = rule.isEnabled();
            
            // Then
            assertThat(result).isFalse();
        }
    }
    
    @Nested
    @DisplayName("파라미터 관리 테스트")
    class ParameterManagementTest {
        
        @Test
        @DisplayName("파라미터 값 가져오기")
        void getParameter_ExistingKey_ReturnsValue() {
            // Given
            PolicyRule rule = PolicyRule.builder().build();
            rule.setParameter("timeout", 300);
            
            // When
            Object result = rule.getParameter("timeout");
            
            // Then
            assertThat(result).isEqualTo(300);
        }
        
        @Test
        @DisplayName("존재하지 않는 파라미터 값 가져오기")
        void getParameter_NonExistingKey_ReturnsNull() {
            // Given
            PolicyRule rule = PolicyRule.builder().build();
            
            // When
            Object result = rule.getParameter("nonExisting");
            
            // Then
            assertThat(result).isNull();
        }
        
        @Test
        @DisplayName("null 파라미터에서 값 가져오기")
        void getParameter_NullParameters_ReturnsNull() {
            // Given
            PolicyRule rule = PolicyRule.builder()
                    .parameters(null)
                    .build();
            
            // When
            Object result = rule.getParameter("anyKey");
            
            // Then
            assertThat(result).isNull();
        }
        
        @Test
        @DisplayName("파라미터 값 설정")
        void setParameter_NewValue_SetsValue() {
            // Given
            PolicyRule rule = PolicyRule.builder().build();
            
            // When
            rule.setParameter("maxRetries", 3);
            
            // Then
            assertThat(rule.getParameter("maxRetries")).isEqualTo(3);
        }
        
        @Test
        @DisplayName("null 파라미터에 값 설정")
        void setParameter_NullParameters_CreatesMapAndSetsValue() {
            // Given
            PolicyRule rule = PolicyRule.builder()
                    .parameters(null)
                    .build();
            
            // When
            rule.setParameter("key", "value");
            
            // Then
            assertThat(rule.getParameters()).isNotNull();
            assertThat(rule.getParameter("key")).isEqualTo("value");
        }
    }
    
    @Nested
    @DisplayName("태그 관리 테스트")
    class TagManagementTest {
        
        @Test
        @DisplayName("태그 존재 확인")
        void hasTag_ExistingTag_ReturnsTrue() {
            // Given
            PolicyRule rule = PolicyRule.builder()
                    .tags(List.of("security", "admin"))
                    .build();
            
            // When
            boolean result = rule.hasTag("security");
            
            // Then
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("존재하지 않는 태그 확인")
        void hasTag_NonExistingTag_ReturnsFalse() {
            // Given
            PolicyRule rule = PolicyRule.builder()
                    .tags(List.of("security"))
                    .build();
            
            // When
            boolean result = rule.hasTag("admin");
            
            // Then
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("null 태그 목록에서 태그 확인")
        void hasTag_NullTags_ReturnsFalse() {
            // Given
            PolicyRule rule = PolicyRule.builder()
                    .tags(null)
                    .build();
            
            // When
            boolean result = rule.hasTag("anyTag");
            
            // Then
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("태그 추가")
        void addTag_NewTag_AddsToList() {
            // Given
            PolicyRule rule = PolicyRule.builder().build();
            
            // When
            rule.addTag("newTag");
            
            // Then
            assertThat(rule.getTags()).contains("newTag");
        }
        
        @Test
        @DisplayName("중복 태그 추가 방지")
        void addTag_DuplicateTag_DoesNotAddDuplicate() {
            // Given
            PolicyRule rule = PolicyRule.builder().build();
            rule.addTag("security");
            
            // When
            rule.addTag("security");
            
            // Then
            assertThat(rule.getTags()).hasSize(1);
            assertThat(rule.getTags()).containsExactly("security");
        }
        
        @Test
        @DisplayName("null 태그 목록에 태그 추가")
        void addTag_NullTags_CreatesListAndAdds() {
            // Given
            PolicyRule rule = PolicyRule.builder()
                    .tags(null)
                    .build();
            
            // When
            rule.addTag("newTag");
            
            // Then
            assertThat(rule.getTags()).isNotNull();
            assertThat(rule.getTags()).contains("newTag");
        }
    }
    
    @Nested
    @DisplayName("액션 타입 확인 테스트")
    class ActionTypeTest {
        
        @Test
        @DisplayName("ALLOW 액션 확인")
        void isAllowAction_AllowAction_ReturnsTrue() {
            // Given
            PolicyRule rule = PolicyRule.builder()
                    .action("ALLOW")
                    .build();
            
            // When
            boolean result = rule.isAllowAction();
            
            // Then
            assertThat(result).isTrue();
            assertThat(rule.isDenyAction()).isFalse();
        }
        
        @Test
        @DisplayName("소문자 ALLOW 액션 확인")
        void isAllowAction_LowerCaseAllow_ReturnsTrue() {
            // Given
            PolicyRule rule = PolicyRule.builder()
                    .action("allow")
                    .build();
            
            // When
            boolean result = rule.isAllowAction();
            
            // Then
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("DENY 액션 확인")
        void isDenyAction_DenyAction_ReturnsTrue() {
            // Given
            PolicyRule rule = PolicyRule.builder()
                    .action("DENY")
                    .build();
            
            // When
            boolean result = rule.isDenyAction();
            
            // Then
            assertThat(result).isTrue();
            assertThat(rule.isAllowAction()).isFalse();
        }
        
        @Test
        @DisplayName("소문자 DENY 액션 확인")
        void isDenyAction_LowerCaseDeny_ReturnsTrue() {
            // Given
            PolicyRule rule = PolicyRule.builder()
                    .action("deny")
                    .build();
            
            // When
            boolean result = rule.isDenyAction();
            
            // Then
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("null 액션 확인")
        void isAllowAction_NullAction_ReturnsFalse() {
            // Given
            PolicyRule rule = PolicyRule.builder()
                    .action(null)
                    .build();
            
            // When & Then
            assertThat(rule.isAllowAction()).isFalse();
            assertThat(rule.isDenyAction()).isFalse();
        }
    }
    
    @Nested
    @DisplayName("toString 테스트")
    class ToStringTest {
        
        @Test
        @DisplayName("toString 메서드가 올바른 형식으로 출력")
        void toString_ReturnsFormattedString() {
            // Given
            PolicyRule rule = PolicyRule.builder()
                    .ruleId("rule-001")
                    .ruleName("테스트 규칙")
                    .condition("user.role == 'ADMIN'")
                    .action("ALLOW")
                    .priority(100)
                    .enabled(true)
                    .build();
            
            // When
            String result = rule.toString();
            
            // Then
            assertThat(result).contains("rule-001");
            assertThat(result).contains("테스트 규칙");
            assertThat(result).contains("user.role == 'ADMIN'");
            assertThat(result).contains("ALLOW");
            assertThat(result).contains("100");
            assertThat(result).contains("true");
        }
    }
}

