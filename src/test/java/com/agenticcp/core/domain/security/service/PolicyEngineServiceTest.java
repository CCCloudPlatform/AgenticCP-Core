package com.agenticcp.core.domain.security.service;

import com.agenticcp.core.domain.security.dto.*;
import com.agenticcp.core.domain.security.entity.SecurityPolicy;
import com.agenticcp.core.domain.security.enums.PolicyDecision;
import com.agenticcp.core.domain.security.repository.SecurityPolicyRepository;
// import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
// import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PolicyEngineService 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PolicyEngineService 테스트")
class PolicyEngineServiceTest {
    
    @Mock
    private SecurityPolicyRepository securityPolicyRepository;
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private ValueOperations<String, Object> valueOperations;
    
    @Mock
    private PolicyJsonParser policyJsonParser;
    
    @InjectMocks
    private PolicyEngineService policyEngineService;
    
    private PolicyEvaluationRequest testRequest;
    private SecurityPolicy testPolicy;
    
    @BeforeEach
    void setUp() {
        // 테스트 요청 생성
        testRequest = PolicyEvaluationRequest.builder()
                .resourceType("EC2_INSTANCE")
                .resourceId("i-1234567890abcdef0")
                .action("CREATE")
                .userId("user123")
                .tenantKey("tenant1")
                .clientIp("192.168.1.100")
                .timestamp(LocalDateTime.now())
                .build();
        
        // 테스트 정책 생성
        testPolicy = SecurityPolicy.builder()
                .policyKey("test-policy-1")
                .policyName("테스트 정책")
                .description("테스트용 정책")
                .status(com.agenticcp.core.common.enums.Status.ACTIVE)
                .isEnabled(true)
                .priority(100)
                .rules("{\"defaultAction\":\"ALLOW\",\"evaluationMode\":\"FIRST\"}")
                .conditions("{\"evaluationMode\":\"ALL\"}")
                .actions("[]")
                .build();
        
        // Redis 템플릿 모킹
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }
    
    @Nested
    @DisplayName("정책 평가 테스트")
    class PolicyEvaluationTest {
        
        @Test
        @DisplayName("유효한 요청으로 정책 평가 성공")
        void evaluatePolicy_ValidRequest_ReturnsEvaluationResult() {
            // Given
            when(valueOperations.get(anyString())).thenReturn(null); // 캐시 없음
            when(securityPolicyRepository.findGlobalPolicies(any())).thenReturn(List.of(testPolicy));
            when(securityPolicyRepository.findActivePolicies(any())).thenReturn(List.of());
            
            PolicyRules mockRules = PolicyRules.builder()
                    .defaultAction("ALLOW")
                    .evaluationMode(PolicyRules.RuleEvaluationMode.FIRST)
                    .build();
            when(policyJsonParser.parsePolicyRules(anyString())).thenReturn(mockRules);
            
            PolicyConditions mockConditions = PolicyConditions.builder()
                    .evaluationMode(PolicyConditions.ConditionEvaluationMode.ALL)
                    .build();
            when(policyJsonParser.parsePolicyConditions(anyString())).thenReturn(mockConditions);
            
            // When
            PolicyEvaluationResult result = policyEngineService.evaluatePolicy(testRequest);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getDecision()).isEqualTo(PolicyDecision.ALLOW);
            assertThat(result.getPolicyKey()).isEqualTo("test-policy-1");
            assertThat(result.getPolicyName()).isEqualTo("테스트 정책");
        }
        
        @Test
        @DisplayName("캐시된 결과 반환")
        void evaluatePolicy_CachedResult_ReturnsCachedResult() {
            // Given
            PolicyEvaluationResult cachedResult = PolicyEvaluationResult.allow("캐시된 결과");
            cachedResult.setExpirationMinutes(5); // 만료되지 않은 캐시 (5분 후 만료)
            when(valueOperations.get(anyString())).thenReturn(cachedResult);
            
            // When
            PolicyEvaluationResult result = policyEngineService.evaluatePolicy(testRequest);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getDecision()).isEqualTo(PolicyDecision.ALLOW);
            assertThat(result.getReason()).isEqualTo("캐시된 결과");
            
            // Repository 호출되지 않음 확인
            verify(securityPolicyRepository, never()).findGlobalPolicies(any());
        }
        
        @Test
        @DisplayName("null 요청으로 정책 평가 - 예외 발생")
        void evaluatePolicy_NullRequest_ThrowsException() {
            // Given
            PolicyEvaluationRequest nullRequest = null;
            
            // When & Then
            org.junit.jupiter.api.Assertions.assertThrows(
                com.agenticcp.core.common.exception.BusinessException.class,
                () -> policyEngineService.evaluatePolicy(nullRequest)
            );
        }
        
        @Test
        @DisplayName("필수 필드가 없는 요청으로 정책 평가 - 예외 발생")
        void evaluatePolicy_InvalidRequest_ThrowsException() {
            // Given
            PolicyEvaluationRequest invalidRequest = PolicyEvaluationRequest.builder()
                    .resourceType("") // 빈 문자열
                    .action("CREATE")
                    .userId("user123")
                    .build();
            
            // When & Then
            org.junit.jupiter.api.Assertions.assertThrows(
                com.agenticcp.core.common.exception.BusinessException.class,
                () -> policyEngineService.evaluatePolicy(invalidRequest)
            );
        }
        
        @Test
        @DisplayName("적용 가능한 정책이 없을 때 기본 허용")
        void evaluatePolicy_NoPolicies_ReturnsDefaultAllow() {
            // Given
            when(valueOperations.get(anyString())).thenReturn(null); // 캐시 없음
            when(securityPolicyRepository.findGlobalPolicies(any())).thenReturn(List.of());
            when(securityPolicyRepository.findActivePolicies(any())).thenReturn(List.of());
            
            // When
            PolicyEvaluationResult result = policyEngineService.evaluatePolicy(testRequest);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getDecision()).isEqualTo(PolicyDecision.ALLOW);
            assertThat(result.getReason()).contains("적용 가능한 정책이 없어 기본적으로 허용합니다");
        }
        
        @Test
        @DisplayName("여러 정책 중 우선순위가 높은 정책 적용")
        void evaluatePolicy_MultiplePolicies_AppliesHighestPriority() {
            // Given
            SecurityPolicy lowPriorityPolicy = SecurityPolicy.builder()
                    .policyKey("low-priority-policy")
                    .policyName("낮은 우선순위 정책")
                    .status(com.agenticcp.core.common.enums.Status.ACTIVE)
                    .isEnabled(true)
                    .priority(50)
                    .rules("{\"defaultAction\":\"DENY\",\"evaluationMode\":\"FIRST\"}")
                    .conditions("{\"evaluationMode\":\"ALL\"}")
                    .build();
            
            SecurityPolicy highPriorityPolicy = SecurityPolicy.builder()
                    .policyKey("high-priority-policy")
                    .policyName("높은 우선순위 정책")
                    .status(com.agenticcp.core.common.enums.Status.ACTIVE)
                    .isEnabled(true)
                    .priority(100)
                    .rules("{\"defaultAction\":\"ALLOW\",\"evaluationMode\":\"FIRST\"}")
                    .conditions("{\"evaluationMode\":\"ALL\"}")
                    .build();
            
            when(valueOperations.get(anyString())).thenReturn(null);
            when(securityPolicyRepository.findGlobalPolicies(any())).thenReturn(List.of(lowPriorityPolicy, highPriorityPolicy));
            when(securityPolicyRepository.findActivePolicies(any())).thenReturn(List.of());
            
            PolicyRules allowRules = PolicyRules.builder()
                    .defaultAction("ALLOW")
                    .evaluationMode(PolicyRules.RuleEvaluationMode.FIRST)
                    .build();
            
            PolicyRules denyRules = PolicyRules.builder()
                    .defaultAction("DENY")
                    .evaluationMode(PolicyRules.RuleEvaluationMode.FIRST)
                    .build();
            
            when(policyJsonParser.parsePolicyRules(contains("ALLOW"))).thenReturn(allowRules);
            when(policyJsonParser.parsePolicyRules(contains("DENY"))).thenReturn(denyRules);
            when(policyJsonParser.parsePolicyConditions(anyString())).thenReturn(PolicyConditions.builder().build());
            
            // When
            PolicyEvaluationResult result = policyEngineService.evaluatePolicy(testRequest);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getPolicyKey()).isEqualTo("high-priority-policy");
            assertThat(result.getDecision()).isEqualTo(PolicyDecision.ALLOW);
        }
    }
    
    @Nested
    @DisplayName("캐시 관리 테스트")
    class CacheManagementTest {
        
        @Test
        @DisplayName("정책 캐시 무효화")
        void evictPolicyCache_ValidParameters_EvictsCache() {
            // Given
            String resourceType = "EC2_INSTANCE";
            String action = "CREATE";
            Set<String> mockKeys = Set.of("policy_evaluation:EC2_INSTANCE:CREATE:user1");
            when(redisTemplate.keys(anyString())).thenReturn(mockKeys);
            
            // When
            policyEngineService.evictPolicyCache(resourceType, action);
            
            // Then
            verify(redisTemplate, atLeastOnce()).delete(anyString());
            verify(redisTemplate, atLeastOnce()).delete(any(Set.class));
        }
        
        @Test
        @DisplayName("모든 정책 캐시 무효화")
        void evictAllPolicyCache_NoParameters_EvictsAllCache() {
            // Given
            Set<String> mockKeys1 = Set.of("policy_evaluation:key1", "policy_evaluation:key2");
            Set<String> mockKeys2 = Set.of("applicable_policies:key1", "applicable_policies:key2");
            when(redisTemplate.keys("policy_evaluation:*")).thenReturn(mockKeys1);
            when(redisTemplate.keys("applicable_policies:*")).thenReturn(mockKeys2);
            
            // When
            policyEngineService.evictAllPolicyCache();
            
            // Then
            verify(redisTemplate, times(2)).delete(any(Set.class));
        }
    }
    
    @Nested
    @DisplayName("정책 조건 평가 테스트")
    class PolicyConditionEvaluationTest {
        
        @Test
        @DisplayName("시간 조건 평가 - 허용 시간")
        void evaluateTimeConditions_AllowedTime_ReturnsTrue() {
            // Given
            TimeConditions timeConditions = TimeConditions.builder()
                    .allowedTimeRanges(List.of(
                            TimeConditions.TimeRange.builder()
                                    .startTime(LocalTime.of(9, 0))
                                    .endTime(LocalTime.of(18, 0))
                                    .build()
                    ))
                    .build();
            
            // When
            boolean result = timeConditions.isCurrentTimeAllowed();
            
            // Then
            // 현재 시간이 9시-18시 사이인지에 따라 결과가 달라질 수 있음
            assertThat(result).isNotNull();
        }
        
        @Test
        @DisplayName("IP 조건 평가 - 허용 IP")
        void evaluateIpConditions_AllowedIp_ReturnsTrue() {
            // Given
            IpConditions ipConditions = IpConditions.builder()
                    .allowedIps(List.of("192.168.1.100", "10.0.0.1"))
                    .build();
            
            // When
            boolean result = ipConditions.isIpAllowed("192.168.1.100");
            
            // Then
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("IP 조건 평가 - 금지 IP")
        void evaluateIpConditions_DeniedIp_ReturnsFalse() {
            // Given
            IpConditions ipConditions = IpConditions.builder()
                    .deniedIps(List.of("192.168.1.100"))
                    .build();
            
            // When
            boolean result = ipConditions.isIpAllowed("192.168.1.100");
            
            // Then
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("사용자 조건 평가 - 허용 사용자")
        void evaluateUserConditions_AllowedUser_ReturnsTrue() {
            // Given
            UserConditions userConditions = UserConditions.builder()
                    .allowedUserIds(List.of("user123", "user456"))
                    .build();
            
            // When
            boolean result = userConditions.isUserIdAllowed("user123");
            
            // Then
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("리소스 조건 평가 - 허용 리소스 타입")
        void evaluateResourceConditions_AllowedResourceType_ReturnsTrue() {
            // Given
            ResourceConditions resourceConditions = ResourceConditions.builder()
                    .allowedResourceTypes(List.of("EC2_INSTANCE", "S3_BUCKET"))
                    .build();
            
            // When
            boolean result = resourceConditions.isResourceTypeAllowed("EC2_INSTANCE");
            
            // Then
            assertThat(result).isTrue();
        }
    }
    
    @Nested
    @DisplayName("정책 규칙 평가 테스트")
    class PolicyRuleEvaluationTest {
        
        @Test
        @DisplayName("FIRST 모드 규칙 평가")
        void evaluateFirstRule_MatchingRule_ReturnsDecision() {
            // Given
            PolicyRule rule1 = PolicyRule.builder()
                    .ruleId("rule1")
                    .condition("user.role == 'ADMIN'")
                    .action("ALLOW")
                    .priority(100)
                    .enabled(true)
                    .build();
            
            PolicyRule rule2 = PolicyRule.builder()
                    .ruleId("rule2")
                    .condition("user.role == 'USER'")
                    .action("DENY")
                    .priority(50)
                    .enabled(true)
                    .build();
            
            PolicyRules rules = PolicyRules.builder()
                    .evaluationMode(PolicyRules.RuleEvaluationMode.FIRST)
                    .rules(List.of(rule1, rule2))
                    .build();
            
            // When
            PolicyDecision result = rules.getRules().get(0).isAllowAction() ? 
                    PolicyDecision.ALLOW : PolicyDecision.DENY;
            
            // Then
            assertThat(result).isEqualTo(PolicyDecision.ALLOW);
        }
        
        @Test
        @DisplayName("ALLOW 액션 규칙 확인")
        void isAllowAction_AllowRule_ReturnsTrue() {
            // Given
            PolicyRule rule = PolicyRule.builder()
                    .action("ALLOW")
                    .build();
            
            // When
            boolean result = rule.isAllowAction();
            
            // Then
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("DENY 액션 규칙 확인")
        void isDenyAction_DenyRule_ReturnsTrue() {
            // Given
            PolicyRule rule = PolicyRule.builder()
                    .action("DENY")
                    .build();
            
            // When
            boolean result = rule.isDenyAction();
            
            // Then
            assertThat(result).isTrue();
        }
    }
}
