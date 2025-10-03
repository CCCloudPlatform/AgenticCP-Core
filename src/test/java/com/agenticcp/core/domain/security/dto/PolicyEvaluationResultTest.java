package com.agenticcp.core.domain.security.dto;

import com.agenticcp.core.domain.security.enums.PolicyDecision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PolicyEvaluationResult DTO 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@DisplayName("PolicyEvaluationResult DTO 테스트")
class PolicyEvaluationResultTest {
    
    private PolicyEvaluationResult result;
    
    @BeforeEach
    void setUp() {
        result = PolicyEvaluationResult.builder()
                .decision(PolicyDecision.ALLOW)
                .policyKey("test-policy")
                .policyName("테스트 정책")
                .reason("정책 조건을 만족합니다")
                .evaluatedAt(LocalDateTime.now())
                .policyPriority(100)
                .policyVersion("1.0")
                .expired(false)
                .build();
    }
    
    @Nested
    @DisplayName("팩토리 메서드 테스트")
    class FactoryMethodTest {
        
        @Test
        @DisplayName("allow 팩토리 메서드 - 이유만")
        void allow_ReasonOnly_CreatesAllowResult() {
            // When
            PolicyEvaluationResult result = PolicyEvaluationResult.allow("접근 허용");
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getDecision()).isEqualTo(PolicyDecision.ALLOW);
            assertThat(result.getReason()).isEqualTo("접근 허용");
            assertThat(result.getEvaluatedAt()).isNotNull();
            assertThat(result.isExpired()).isFalse();
        }
        
        @Test
        @DisplayName("allow 팩토리 메서드 - 정책 정보 포함")
        void allow_WithPolicyInfo_CreatesAllowResultWithPolicyInfo() {
            // When
            PolicyEvaluationResult result = PolicyEvaluationResult.allow(
                "policy-123", 
                "관리자 정책", 
                "관리자 권한이 있습니다"
            );
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getDecision()).isEqualTo(PolicyDecision.ALLOW);
            assertThat(result.getPolicyKey()).isEqualTo("policy-123");
            assertThat(result.getPolicyName()).isEqualTo("관리자 정책");
            assertThat(result.getReason()).isEqualTo("관리자 권한이 있습니다");
            assertThat(result.getEvaluatedAt()).isNotNull();
            assertThat(result.isExpired()).isFalse();
        }
        
        @Test
        @DisplayName("deny 팩토리 메서드 - 이유만")
        void deny_ReasonOnly_CreatesDenyResult() {
            // When
            PolicyEvaluationResult result = PolicyEvaluationResult.deny("접근 거부");
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getDecision()).isEqualTo(PolicyDecision.DENY);
            assertThat(result.getReason()).isEqualTo("접근 거부");
            assertThat(result.getEvaluatedAt()).isNotNull();
            assertThat(result.isExpired()).isFalse();
        }
        
        @Test
        @DisplayName("deny 팩토리 메서드 - 정책 정보 포함")
        void deny_WithPolicyInfo_CreatesDenyResultWithPolicyInfo() {
            // When
            PolicyEvaluationResult result = PolicyEvaluationResult.deny(
                "policy-456", 
                "보안 정책", 
                "보안 규칙 위반"
            );
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getDecision()).isEqualTo(PolicyDecision.DENY);
            assertThat(result.getPolicyKey()).isEqualTo("policy-456");
            assertThat(result.getPolicyName()).isEqualTo("보안 정책");
            assertThat(result.getReason()).isEqualTo("보안 규칙 위반");
            assertThat(result.getEvaluatedAt()).isNotNull();
            assertThat(result.isExpired()).isFalse();
        }
        
        @Test
        @DisplayName("inconclusive 팩토리 메서드")
        void inconclusive_Reason_CreatesInconclusiveResult() {
            // When
            PolicyEvaluationResult result = PolicyEvaluationResult.inconclusive("조건이 불충분합니다");
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getDecision()).isEqualTo(PolicyDecision.INCONCLUSIVE);
            assertThat(result.getReason()).isEqualTo("조건이 불충분합니다");
            assertThat(result.getEvaluatedAt()).isNotNull();
            assertThat(result.isExpired()).isFalse();
        }
    }
    
    @Nested
    @DisplayName("결정 상태 확인 테스트")
    class DecisionStateTest {
        
        @Test
        @DisplayName("허용 결정 확인")
        void isAllowed_AllowDecision_ReturnsTrue() {
            // Given
            result.setDecision(PolicyDecision.ALLOW);
            
            // When & Then
            assertThat(result.isAllowed()).isTrue();
            assertThat(result.isDenied()).isFalse();
            assertThat(result.isInconclusive()).isFalse();
        }
        
        @Test
        @DisplayName("거부 결정 확인")
        void isDenied_DenyDecision_ReturnsTrue() {
            // Given
            result.setDecision(PolicyDecision.DENY);
            
            // When & Then
            assertThat(result.isAllowed()).isFalse();
            assertThat(result.isDenied()).isTrue();
            assertThat(result.isInconclusive()).isFalse();
        }
        
        @Test
        @DisplayName("결정 불가 확인")
        void isInconclusive_InconclusiveDecision_ReturnsTrue() {
            // Given
            result.setDecision(PolicyDecision.INCONCLUSIVE);
            
            // When & Then
            assertThat(result.isAllowed()).isFalse();
            assertThat(result.isDenied()).isFalse();
            assertThat(result.isInconclusive()).isTrue();
        }
        
        @Test
        @DisplayName("null 결정 확인")
        void isAllowed_NullDecision_ReturnsFalse() {
            // Given
            result.setDecision(null);
            
            // When & Then
            assertThat(result.isAllowed()).isFalse();
            assertThat(result.isDenied()).isFalse();
            assertThat(result.isInconclusive()).isFalse();
        }
    }
    
    @Nested
    @DisplayName("만료 관리 테스트")
    class ExpirationManagementTest {
        
        @Test
        @DisplayName("만료 시간이 없을 때")
        void isExpired_NoExpiration_ReturnsFalse() {
            // Given
            result.setExpiresAt(null);
            result.setExpired(false);
            
            // When
            boolean expired = result.isExpired();
            
            // Then
            assertThat(expired).isFalse();
        }
        
        @Test
        @DisplayName("만료 시간이 미래일 때")
        void isExpired_FutureExpiration_ReturnsFalse() {
            // Given
            result.setExpiresAt(LocalDateTime.now().plusHours(1));
            result.setExpired(false);
            
            // When
            boolean expired = result.isExpired();
            
            // Then
            assertThat(expired).isFalse();
        }
        
        @Test
        @DisplayName("만료 시간이 과거일 때")
        void isExpired_PastExpiration_ReturnsTrue() {
            // Given
            result.setExpiresAt(LocalDateTime.now().minusHours(1));
            result.setExpired(false);
            
            // When
            boolean expired = result.isExpired();
            
            // Then
            assertThat(expired).isTrue();
        }
        
        @Test
        @DisplayName("만료 플래그가 true일 때")
        void isExpired_ExpiredFlagTrue_ReturnsTrue() {
            // Given
            result.setExpiresAt(LocalDateTime.now().plusHours(1));
            result.setExpired(true);
            
            // When
            boolean expired = result.isExpired();
            
            // Then
            assertThat(expired).isTrue();
        }
        
        @Test
        @DisplayName("분 단위로 만료 시간 설정")
        void setExpirationMinutes_ValidMinutes_SetsExpiration() {
            // Given
            LocalDateTime before = LocalDateTime.now();
            
            // When
            result.setExpirationMinutes(10);
            
            // Then
            assertThat(result.getExpiresAt()).isNotNull();
            assertThat(result.getExpiresAt()).isAfter(before);
            assertThat(result.getExpiresAt()).isBefore(LocalDateTime.now().plusMinutes(11));
        }
        
        @Test
        @DisplayName("초 단위로 만료 시간 설정")
        void setExpirationSeconds_ValidSeconds_SetsExpiration() {
            // Given
            LocalDateTime before = LocalDateTime.now();
            
            // When
            result.setExpirationSeconds(60);
            
            // Then
            assertThat(result.getExpiresAt()).isNotNull();
            assertThat(result.getExpiresAt()).isAfter(before);
            assertThat(result.getExpiresAt()).isBefore(LocalDateTime.now().plusSeconds(61));
        }
    }
    
    @Nested
    @DisplayName("평가 시간 관리 테스트")
    class EvaluationTimeTest {
        
        @Test
        @DisplayName("평가 시간 설정")
        void setEvaluationTime_ValidStartTime_CalculatesDuration() throws InterruptedException {
            // Given
            LocalDateTime startTime = LocalDateTime.now().minus(100, ChronoUnit.MILLIS);
            
            // When
            Thread.sleep(10); // 약간의 시간 지연
            result.setEvaluationTime(startTime);
            
            // Then
            assertThat(result.getEvaluationTimeMs()).isNotNull();
            assertThat(result.getEvaluationTimeMs()).isGreaterThanOrEqualTo(10L);
        }
        
        @Test
        @DisplayName("null 시작 시간으로 평가 시간 설정")
        void setEvaluationTime_NullStartTime_DoesNothing() {
            // When
            result.setEvaluationTime(null);
            
            // Then
            assertThat(result.getEvaluationTimeMs()).isNull();
        }
    }
    
    @Nested
    @DisplayName("메타데이터 관리 테스트")
    class MetadataManagementTest {
        
        @Test
        @DisplayName("메타데이터 값 설정 및 가져오기")
        void setMetadataValue_NewValue_SetsValue() {
            // When
            result.setMetadataValue("evaluator", "PolicyEngine");
            
            // Then
            assertThat(result.getMetadataValue("evaluator")).isEqualTo("PolicyEngine");
        }
        
        @Test
        @DisplayName("null 메타데이터에 값 설정")
        void setMetadataValue_NullMetadata_CreatesMetadataAndSetsValue() {
            // Given
            result.setMetadata(null);
            
            // When
            result.setMetadataValue("key", "value");
            
            // Then
            assertThat(result.getMetadata()).isNotNull();
            assertThat(result.getMetadataValue("key")).isEqualTo("value");
        }
        
        @Test
        @DisplayName("존재하지 않는 메타데이터 값 가져오기")
        void getMetadataValue_NonExistingKey_ReturnsNull() {
            // When
            Object value = result.getMetadataValue("nonExistingKey");
            
            // Then
            assertThat(value).isNull();
        }
        
        @Test
        @DisplayName("null 메타데이터에서 값 가져오기")
        void getMetadataValue_NullMetadata_ReturnsNull() {
            // Given
            result.setMetadata(null);
            
            // When
            Object value = result.getMetadataValue("anyKey");
            
            // Then
            assertThat(value).isNull();
        }
    }
    
    @Nested
    @DisplayName("경고 및 오류 관리 테스트")
    class WarningsAndErrorsTest {
        
        @Test
        @DisplayName("경고 추가")
        void addWarning_ValidWarning_AddsToList() {
            // When
            result.addWarning("경고 메시지 1");
            result.addWarning("경고 메시지 2");
            
            // Then
            assertThat(result.getWarnings()).isNotNull();
            assertThat(result.getWarnings()).hasSize(2);
            assertThat(result.getWarnings()).containsExactly("경고 메시지 1", "경고 메시지 2");
        }
        
        @Test
        @DisplayName("null 경고 목록에 경고 추가")
        void addWarning_NullList_CreatesListAndAdds() {
            // Given
            result.setWarnings(null);
            
            // When
            result.addWarning("새 경고");
            
            // Then
            assertThat(result.getWarnings()).isNotNull();
            assertThat(result.getWarnings()).hasSize(1);
            assertThat(result.getWarnings()).containsExactly("새 경고");
        }
        
        @Test
        @DisplayName("오류 추가")
        void addError_ValidError_AddsToList() {
            // When
            result.addError("오류 메시지 1");
            result.addError("오류 메시지 2");
            
            // Then
            assertThat(result.getErrors()).isNotNull();
            assertThat(result.getErrors()).hasSize(2);
            assertThat(result.getErrors()).containsExactly("오류 메시지 1", "오류 메시지 2");
        }
        
        @Test
        @DisplayName("null 오류 목록에 오류 추가")
        void addError_NullList_CreatesListAndAdds() {
            // Given
            result.setErrors(null);
            
            // When
            result.addError("새 오류");
            
            // Then
            assertThat(result.getErrors()).isNotNull();
            assertThat(result.getErrors()).hasSize(1);
            assertThat(result.getErrors()).containsExactly("새 오류");
        }
    }
    
    @Nested
    @DisplayName("toString 테스트")
    class ToStringTest {
        
        @Test
        @DisplayName("toString 메서드가 올바른 형식으로 출력")
        void toString_ReturnsFormattedString() {
            // When
            String resultString = result.toString();
            
            // Then
            assertThat(resultString).contains("ALLOW");
            assertThat(resultString).contains("test-policy");
            assertThat(resultString).contains("정책 조건을 만족합니다");
        }
    }
    
    @Nested
    @DisplayName("빌더 테스트")
    class BuilderTest {
        
        @Test
        @DisplayName("모든 필드로 객체 생성")
        void builder_AllFields_CreatesCompleteObject() {
            // Given
            LocalDateTime evaluatedAt = LocalDateTime.now();
            LocalDateTime expiresAt = evaluatedAt.plusHours(1);
            LocalDateTime policyCreatedAt = evaluatedAt.minusDays(1);
            LocalDateTime policyUpdatedAt = evaluatedAt.minusHours(1);
            
            List<String> warnings = new ArrayList<>();
            warnings.add("경고 1");
            
            List<String> errors = new ArrayList<>();
            errors.add("오류 1");
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("key", "value");
            
            Map<String, Boolean> evaluatedConditions = new HashMap<>();
            evaluatedConditions.put("timeCondition", true);
            
            Map<String, String> evaluatedRules = new HashMap<>();
            evaluatedRules.put("rule1", "ALLOW");
            
            List<PolicyAction> actions = new ArrayList<>();
            
            // When
            PolicyEvaluationResult completeResult = PolicyEvaluationResult.builder()
                    .decision(PolicyDecision.DENY)
                    .policyKey("complete-policy")
                    .policyName("완전한 정책")
                    .reason("모든 필드 테스트")
                    .actions(actions)
                    .evaluatedAt(evaluatedAt)
                    .expiresAt(expiresAt)
                    .expired(false)
                    .evaluationTimeMs(250L)
                    .policyPriority(200)
                    .warnings(warnings)
                    .errors(errors)
                    .metadata(metadata)
                    .requestId("req-test-123")
                    .evaluatedConditions(evaluatedConditions)
                    .evaluatedRules(evaluatedRules)
                    .policyVersion("2.0")
                    .policyCreatedAt(policyCreatedAt)
                    .policyUpdatedAt(policyUpdatedAt)
                    .build();
            
            // Then
            assertThat(completeResult.getDecision()).isEqualTo(PolicyDecision.DENY);
            assertThat(completeResult.getPolicyKey()).isEqualTo("complete-policy");
            assertThat(completeResult.getPolicyName()).isEqualTo("완전한 정책");
            assertThat(completeResult.getReason()).isEqualTo("모든 필드 테스트");
            assertThat(completeResult.getActions()).isEqualTo(actions);
            assertThat(completeResult.getEvaluatedAt()).isEqualTo(evaluatedAt);
            assertThat(completeResult.getExpiresAt()).isEqualTo(expiresAt);
            assertThat(completeResult.isExpired()).isFalse();
            assertThat(completeResult.getEvaluationTimeMs()).isEqualTo(250L);
            assertThat(completeResult.getPolicyPriority()).isEqualTo(200);
            assertThat(completeResult.getWarnings()).isEqualTo(warnings);
            assertThat(completeResult.getErrors()).isEqualTo(errors);
            assertThat(completeResult.getMetadata()).isEqualTo(metadata);
            assertThat(completeResult.getRequestId()).isEqualTo("req-test-123");
            assertThat(completeResult.getEvaluatedConditions()).isEqualTo(evaluatedConditions);
            assertThat(completeResult.getEvaluatedRules()).isEqualTo(evaluatedRules);
            assertThat(completeResult.getPolicyVersion()).isEqualTo("2.0");
            assertThat(completeResult.getPolicyCreatedAt()).isEqualTo(policyCreatedAt);
            assertThat(completeResult.getPolicyUpdatedAt()).isEqualTo(policyUpdatedAt);
        }
    }
}

