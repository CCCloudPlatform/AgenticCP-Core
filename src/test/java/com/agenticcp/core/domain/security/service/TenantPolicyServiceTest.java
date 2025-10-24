package com.agenticcp.core.domain.security.service;

import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.security.dto.EffectivePolicySetDTO;
import com.agenticcp.core.domain.security.entity.SecurityPolicy;
import com.agenticcp.core.domain.security.repository.SecurityPolicyRepository;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.tenant.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TenantPolicyService 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TenantPolicyService 테스트")
class TenantPolicyServiceTest {
    
    @Mock
    private SecurityPolicyRepository policyRepository;
    
    @Mock
    private TenantRepository tenantRepository;
    
    @InjectMocks
    private TenantPolicyService tenantPolicyService;
    
    private Tenant testTenant;
    private List<SecurityPolicy> globalPolicies;
    private List<SecurityPolicy> tenantPolicies;
    
    @BeforeEach
    void setUp() {
        // 테스트 테넌트 설정
        testTenant = Tenant.builder()
                .tenantKey("TEST_TENANT")
                .tenantName("테스트 테넌트")
                .status(Status.ACTIVE)
                .build();
        testTenant.setId(1L);
        
        // 글로벌 정책 설정
        globalPolicies = new ArrayList<>();
        SecurityPolicy globalPolicy1 = SecurityPolicy.builder()
                .policyKey("GLOBAL_POLICY_1")
                .policyName("글로벌 정책 1")
                .policyType(SecurityPolicy.PolicyType.ACCESS_CONTROL)
                .priority(100)
                .isGlobal(true)
                .isEnabled(true)
                .build();
        globalPolicy1.setId(1L);
        globalPolicies.add(globalPolicy1);
        
        SecurityPolicy globalPolicy2 = SecurityPolicy.builder()
                .policyKey("GLOBAL_POLICY_2")
                .policyName("글로벌 정책 2")
                .policyType(SecurityPolicy.PolicyType.AUTHENTICATION)
                .priority(200)
                .isGlobal(true)
                .isEnabled(true)
                .build();
        globalPolicy2.setId(2L);
        globalPolicies.add(globalPolicy2);
        
        // 테넌트 정책 설정
        tenantPolicies = new ArrayList<>();
        SecurityPolicy tenantPolicy1 = SecurityPolicy.builder()
                .policyKey("TENANT_POLICY_1")
                .policyName("테넌트 정책 1")
                .policyType(SecurityPolicy.PolicyType.DATA_PROTECTION)
                .priority(150)
                .tenant(testTenant)
                .isGlobal(false)
                .isEnabled(true)
                .build();
        tenantPolicy1.setId(3L);
        tenantPolicies.add(tenantPolicy1);
    }
    
    @Nested
    @DisplayName("유효한 정책 조회 테스트")
    class GetEffectivePoliciesTest {
        
        @Test
        @DisplayName("글로벌 + 테넌트 정책 조회 성공")
        void getEffectivePolicies_WithValidTenantId_Success() {
            // Given
            when(tenantRepository.findById(1L)).thenReturn(Optional.of(testTenant));
            when(policyRepository.findByIsGlobalTrueAndIsEnabledTrue()).thenReturn(globalPolicies);
            when(policyRepository.findByTenantIdAndIsEnabledTrue(1L)).thenReturn(tenantPolicies);
            
            // When
            EffectivePolicySetDTO result = tenantPolicyService.getEffectivePolicies(1L);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTenantId()).isEqualTo("1");
            assertThat(result.getGlobalPolicyCount()).isEqualTo(2);
            assertThat(result.getTenantPolicyCount()).isEqualTo(1);
            assertThat(result.getTotalPolicyCount()).isEqualTo(3);
            
            verify(tenantRepository).findById(1L);
            verify(policyRepository).findByIsGlobalTrueAndIsEnabledTrue();
            verify(policyRepository).findByTenantIdAndIsEnabledTrue(1L);
        }
        
        @Test
        @DisplayName("테넌트가 존재하지 않으면 예외 발생")
        void getEffectivePolicies_WithNonExistentTenant_ThrowsException() {
            // Given
            when(tenantRepository.findById(999L)).thenReturn(Optional.empty());
            
            // When & Then
            assertThatThrownBy(() -> tenantPolicyService.getEffectivePolicies(999L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("테넌트를 찾을 수 없습니다");
            
            verify(tenantRepository).findById(999L);
            verify(policyRepository, never()).findByIsGlobalTrueAndIsEnabledTrue();
        }
        
        @Test
        @DisplayName("글로벌 정책만 있는 경우")
        void getEffectivePolicies_WithOnlyGlobalPolicies_Success() {
            // Given
            when(tenantRepository.findById(1L)).thenReturn(Optional.of(testTenant));
            when(policyRepository.findByIsGlobalTrueAndIsEnabledTrue()).thenReturn(globalPolicies);
            when(policyRepository.findByTenantIdAndIsEnabledTrue(1L)).thenReturn(new ArrayList<>());
            
            // When
            EffectivePolicySetDTO result = tenantPolicyService.getEffectivePolicies(1L);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getGlobalPolicyCount()).isEqualTo(2);
            assertThat(result.getTenantPolicyCount()).isEqualTo(0);
            assertThat(result.getTotalPolicyCount()).isEqualTo(2);
        }
        
        @Test
        @DisplayName("정책이 하나도 없는 경우")
        void getEffectivePolicies_WithNoPolicies_ReturnsEmptySet() {
            // Given
            when(tenantRepository.findById(1L)).thenReturn(Optional.of(testTenant));
            when(policyRepository.findByIsGlobalTrueAndIsEnabledTrue()).thenReturn(new ArrayList<>());
            when(policyRepository.findByTenantIdAndIsEnabledTrue(1L)).thenReturn(new ArrayList<>());
            
            // When
            EffectivePolicySetDTO result = tenantPolicyService.getEffectivePolicies(1L);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalPolicyCount()).isEqualTo(0);
        }
    }
    
    @Nested
    @DisplayName("정렬된 정책 조회 테스트")
    class GetSortedEffectivePoliciesTest {
        
        @Test
        @DisplayName("우선순위 내림차순 정렬")
        void getSortedEffectivePolicies_DescendingOrder_Success() {
            // Given
            when(policyRepository.findByIsGlobalTrueAndIsEnabledTrue()).thenReturn(globalPolicies);
            when(policyRepository.findByTenantIdAndIsEnabledTrue(1L)).thenReturn(tenantPolicies);
            
            // When
            List<SecurityPolicy> result = tenantPolicyService.getSortedEffectivePolicies(1L, false);
            
            // Then
            assertThat(result).isNotEmpty();
            assertThat(result).hasSize(3);
            assertThat(result.get(0).getPriority()).isGreaterThanOrEqualTo(result.get(1).getPriority());
        }
        
        @Test
        @DisplayName("우선순위 오름차순 정렬")
        void getSortedEffectivePolicies_AscendingOrder_Success() {
            // Given
            when(policyRepository.findByIsGlobalTrueAndIsEnabledTrue()).thenReturn(globalPolicies);
            when(policyRepository.findByTenantIdAndIsEnabledTrue(1L)).thenReturn(tenantPolicies);
            
            // When
            List<SecurityPolicy> result = tenantPolicyService.getSortedEffectivePolicies(1L, true);
            
            // Then
            assertThat(result).isNotEmpty();
            assertThat(result).hasSize(3);
            assertThat(result.get(0).getPriority()).isLessThanOrEqualTo(result.get(1).getPriority());
        }
    }
    
    @Nested
    @DisplayName("기본 정책 초기화 테스트")
    class InitializeDefaultPoliciesTest {
        
        @Test
        @DisplayName("기본 정책 5개 생성 성공")
        void initializeDefaultPolicies_Success() {
            // Given
            when(policyRepository.save(any(SecurityPolicy.class))).thenAnswer(invocation -> {
                SecurityPolicy policy = invocation.getArgument(0);
                policy.setId(1L);
                return policy;
            });
            
            // When
            List<SecurityPolicy> result = tenantPolicyService.initializeDefaultPolicies(testTenant);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(5);  // 5개 기본 정책
            
            // 정책 타입 검증
            assertThat(result).extracting(SecurityPolicy::getPolicyType)
                    .contains(
                            SecurityPolicy.PolicyType.ACCESS_CONTROL,
                            SecurityPolicy.PolicyType.AUTHENTICATION,
                            SecurityPolicy.PolicyType.AUTHORIZATION,
                            SecurityPolicy.PolicyType.DATA_PROTECTION,
                            SecurityPolicy.PolicyType.AUDIT_LOGGING
                    );
            
            // 모두 시스템 정책이어야 함
            assertThat(result).allMatch(SecurityPolicy::getIsSystem);
            
            // 모두 활성화되어 있어야 함
            assertThat(result).allMatch(SecurityPolicy::getIsEnabled);
            
            verify(policyRepository, times(5)).save(any(SecurityPolicy.class));
        }
    }
    
    @Nested
    @DisplayName("정책 타입별 조회 테스트")
    class GetPoliciesByTypeTest {
        
        @Test
        @DisplayName("특정 정책 타입만 필터링")
        void getPoliciesByType_WithSpecificType_Success() {
            // Given
            when(policyRepository.findByIsGlobalTrueAndIsEnabledTrue()).thenReturn(globalPolicies);
            when(policyRepository.findByTenantIdAndIsEnabledTrue(1L)).thenReturn(tenantPolicies);
            
            // When
            List<SecurityPolicy> result = tenantPolicyService.getPoliciesByType(1L, SecurityPolicy.PolicyType.ACCESS_CONTROL);
            
            // Then
            assertThat(result).isNotEmpty();
            assertThat(result).allMatch(p -> p.getPolicyType() == SecurityPolicy.PolicyType.ACCESS_CONTROL);
        }
        
        @Test
        @DisplayName("해당 타입 정책이 없으면 빈 목록 반환")
        void getPoliciesByType_WithNonExistentType_ReturnsEmptyList() {
            // Given
            when(policyRepository.findByIsGlobalTrueAndIsEnabledTrue()).thenReturn(globalPolicies);
            when(policyRepository.findByTenantIdAndIsEnabledTrue(1L)).thenReturn(tenantPolicies);
            
            // When
            List<SecurityPolicy> result = tenantPolicyService.getPoliciesByType(1L, SecurityPolicy.PolicyType.COMPLIANCE);
            
            // Then
            assertThat(result).isEmpty();
        }
    }
    
    @Nested
    @DisplayName("정책 통계 테스트")
    class GetPolicyStatisticsTest {
        
        @Test
        @DisplayName("정책 통계 조회 성공")
        void getPolicyStatistics_Success() {
            // Given
            when(tenantRepository.findById(1L)).thenReturn(Optional.of(testTenant));
            when(policyRepository.findByIsGlobalTrueAndIsEnabledTrue()).thenReturn(globalPolicies);
            when(policyRepository.findByTenantIdAndIsEnabledTrue(1L)).thenReturn(tenantPolicies);
            
            // When
            TenantPolicyService.PolicyStatisticsDTO result = tenantPolicyService.getPolicyStatistics(1L);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTenantId()).isEqualTo("1");
            assertThat(result.getTotalPolicies()).isEqualTo(3);
            assertThat(result.getGlobalPolicies()).isEqualTo(2);
            assertThat(result.getTenantPolicies()).isEqualTo(1);
            assertThat(result.getPoliciesByType()).isNotNull();
            assertThat(result.getPoliciesBySeverity()).isNotNull();
        }
    }
    
    @Nested
    @DisplayName("캐시 무효화 테스트")
    class EvictCacheTest {
        
        @Test
        @DisplayName("테넌트 정책 캐시 무효화")
        void evictTenantPolicyCache_Success() {
            // When
            tenantPolicyService.evictTenantPolicyCache(1L);
            
            // Then
            // 예외가 발생하지 않으면 성공
        }
        
        @Test
        @DisplayName("전체 캐시 무효화")
        void evictAllTenantPolicyCache_Success() {
            // When
            tenantPolicyService.evictAllTenantPolicyCache();
            
            // Then
            // 예외가 발생하지 않으면 성공
        }
    }
}

