package com.agenticcp.core.domain.security.service;

import com.agenticcp.core.domain.security.dto.PolicyConflictResolution;
import com.agenticcp.core.domain.security.entity.SecurityPolicy;
import com.agenticcp.core.domain.security.enums.ConflictResolutionStrategy;
import com.agenticcp.core.domain.security.enums.PolicyDecision;
import com.agenticcp.core.domain.security.repository.SecurityPolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PolicyPriorityService 테스트")
class PolicyPriorityServiceTest {

    @Mock
    private SecurityPolicyRepository policyRepository;

    @InjectMocks
    private PolicyPriorityService policyPriorityService;

    private SecurityPolicy createTestPolicy(Long id, Integer priority) {
        SecurityPolicy policy = SecurityPolicy.builder()
            .policyKey("TEST_POLICY_" + id)
            .policyName("Test Policy " + id)
            .priority(priority)
            .isEnabled(true)
            .build();
        // BaseEntity의 id는 리플렉션으로 설정
        try {
            java.lang.reflect.Field idField = policy.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(policy, id);
        } catch (Exception e) {
            // 테스트에서만 사용하므로 예외 무시
        }
        return policy;
    }

    @Nested
    @DisplayName("우선순위 유효성 검증 테스트")
    class PriorityValidationTest {

        @Test
        @DisplayName("유효한 우선순위 - 최소값")
        void testValidPriority_Min() {
            assertThat(policyPriorityService.isValidPriority(1)).isTrue();
        }

        @Test
        @DisplayName("유효한 우선순위 - 최대값")
        void testValidPriority_Max() {
            assertThat(policyPriorityService.isValidPriority(1000)).isTrue();
        }

        @Test
        @DisplayName("유효한 우선순위 - 중간값")
        void testValidPriority_Middle() {
            assertThat(policyPriorityService.isValidPriority(500)).isTrue();
        }

        @Test
        @DisplayName("유효하지 않은 우선순위 - 최소값 미만")
        void testInvalidPriority_BelowMin() {
            assertThat(policyPriorityService.isValidPriority(0)).isFalse();
        }

        @Test
        @DisplayName("유효하지 않은 우선순위 - 최대값 초과")
        void testInvalidPriority_AboveMax() {
            assertThat(policyPriorityService.isValidPriority(1001)).isFalse();
        }

        @Test
        @DisplayName("유효하지 않은 우선순위 - null")
        void testInvalidPriority_Null() {
            assertThat(policyPriorityService.isValidPriority(null)).isFalse();
        }

        @Test
        @DisplayName("우선순위 검증 예외 발생")
        void testValidatePriority_ThrowsException() {
            assertThatThrownBy(() -> policyPriorityService.validatePriority(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("우선순위는 1~1000 범위여야 합니다");
        }
    }

    @Nested
    @DisplayName("우선순위 기반 정렬 테스트")
    class SortByPriorityTest {

        @Test
        @DisplayName("우선순위 내림차순 정렬")
        void testSortByPriority_Descending() {
            List<SecurityPolicy> policies = Arrays.asList(
                createTestPolicy(1L, 100),
                createTestPolicy(2L, 500),
                createTestPolicy(3L, 300)
            );

            List<SecurityPolicy> sorted = policyPriorityService.sortByPriority(policies);

            assertThat(sorted).hasSize(3);
            assertThat(sorted.get(0).getPriority()).isEqualTo(500);
            assertThat(sorted.get(1).getPriority()).isEqualTo(300);
            assertThat(sorted.get(2).getPriority()).isEqualTo(100);
        }

        @Test
        @DisplayName("우선순위 오름차순 정렬")
        void testSortByPriority_Ascending() {
            List<SecurityPolicy> policies = Arrays.asList(
                createTestPolicy(1L, 100),
                createTestPolicy(2L, 500),
                createTestPolicy(3L, 300)
            );

            List<SecurityPolicy> sorted = policyPriorityService.sortByPriority(policies, true);

            assertThat(sorted).hasSize(3);
            assertThat(sorted.get(0).getPriority()).isEqualTo(100);
            assertThat(sorted.get(1).getPriority()).isEqualTo(300);
            assertThat(sorted.get(2).getPriority()).isEqualTo(500);
        }

        @Test
        @DisplayName("빈 리스트 정렬")
        void testSortByPriority_EmptyList() {
            List<SecurityPolicy> sorted = policyPriorityService.sortByPriority(new ArrayList<>());
            assertThat(sorted).isEmpty();
        }

        @Test
        @DisplayName("null 리스트 정렬")
        void testSortByPriority_NullList() {
            List<SecurityPolicy> sorted = policyPriorityService.sortByPriority(null);
            assertThat(sorted).isEmpty();
        }
    }

    @Nested
    @DisplayName("자동 우선순위 할당 테스트")
    class AutoPriorityAssignmentTest {

        @Test
        @DisplayName("기존 정책이 없을 때 기본값 반환")
        void testAssignAutoPriority_NoPolicies() {
            when(policyRepository.findByTenantIdAndIsEnabledTrue(anyString()))
                .thenReturn(Collections.emptyList());

            Integer priority = policyPriorityService.assignAutoPriority("tenant1");

            assertThat(priority).isEqualTo(PolicyPriorityService.DEFAULT_PRIORITY);
        }

        @Test
        @DisplayName("기존 정책이 있을 때 평균값 계산")
        void testAssignAutoPriority_WithExistingPolicies() {
            List<SecurityPolicy> existingPolicies = Arrays.asList(
                createTestPolicy(1L, 100),
                createTestPolicy(2L, 200),
                createTestPolicy(3L, 300)
            );

            when(policyRepository.findByTenantIdAndIsEnabledTrue(anyString()))
                .thenReturn(existingPolicies);

            Integer priority = policyPriorityService.assignAutoPriority("tenant1");

            // 평균 200을 10 단위로 반올림 = 200
            assertThat(priority).isEqualTo(200);
        }

        @Test
        @DisplayName("전역 정책에 대한 자동 우선순위")
        void testAssignAutoPriority_GlobalPolicies() {
            List<SecurityPolicy> globalPolicies = Arrays.asList(
                createTestPolicy(1L, 800),
                createTestPolicy(2L, 900)
            );

            when(policyRepository.findByIsGlobalTrueAndIsEnabledTrue())
                .thenReturn(globalPolicies);

            Integer priority = policyPriorityService.assignAutoPriority(null);

            // 평균 850을 10 단위로 반올림 = 850
            assertThat(priority).isEqualTo(850);
        }
    }

    @Nested
    @DisplayName("충돌 해결 테스트 - DENY_OVERRIDES")
    class DenyOverridesTest {

        @Test
        @DisplayName("하나라도 DENY가 있으면 DENY 반환")
        void testResolveConflict_DenyOverrides_WithDeny() {
            List<SecurityPolicy> policies = Arrays.asList(
                createTestPolicy(1L, 500),
                createTestPolicy(2L, 600)
            );

            Map<String, PolicyDecision> decisions = new HashMap<>();
            decisions.put("1", PolicyDecision.ALLOW);
            decisions.put("2", PolicyDecision.DENY);

            PolicyConflictResolution resolution = policyPriorityService.resolveConflict(
                policies, decisions, ConflictResolutionStrategy.DENY_OVERRIDES,
                "resource1", "API", "read"
            );

            assertThat(resolution.getFinalDecision()).isEqualTo(PolicyDecision.DENY);
            assertThat(resolution.getStrategy()).isEqualTo(ConflictResolutionStrategy.DENY_OVERRIDES);
        }

        @Test
        @DisplayName("모두 ALLOW이면 ALLOW 반환")
        void testResolveConflict_DenyOverrides_AllAllow() {
            List<SecurityPolicy> policies = Arrays.asList(
                createTestPolicy(1L, 500),
                createTestPolicy(2L, 600)
            );

            Map<String, PolicyDecision> decisions = new HashMap<>();
            decisions.put("1", PolicyDecision.ALLOW);
            decisions.put("2", PolicyDecision.ALLOW);

            PolicyConflictResolution resolution = policyPriorityService.resolveConflict(
                policies, decisions, ConflictResolutionStrategy.DENY_OVERRIDES,
                "resource1", "API", "read"
            );

            assertThat(resolution.getFinalDecision()).isEqualTo(PolicyDecision.ALLOW);
        }
    }

    @Nested
    @DisplayName("충돌 해결 테스트 - ALLOW_OVERRIDES")
    class AllowOverridesTest {

        @Test
        @DisplayName("하나라도 ALLOW가 있으면 ALLOW 반환")
        void testResolveConflict_AllowOverrides_WithAllow() {
            List<SecurityPolicy> policies = Arrays.asList(
                createTestPolicy(1L, 500),
                createTestPolicy(2L, 600)
            );

            Map<String, PolicyDecision> decisions = new HashMap<>();
            decisions.put("1", PolicyDecision.DENY);
            decisions.put("2", PolicyDecision.ALLOW);

            PolicyConflictResolution resolution = policyPriorityService.resolveConflict(
                policies, decisions, ConflictResolutionStrategy.ALLOW_OVERRIDES,
                "resource1", "API", "read"
            );

            assertThat(resolution.getFinalDecision()).isEqualTo(PolicyDecision.ALLOW);
        }

        @Test
        @DisplayName("모두 DENY이면 DENY 반환")
        void testResolveConflict_AllowOverrides_AllDeny() {
            List<SecurityPolicy> policies = Arrays.asList(
                createTestPolicy(1L, 500),
                createTestPolicy(2L, 600)
            );

            Map<String, PolicyDecision> decisions = new HashMap<>();
            decisions.put("1", PolicyDecision.DENY);
            decisions.put("2", PolicyDecision.DENY);

            PolicyConflictResolution resolution = policyPriorityService.resolveConflict(
                policies, decisions, ConflictResolutionStrategy.ALLOW_OVERRIDES,
                "resource1", "API", "read"
            );

            assertThat(resolution.getFinalDecision()).isEqualTo(PolicyDecision.DENY);
        }
    }

    @Nested
    @DisplayName("충돌 해결 테스트 - HIGHEST_PRIORITY")
    class HighestPriorityTest {

        @Test
        @DisplayName("우선순위가 가장 높은 정책의 결정 반환")
        void testResolveConflict_HighestPriority() {
            List<SecurityPolicy> policies = Arrays.asList(
                createTestPolicy(1L, 500),
                createTestPolicy(2L, 800),
                createTestPolicy(3L, 300)
            );

            Map<String, PolicyDecision> decisions = new HashMap<>();
            decisions.put("1", PolicyDecision.ALLOW);
            decisions.put("2", PolicyDecision.DENY);  // 가장 높은 우선순위
            decisions.put("3", PolicyDecision.ALLOW);

            PolicyConflictResolution resolution = policyPriorityService.resolveConflict(
                policies, decisions, ConflictResolutionStrategy.HIGHEST_PRIORITY,
                "resource1", "API", "read"
            );

            assertThat(resolution.getFinalDecision()).isEqualTo(PolicyDecision.DENY);
            assertThat(resolution.getDecidingPolicyId()).isEqualTo("2");
        }
    }

    @Nested
    @DisplayName("충돌 해결 테스트 - MAJORITY_WINS")
    class MajorityWinsTest {

        @Test
        @DisplayName("ALLOW가 더 많으면 ALLOW 반환")
        void testResolveConflict_MajorityWins_AllowMajority() {
            List<SecurityPolicy> policies = Arrays.asList(
                createTestPolicy(1L, 500),
                createTestPolicy(2L, 600),
                createTestPolicy(3L, 700)
            );

            Map<String, PolicyDecision> decisions = new HashMap<>();
            decisions.put("1", PolicyDecision.ALLOW);
            decisions.put("2", PolicyDecision.ALLOW);
            decisions.put("3", PolicyDecision.DENY);

            PolicyConflictResolution resolution = policyPriorityService.resolveConflict(
                policies, decisions, ConflictResolutionStrategy.MAJORITY_WINS,
                "resource1", "API", "read"
            );

            assertThat(resolution.getFinalDecision()).isEqualTo(PolicyDecision.ALLOW);
        }

        @Test
        @DisplayName("DENY가 더 많으면 DENY 반환")
        void testResolveConflict_MajorityWins_DenyMajority() {
            List<SecurityPolicy> policies = Arrays.asList(
                createTestPolicy(1L, 500),
                createTestPolicy(2L, 600),
                createTestPolicy(3L, 700)
            );

            Map<String, PolicyDecision> decisions = new HashMap<>();
            decisions.put("1", PolicyDecision.DENY);
            decisions.put("2", PolicyDecision.DENY);
            decisions.put("3", PolicyDecision.ALLOW);

            PolicyConflictResolution resolution = policyPriorityService.resolveConflict(
                policies, decisions, ConflictResolutionStrategy.MAJORITY_WINS,
                "resource1", "API", "read"
            );

            assertThat(resolution.getFinalDecision()).isEqualTo(PolicyDecision.DENY);
        }

        @Test
        @DisplayName("동수일 경우 DENY 반환 (보안 우선)")
        void testResolveConflict_MajorityWins_Tie() {
            List<SecurityPolicy> policies = Arrays.asList(
                createTestPolicy(1L, 500),
                createTestPolicy(2L, 600)
            );

            Map<String, PolicyDecision> decisions = new HashMap<>();
            decisions.put("1", PolicyDecision.ALLOW);
            decisions.put("2", PolicyDecision.DENY);

            PolicyConflictResolution resolution = policyPriorityService.resolveConflict(
                policies, decisions, ConflictResolutionStrategy.MAJORITY_WINS,
                "resource1", "API", "read"
            );

            assertThat(resolution.getFinalDecision()).isEqualTo(PolicyDecision.DENY);
            assertThat(resolution.getWarnings()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("충돌 해결 테스트 - WEIGHTED_SUM")
    class WeightedSumTest {

        @Test
        @DisplayName("ALLOW 가중치 합이 더 크면 ALLOW 반환")
        void testResolveConflict_WeightedSum_AllowWins() {
            List<SecurityPolicy> policies = Arrays.asList(
                createTestPolicy(1L, 800),  // ALLOW with high priority
                createTestPolicy(2L, 300)   // DENY with low priority
            );

            Map<String, PolicyDecision> decisions = new HashMap<>();
            decisions.put("1", PolicyDecision.ALLOW);  // 800 weight
            decisions.put("2", PolicyDecision.DENY);   // 300 weight

            PolicyConflictResolution resolution = policyPriorityService.resolveConflict(
                policies, decisions, ConflictResolutionStrategy.WEIGHTED_SUM,
                "resource1", "API", "read"
            );

            assertThat(resolution.getFinalDecision()).isEqualTo(PolicyDecision.ALLOW);
        }

        @Test
        @DisplayName("DENY 가중치 합이 더 크면 DENY 반환")
        void testResolveConflict_WeightedSum_DenyWins() {
            List<SecurityPolicy> policies = Arrays.asList(
                createTestPolicy(1L, 300),  // ALLOW with low priority
                createTestPolicy(2L, 800)   // DENY with high priority
            );

            Map<String, PolicyDecision> decisions = new HashMap<>();
            decisions.put("1", PolicyDecision.ALLOW);  // 300 weight
            decisions.put("2", PolicyDecision.DENY);   // 800 weight

            PolicyConflictResolution resolution = policyPriorityService.resolveConflict(
                policies, decisions, ConflictResolutionStrategy.WEIGHTED_SUM,
                "resource1", "API", "read"
            );

            assertThat(resolution.getFinalDecision()).isEqualTo(PolicyDecision.DENY);
        }
    }

    @Nested
    @DisplayName("충돌 심각도 계산 테스트")
    class ConflictSeverityTest {

        @Test
        @DisplayName("충돌 없을 때 - LOW")
        void testConflictSeverity_NoConflict() {
            List<SecurityPolicy> policies = Arrays.asList(
                createTestPolicy(1L, 500),
                createTestPolicy(2L, 600)
            );

            Map<String, PolicyDecision> decisions = new HashMap<>();
            decisions.put("1", PolicyDecision.ALLOW);
            decisions.put("2", PolicyDecision.ALLOW);

            PolicyConflictResolution resolution = policyPriorityService.resolveConflict(
                policies, decisions, ConflictResolutionStrategy.DENY_OVERRIDES,
                "resource1", "API", "read"
            );

            assertThat(resolution.getSeverity())
                .isEqualTo(PolicyConflictResolution.ConflictSeverity.LOW);
        }

        @Test
        @DisplayName("10개 이상 충돌 - CRITICAL")
        void testConflictSeverity_Critical() {
            List<SecurityPolicy> policies = new ArrayList<>();
            Map<String, PolicyDecision> decisions = new HashMap<>();

            for (int i = 1; i <= 12; i++) {
                policies.add(createTestPolicy((long) i, i * 100));
                decisions.put(String.valueOf(i), i % 2 == 0 ? PolicyDecision.ALLOW : PolicyDecision.DENY);
            }

            PolicyConflictResolution resolution = policyPriorityService.resolveConflict(
                policies, decisions, ConflictResolutionStrategy.DENY_OVERRIDES,
                "resource1", "API", "read"
            );

            assertThat(resolution.getSeverity())
                .isEqualTo(PolicyConflictResolution.ConflictSeverity.CRITICAL);
        }
    }

    @Nested
    @DisplayName("다음 사용 가능한 우선순위 찾기 테스트")
    class FindNextAvailablePriorityTest {

        @Test
        @DisplayName("현재 우선순위가 사용되지 않을 때")
        void testFindNextAvailablePriority_CurrentAvailable() {
            when(policyRepository.findByTenantIdAndIsEnabledTrue(anyString()))
                .thenReturn(Arrays.asList(
                    createTestPolicy(1L, 100),
                    createTestPolicy(2L, 200)
                ));

            Integer nextPriority = policyPriorityService.findNextAvailablePriority("tenant1", 300);

            assertThat(nextPriority).isEqualTo(300);
        }

        @Test
        @DisplayName("현재 우선순위가 사용 중일 때 위로 탐색")
        void testFindNextAvailablePriority_SearchUp() {
            when(policyRepository.findByTenantIdAndIsEnabledTrue(anyString()))
                .thenReturn(Arrays.asList(
                    createTestPolicy(1L, 300),
                    createTestPolicy(2L, 310)
                ));

            Integer nextPriority = policyPriorityService.findNextAvailablePriority("tenant1", 300);

            assertThat(nextPriority).isEqualTo(320);
        }
    }
}

