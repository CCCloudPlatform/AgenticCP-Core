package com.agenticcp.core.domain.platform.service;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.platform.TestDataBuilder;
import com.agenticcp.core.domain.platform.dto.CreateTargetRuleRequestDto;
import com.agenticcp.core.domain.platform.dto.TargetRuleResponseDto;
import com.agenticcp.core.domain.platform.dto.UpdateTargetRuleRequestDto;
import com.agenticcp.core.domain.platform.entity.FeatureFlag;
import com.agenticcp.core.domain.platform.entity.FeatureFlagTargetRule;
import com.agenticcp.core.domain.platform.repository.FeatureFlagRepository;
import com.agenticcp.core.domain.platform.repository.FeatureFlagTargetRuleRepository;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TargetingRuleService 단위 테스트
 * 
 * 타겟팅 규칙 관리 서비스의 모든 기능을 테스트합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class TargetingRuleServiceTest {

    @Mock
    private FeatureFlagTargetRuleRepository targetRuleRepository;

    @Mock
    private FeatureFlagRepository featureFlagRepository;

    @InjectMocks
    private TargetingRuleService targetingRuleService;

    private Tenant mockTenant;
    private MockedStatic<TenantContextHolder> tenantContextMock;

    @BeforeEach
    void setUp() {
        mockTenant = Tenant.builder()
                .tenantKey("test-tenant")
                .tenantName("테스트 테넌트")
                .build();
        mockTenant.setId(1L);

        // 기존 static mock이 있다면 해제
        if (tenantContextMock != null) {
            tenantContextMock.close();
        }
        
        tenantContextMock = mockStatic(TenantContextHolder.class);
        tenantContextMock.when(TenantContextHolder::getCurrentTenantOrThrow).thenReturn(mockTenant);
    }

    @AfterEach
    void tearDown() {
        if (tenantContextMock != null) {
            tenantContextMock.close();
        }
    }

    @Nested
    @DisplayName("타겟팅 규칙 조회 테스트")
    class GetTargetRulesTest {

        @Test
        @DisplayName("정상적인 타겟팅 규칙 조회")
        void getTargetRulesByFlagKey_Success() {
            // Given
            String flagKey = "test-feature-flag";
            FeatureFlag featureFlag = TestDataBuilder.featureFlagBuilder().build();
            FeatureFlagTargetRule targetRule = TestDataBuilder.targetRuleBuilder().build();
            List<FeatureFlagTargetRule> targetRules = List.of(targetRule);

            when(featureFlagRepository.findByFlagKey(flagKey)).thenReturn(Optional.of(featureFlag));
            when(targetRuleRepository.findByFlagKeyAndTenantId(flagKey, 1L)).thenReturn(targetRules);

            // When
            List<TargetRuleResponseDto> result = targetingRuleService.getTargetRulesByFlagKey(flagKey);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getFlagKey()).isEqualTo(flagKey);
            assertThat(result.get(0).getRuleName()).isEqualTo("테스트 타겟팅 규칙");
            verify(featureFlagRepository).findByFlagKey(flagKey);
            verify(targetRuleRepository).findByFlagKeyAndTenantId(flagKey, 1L);
        }

        @Test
        @DisplayName("존재하지 않는 기능 플래그로 조회 시 예외 발생")
        void getTargetRulesByFlagKey_NonExistingFlag_ThrowsException() {
            // Given
            String flagKey = "non-existing-flag";
            when(featureFlagRepository.findByFlagKey(flagKey)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> targetingRuleService.getTargetRulesByFlagKey(flagKey))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("기능 플래그를 찾을 수 없습니다.");

            verify(featureFlagRepository).findByFlagKey(flagKey);
            verify(targetRuleRepository, never()).findByFlagKeyAndTenantId(anyString(), anyLong());
        }

        @Test
        @DisplayName("활성화된 타겟팅 규칙 조회")
        void getActiveTargetRulesByFlagKey_Success() {
            // Given
            String flagKey = "test-feature-flag";
            FeatureFlag featureFlag = TestDataBuilder.featureFlagBuilder().build();
            FeatureFlagTargetRule targetRule = TestDataBuilder.targetRuleBuilder().build();
            List<FeatureFlagTargetRule> targetRules = List.of(targetRule);

            when(featureFlagRepository.findByFlagKey(flagKey)).thenReturn(Optional.of(featureFlag));
            when(targetRuleRepository.findActiveTargetRulesByFlagKey(eq(flagKey), eq(Status.ACTIVE), any(LocalDateTime.class)))
                    .thenReturn(targetRules);

            // When
            List<TargetRuleResponseDto> result = targetingRuleService.getActiveTargetRulesByFlagKey(flagKey);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getIsEnabled()).isTrue();
            verify(targetRuleRepository).findActiveTargetRulesByFlagKey(eq(flagKey), eq(Status.ACTIVE), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("페이징을 통한 타겟팅 규칙 조회")
        void getTargetRulesByFlagKeyWithPagination_Success() {
            // Given
            String flagKey = "test-feature-flag";
            FeatureFlag featureFlag = TestDataBuilder.featureFlagBuilder().build();
            FeatureFlagTargetRule targetRule = TestDataBuilder.targetRuleBuilder().build();
            Page<FeatureFlagTargetRule> targetRulePage = new PageImpl<>(List.of(targetRule));
            Pageable pageable = PageRequest.of(0, 10);

            when(featureFlagRepository.findByFlagKey(flagKey)).thenReturn(Optional.of(featureFlag));
            when(targetRuleRepository.findByFlagKeyWithPagination(flagKey, pageable)).thenReturn(targetRulePage);

            // When
            Page<TargetRuleResponseDto> result = targetingRuleService.getTargetRulesByFlagKeyWithPagination(flagKey, pageable);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(targetRuleRepository).findByFlagKeyWithPagination(flagKey, pageable);
        }

        @Test
        @DisplayName("ID로 타겟팅 규칙 조회")
        void getTargetRuleById_Success() {
            // Given
            Long ruleId = 1L;
            FeatureFlagTargetRule targetRule = TestDataBuilder.targetRuleBuilder().id(ruleId).build();

            when(targetRuleRepository.findByIdAndTenantId(ruleId, 1L)).thenReturn(Optional.of(targetRule));

            // When
            Optional<TargetRuleResponseDto> result = targetingRuleService.getTargetRuleById(ruleId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(ruleId);
            verify(targetRuleRepository).findByIdAndTenantId(ruleId, 1L);
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회 시 빈 Optional 반환")
        void getTargetRuleById_NonExistingId_ReturnsEmpty() {
            // Given
            Long ruleId = 999L;
            when(targetRuleRepository.findByIdAndTenantId(ruleId, 1L)).thenReturn(Optional.empty());

            // When
            Optional<TargetRuleResponseDto> result = targetingRuleService.getTargetRuleById(ruleId);

            // Then
            assertThat(result).isEmpty();
            verify(targetRuleRepository).findByIdAndTenantId(ruleId, 1L);
        }
    }

    @Nested
    @DisplayName("타겟팅 규칙 생성 테스트")
    class CreateTargetRuleTest {

        @Test
        @DisplayName("정상적인 타겟팅 규칙 생성")
        void createTargetRule_Success() {
            // Given
            CreateTargetRuleRequestDto request = TestDataBuilder.createTargetRuleRequestBuilder().build();
            FeatureFlag featureFlag = TestDataBuilder.featureFlagBuilder().build();
            FeatureFlagTargetRule savedRule = TestDataBuilder.targetRuleBuilder().id(1L).build();

            when(featureFlagRepository.findByFlagKey(request.getFlagKey())).thenReturn(Optional.of(featureFlag));
            when(targetRuleRepository.existsByFlagKeyAndRuleNameAndTenantId(
                    request.getFlagKey(), request.getRuleName(), 1L)).thenReturn(false);
            when(targetRuleRepository.save(any(FeatureFlagTargetRule.class))).thenReturn(savedRule);

            // When
            TargetRuleResponseDto result = targetingRuleService.createTargetRule(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getRuleName()).isEqualTo("테스트 타겟팅 규칙");
            verify(targetRuleRepository).save(any(FeatureFlagTargetRule.class));
        }

        @Test
        @DisplayName("존재하지 않는 기능 플래그로 생성 시 예외 발생")
        void createTargetRule_NonExistingFlag_ThrowsException() {
            // Given
            CreateTargetRuleRequestDto request = TestDataBuilder.createTargetRuleRequestBuilder().build();
            when(featureFlagRepository.findByFlagKey(request.getFlagKey())).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> targetingRuleService.createTargetRule(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("기능 플래그를 찾을 수 없습니다.");

            verify(targetRuleRepository, never()).save(any(FeatureFlagTargetRule.class));
        }

        @Test
        @DisplayName("중복된 규칙명으로 생성 시 예외 발생")
        void createTargetRule_DuplicateRuleName_ThrowsException() {
            // Given
            CreateTargetRuleRequestDto request = TestDataBuilder.createTargetRuleRequestBuilder().build();
            FeatureFlag featureFlag = TestDataBuilder.featureFlagBuilder().build();

            when(featureFlagRepository.findByFlagKey(request.getFlagKey())).thenReturn(Optional.of(featureFlag));
            when(targetRuleRepository.existsByFlagKeyAndRuleNameAndTenantId(
                    request.getFlagKey(), request.getRuleName(), 1L)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> targetingRuleService.createTargetRule(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("해당 기능 플래그에 동일한 규칙명이 이미 존재합니다");

            verify(targetRuleRepository, never()).save(any(FeatureFlagTargetRule.class));
        }

        @Test
        @DisplayName("잘못된 롤아웃 비율로 생성 시 예외 발생")
        void createTargetRule_InvalidRolloutPercentage_ThrowsException() {
            // Given
            CreateTargetRuleRequestDto request = TestDataBuilder.invalidRolloutPercentageRequestBuilder().build();
            FeatureFlag featureFlag = TestDataBuilder.featureFlagBuilder().build();

            when(featureFlagRepository.findByFlagKey(request.getFlagKey())).thenReturn(Optional.of(featureFlag));
            when(targetRuleRepository.existsByFlagKeyAndRuleNameAndTenantId(
                    request.getFlagKey(), request.getRuleName(), 1L)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> targetingRuleService.createTargetRule(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("롤아웃 비율은 0-100% 범위 내에서만 설정 가능합니다.");
        }

        @Test
        @DisplayName("잘못된 날짜 범위로 생성 시 예외 발생")
        void createTargetRule_InvalidDateRange_ThrowsException() {
            // Given
            CreateTargetRuleRequestDto request = TestDataBuilder.invalidDateRangeRequestBuilder().build();
            FeatureFlag featureFlag = TestDataBuilder.featureFlagBuilder().build();

            when(featureFlagRepository.findByFlagKey(request.getFlagKey())).thenReturn(Optional.of(featureFlag));
            when(targetRuleRepository.existsByFlagKeyAndRuleNameAndTenantId(
                    request.getFlagKey(), request.getRuleName(), 1L)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> targetingRuleService.createTargetRule(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("시작일은 종료일보다 이전이어야 합니다.");
        }

        @Test
        @DisplayName("잘못된 우선순위로 생성 시 예외 발생")
        void createTargetRule_InvalidPriority_ThrowsException() {
            // Given
            CreateTargetRuleRequestDto request = TestDataBuilder.invalidPriorityRequestBuilder().build();
            FeatureFlag featureFlag = TestDataBuilder.featureFlagBuilder().build();

            when(featureFlagRepository.findByFlagKey(request.getFlagKey())).thenReturn(Optional.of(featureFlag));
            when(targetRuleRepository.existsByFlagKeyAndRuleNameAndTenantId(
                    request.getFlagKey(), request.getRuleName(), 1L)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> targetingRuleService.createTargetRule(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("우선순위는 0-999 범위 내에서만 설정 가능합니다.");
        }
    }

    @Nested
    @DisplayName("타겟팅 규칙 수정 테스트")
    class UpdateTargetRuleTest {

        @Test
        @DisplayName("정상적인 타겟팅 규칙 수정")
        void updateTargetRule_Success() {
            // Given
            Long ruleId = 1L;
            UpdateTargetRuleRequestDto request = TestDataBuilder.updateTargetRuleRequestBuilder().build();
            FeatureFlagTargetRule existingRule = TestDataBuilder.targetRuleBuilder().id(ruleId).build();
            FeatureFlagTargetRule updatedRule = TestDataBuilder.targetRuleBuilder()
                    .id(ruleId)
                    .ruleName("수정된 타겟팅 규칙")
                    .build();

            when(targetRuleRepository.findByIdAndTenantId(ruleId, 1L)).thenReturn(Optional.of(existingRule));
            when(targetRuleRepository.save(any(FeatureFlagTargetRule.class))).thenReturn(updatedRule);

            // When
            TargetRuleResponseDto result = targetingRuleService.updateTargetRule(ruleId, request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(ruleId);
            assertThat(result.getRuleName()).isEqualTo("수정된 타겟팅 규칙");
            verify(targetRuleRepository).save(any(FeatureFlagTargetRule.class));
        }

        @Test
        @DisplayName("존재하지 않는 타겟팅 규칙 수정 시 예외 발생")
        void updateTargetRule_NonExistingRule_ThrowsException() {
            // Given
            Long ruleId = 999L;
            UpdateTargetRuleRequestDto request = TestDataBuilder.updateTargetRuleRequestBuilder().build();

            when(targetRuleRepository.findByIdAndTenantId(ruleId, 1L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> targetingRuleService.updateTargetRule(ruleId, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("타겟팅 규칙을 찾을 수 없습니다.");

            verify(targetRuleRepository, never()).save(any(FeatureFlagTargetRule.class));
        }

        @Test
        @DisplayName("잘못된 롤아웃 비율로 수정 시 예외 발생")
        void updateTargetRule_InvalidRolloutPercentage_ThrowsException() {
            // Given
            Long ruleId = 1L;
            UpdateTargetRuleRequestDto request = TestDataBuilder.updateTargetRuleRequestBuilder()
                    .rolloutPercentage(150) // 잘못된 롤아웃 비율
                    .build();

            // When & Then
            assertThatThrownBy(() -> targetingRuleService.updateTargetRule(ruleId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("롤아웃 비율은 0-100% 범위 내에서만 설정 가능합니다.");
        }
    }

    @Nested
    @DisplayName("타겟팅 규칙 삭제 테스트")
    class DeleteTargetRuleTest {

        @Test
        @DisplayName("정상적인 타겟팅 규칙 삭제")
        void deleteTargetRule_Success() {
            // Given
            Long ruleId = 1L;
            FeatureFlagTargetRule existingRule = TestDataBuilder.targetRuleBuilder().id(ruleId).build();

            when(targetRuleRepository.findByIdAndTenantId(ruleId, 1L)).thenReturn(Optional.of(existingRule));

            // When
            targetingRuleService.deleteTargetRule(ruleId);

            // Then
            verify(targetRuleRepository).delete(existingRule);
        }

        @Test
        @DisplayName("존재하지 않는 타겟팅 규칙 삭제 시 예외 발생")
        void deleteTargetRule_NonExistingRule_ThrowsException() {
            // Given
            Long ruleId = 999L;
            when(targetRuleRepository.findByIdAndTenantId(ruleId, 1L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> targetingRuleService.deleteTargetRule(ruleId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("타겟팅 규칙을 찾을 수 없습니다.");

            verify(targetRuleRepository, never()).delete(any(FeatureFlagTargetRule.class));
        }
    }

    @Nested
    @DisplayName("타겟팅 규칙 토글 테스트")
    class ToggleTargetRuleTest {

        @Test
        @DisplayName("정상적인 타겟팅 규칙 활성화")
        void toggleTargetRule_Enable_Success() {
            // Given
            Long ruleId = 1L;
            boolean enabled = true;
            FeatureFlagTargetRule existingRule = TestDataBuilder.targetRuleBuilder()
                    .id(ruleId)
                    .isEnabled(false)
                    .build();
            FeatureFlagTargetRule toggledRule = TestDataBuilder.targetRuleBuilder()
                    .id(ruleId)
                    .isEnabled(true)
                    .build();

            when(targetRuleRepository.findByIdAndTenantId(ruleId, 1L)).thenReturn(Optional.of(existingRule));
            when(targetRuleRepository.save(any(FeatureFlagTargetRule.class))).thenReturn(toggledRule);

            // When
            TargetRuleResponseDto result = targetingRuleService.toggleTargetRule(ruleId, enabled);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(ruleId);
            assertThat(result.getIsEnabled()).isTrue();
            verify(targetRuleRepository).save(any(FeatureFlagTargetRule.class));
        }

        @Test
        @DisplayName("정상적인 타겟팅 규칙 비활성화")
        void toggleTargetRule_Disable_Success() {
            // Given
            Long ruleId = 1L;
            boolean enabled = false;
            FeatureFlagTargetRule existingRule = TestDataBuilder.targetRuleBuilder()
                    .id(ruleId)
                    .isEnabled(true)
                    .build();
            FeatureFlagTargetRule toggledRule = TestDataBuilder.targetRuleBuilder()
                    .id(ruleId)
                    .isEnabled(false)
                    .build();

            when(targetRuleRepository.findByIdAndTenantId(ruleId, 1L)).thenReturn(Optional.of(existingRule));
            when(targetRuleRepository.save(any(FeatureFlagTargetRule.class))).thenReturn(toggledRule);

            // When
            TargetRuleResponseDto result = targetingRuleService.toggleTargetRule(ruleId, enabled);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(ruleId);
            assertThat(result.getIsEnabled()).isFalse();
            verify(targetRuleRepository).save(any(FeatureFlagTargetRule.class));
        }

        @Test
        @DisplayName("존재하지 않는 타겟팅 규칙 토글 시 예외 발생")
        void toggleTargetRule_NonExistingRule_ThrowsException() {
            // Given
            Long ruleId = 999L;
            boolean enabled = true;
            when(targetRuleRepository.findByIdAndTenantId(ruleId, 1L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> targetingRuleService.toggleTargetRule(ruleId, enabled))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("타겟팅 규칙을 찾을 수 없습니다.");

            verify(targetRuleRepository, never()).save(any(FeatureFlagTargetRule.class));
        }
    }
}
