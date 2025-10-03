package com.agenticcp.core.domain.security.service;

import com.agenticcp.core.domain.security.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PolicyJsonParser 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@DisplayName("PolicyJsonParser 테스트")
class PolicyJsonParserTest {
    
    private PolicyJsonParser policyJsonParser;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        policyJsonParser = new PolicyJsonParser(objectMapper);
    }
    
    @Nested
    @DisplayName("정책 규칙 파싱 테스트")
    class PolicyRulesParsingTest {
        
        @Test
        @DisplayName("유효한 정책 규칙 JSON 파싱")
        void parsePolicyRules_ValidJson_ReturnsPolicyRules() {
            // Given
            String rulesJson = """
                {
                    "defaultAction": "DENY",
                    "evaluationMode": "FIRST",
                    "rules": [
                        {
                            "ruleId": "rule1",
                            "ruleName": "관리자 규칙",
                            "condition": "user.role == 'ADMIN'",
                            "action": "ALLOW",
                            "priority": 100,
                            "enabled": true
                        }
                    ]
                }
                """;
            
            // When
            PolicyRules result = policyJsonParser.parsePolicyRules(rulesJson);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getDefaultAction()).isEqualTo("DENY");
            assertThat(result.getEvaluationMode()).isEqualTo(PolicyRules.RuleEvaluationMode.FIRST);
            assertThat(result.getRuleCount()).isEqualTo(1);
            assertThat(result.getRules()).hasSize(1);
            assertThat(result.getRules().get(0).getRuleId()).isEqualTo("rule1");
            assertThat(result.getRules().get(0).getAction()).isEqualTo("ALLOW");
        }
        
        @Test
        @DisplayName("빈 정책 규칙 JSON 파싱")
        void parsePolicyRules_EmptyJson_ReturnsEmptyPolicyRules() {
            // Given
            String rulesJson = "{}";
            
            // When
            PolicyRules result = policyJsonParser.parsePolicyRules(rulesJson);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getRuleCount()).isEqualTo(0);
            assertThat(result.isEmpty()).isTrue();
        }
        
        @Test
        @DisplayName("null 정책 규칙 JSON 파싱")
        void parsePolicyRules_NullJson_ReturnsEmptyPolicyRules() {
            // Given
            String rulesJson = null;
            
            // When
            PolicyRules result = policyJsonParser.parsePolicyRules(rulesJson);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getRuleCount()).isEqualTo(0);
            assertThat(result.isEmpty()).isTrue();
        }
        
        @Test
        @DisplayName("잘못된 정책 규칙 JSON 파싱")
        void parsePolicyRules_InvalidJson_ReturnsEmptyPolicyRules() {
            // Given
            String rulesJson = "{ invalid json }";
            
            // When
            PolicyRules result = policyJsonParser.parsePolicyRules(rulesJson);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getRuleCount()).isEqualTo(0);
            assertThat(result.isEmpty()).isTrue();
        }
    }
    
    @Nested
    @DisplayName("정책 조건 파싱 테스트")
    class PolicyConditionsParsingTest {
        
        @Test
        @DisplayName("유효한 정책 조건 JSON 파싱")
        void parsePolicyConditions_ValidJson_ReturnsPolicyConditions() {
            // Given
            String conditionsJson = """
                {
                    "evaluationMode": "ALL",
                    "timeConditions": {
                        "allowedTimeRanges": [
                            {
                                "startTime": "09:00",
                                "endTime": "18:00",
                                "description": "업무시간"
                            }
                        ]
                    },
                    "ipConditions": {
                        "allowedIps": ["192.168.1.1", "10.0.0.1"]
                    }
                }
                """;
            
            // When
            PolicyConditions result = policyJsonParser.parsePolicyConditions(conditionsJson);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getEvaluationMode()).isEqualTo(PolicyConditions.ConditionEvaluationMode.ALL);
            assertThat(result.getTimeConditions()).isNotNull();
            assertThat(result.getIpConditions()).isNotNull();
            assertThat(result.getActiveConditionCount()).isEqualTo(2);
        }
        
        @Test
        @DisplayName("빈 정책 조건 JSON 파싱")
        void parsePolicyConditions_EmptyJson_ReturnsEmptyPolicyConditions() {
            // Given
            String conditionsJson = "{}";
            
            // When
            PolicyConditions result = policyJsonParser.parsePolicyConditions(conditionsJson);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getActiveConditionCount()).isEqualTo(0);
            assertThat(result.isEmpty()).isTrue();
        }
    }
    
    @Nested
    @DisplayName("정책 액션 파싱 테스트")
    class PolicyActionsParsingTest {
        
        @Test
        @DisplayName("유효한 정책 액션 JSON 파싱")
        void parsePolicyActions_ValidJson_ReturnsPolicyActions() {
            // Given
            String actionsJson = """
                [
                    {
                        "type": "BLOCK_USER",
                        "description": "사용자 차단",
                        "duration": "PT1H",
                        "priority": 100,
                        "status": "PENDING"
                    },
                    {
                        "type": "SEND_ALERT",
                        "description": "알림 발송",
                        "priority": 50,
                        "status": "PENDING"
                    }
                ]
                """;
            
            // When
            List<PolicyAction> result = policyJsonParser.parsePolicyActions(actionsJson);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getType()).isEqualTo(PolicyAction.ActionType.BLOCK_USER);
            assertThat(result.get(1).getType()).isEqualTo(PolicyAction.ActionType.SEND_ALERT);
        }
        
        @Test
        @DisplayName("빈 정책 액션 JSON 파싱")
        void parsePolicyActions_EmptyJson_ReturnsEmptyList() {
            // Given
            String actionsJson = "[]";
            
            // When
            List<PolicyAction> result = policyJsonParser.parsePolicyActions(actionsJson);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result).isEmpty();
        }
    }
    
    @Nested
    @DisplayName("JSON 유효성 검증 테스트")
    class JsonValidationTest {
        
        @Test
        @DisplayName("유효한 JSON 검증")
        void isValidJson_ValidJson_ReturnsTrue() {
            // Given
            String validJson = "{\"key\": \"value\"}";
            
            // When
            boolean result = policyJsonParser.isValidJson(validJson);
            
            // Then
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("잘못된 JSON 검증")
        void isValidJson_InvalidJson_ReturnsFalse() {
            // Given
            String invalidJson = "{ invalid json }";
            
            // When
            boolean result = policyJsonParser.isValidJson(invalidJson);
            
            // Then
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("null JSON 검증")
        void isValidJson_NullJson_ReturnsFalse() {
            // Given
            String nullJson = null;
            
            // When
            boolean result = policyJsonParser.isValidJson(nullJson);
            
            // Then
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("빈 JSON 검증")
        void isValidJson_EmptyJson_ReturnsFalse() {
            // Given
            String emptyJson = "";
            
            // When
            boolean result = policyJsonParser.isValidJson(emptyJson);
            
            // Then
            assertThat(result).isFalse();
        }
    }
    
    @Nested
    @DisplayName("ISO 8601 날짜/시간 파싱 테스트")
    class Iso8601DateTimeParsingTest {
        
        @Test
        @DisplayName("ISO 8601 형식의 timestamp 파싱")
        void parseIso8601Timestamp_ValidFormat_ReturnsPolicyEvaluationRequest() {
            // Given
            String requestJson = """
                {
                    "resourceType": "EC2_INSTANCE",
                    "resourceId": "i-1234567890abcdef0",
                    "action": "START",
                    "userId": "user123",
                    "tenantKey": "tenant1",
                    "clientIp": "192.168.1.1",
                    "timestamp": "2025-09-21T14:02:01.664Z"
                }
                """;
            
            // When
            PolicyEvaluationRequest result = policyJsonParser.parsePolicyEvaluationRequest(requestJson);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getResourceType()).isEqualTo("EC2_INSTANCE");
            assertThat(result.getResourceId()).isEqualTo("i-1234567890abcdef0");
            assertThat(result.getAction()).isEqualTo("START");
            assertThat(result.getUserId()).isEqualTo("user123");
            assertThat(result.getTenantKey()).isEqualTo("tenant1");
            assertThat(result.getClientIp()).isEqualTo("192.168.1.1");
            assertThat(result.getTimestamp()).isNotNull();
            // timestamp가 정상적으로 파싱되었는지 확인
            assertThat(result.getTimestamp().getYear()).isEqualTo(2025);
            assertThat(result.getTimestamp().getMonthValue()).isEqualTo(9);
            assertThat(result.getTimestamp().getDayOfMonth()).isEqualTo(21);
            assertThat(result.getTimestamp().getHour()).isEqualTo(14);
            assertThat(result.getTimestamp().getMinute()).isEqualTo(2);
            assertThat(result.getTimestamp().getSecond()).isEqualTo(1);
        }
        
        @Test
        @DisplayName("ISO 8601 형식의 expiresAt 파싱")
        void parseIso8601ExpiresAt_ValidFormat_ReturnsPolicyEvaluationRequest() {
            // Given
            String requestJson = """
                {
                    "resourceType": "S3_BUCKET",
                    "resourceId": "my-bucket",
                    "action": "DELETE",
                    "userId": "user456",
                    "tenantKey": "tenant2",
                    "clientIp": "10.0.0.1",
                    "timestamp": "2025-09-21T14:02:01.664Z",
                    "expiresAt": "2025-09-21T15:02:01.664Z"
                }
                """;
            
            // When
            PolicyEvaluationRequest result = policyJsonParser.parsePolicyEvaluationRequest(requestJson);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getExpiresAt()).isNotNull();
            assertThat(result.getExpiresAt().getYear()).isEqualTo(2025);
            assertThat(result.getExpiresAt().getMonthValue()).isEqualTo(9);
            assertThat(result.getExpiresAt().getDayOfMonth()).isEqualTo(21);
            assertThat(result.getExpiresAt().getHour()).isEqualTo(15);
            assertThat(result.getExpiresAt().getMinute()).isEqualTo(2);
            assertThat(result.getExpiresAt().getSecond()).isEqualTo(1);
        }
    }
    
    @Nested
    @DisplayName("JSON 변환 테스트")
    class JsonConversionTest {
        
        @Test
        @DisplayName("PolicyRules를 JSON으로 변환")
        void toJson_PolicyRules_ReturnsJsonString() {
            // Given
            PolicyRules rules = PolicyRules.builder()
                    .defaultAction("DENY")
                    .evaluationMode(PolicyRules.RuleEvaluationMode.FIRST)
                    .build();
            
            // When
            String result = policyJsonParser.toJson(rules);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result).contains("DENY");
            assertThat(result).contains("FIRST");
        }
        
        @Test
        @DisplayName("PolicyConditions를 JSON으로 변환")
        void toJson_PolicyConditions_ReturnsJsonString() {
            // Given
            PolicyConditions conditions = PolicyConditions.builder()
                    .evaluationMode(PolicyConditions.ConditionEvaluationMode.ALL)
                    .build();
            
            // When
            String result = policyJsonParser.toJson(conditions);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result).contains("ALL");
        }
        
        @Test
        @DisplayName("null 객체를 JSON으로 변환")
        void toJson_NullObject_ReturnsNull() {
            // Given
            PolicyRules rules = null;
            
            // When
            String result = policyJsonParser.toJson(rules);
            
            // Then
            assertThat(result).isNull();
        }
    }
}
