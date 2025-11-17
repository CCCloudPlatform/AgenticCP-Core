package com.agenticcp.core.domain.monitoring.service;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.monitoring.entity.TenantDataRetentionPolicy;
import com.agenticcp.core.domain.monitoring.repository.MetricRepository;
import com.agenticcp.core.domain.monitoring.repository.TenantDataRetentionPolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * TenantDataRetentionService 서비스 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TenantDataRetentionService 테스트")
class TenantDataRetentionServiceTest {

    @Mock
    private TenantDataRetentionPolicyRepository policyRepository;

    @Mock
    private MetricRepository metricRepository;

    @InjectMocks
    private TenantDataRetentionService retentionService;

    private TenantDataRetentionPolicy samplePolicy;
    private String testTenantId;
    private String testDataType;

    @BeforeEach
    void setUp() {
        testTenantId = "tenant-001";
        testDataType = "METRIC";
        
        samplePolicy = TenantDataRetentionPolicy.builder()
                .tenantId(testTenantId)
                .dataType(testDataType)
                .retentionDays(30)
                .isEnabled(true)
                .deletionStrategy(TenantDataRetentionPolicy.DeletionStrategy.DELETE)
                .priority(50)
                .lastDeletedCount(0L)
                .build();
    }

    @Nested
    @DisplayName("기본 보관 정책 생성 테스트")
    class CreateDefaultPolicyTest {

        @Test
        @DisplayName("기본 보관 정책 생성 성공")
        void createDefaultRetentionPolicy_WhenPolicyNotExists_ReturnsCreatedPolicy() {
            // Given - 테넌트에 기본 보관 정책이 존재하지 않는 상황
            when(policyRepository.findByTenantIdAndDataType(testTenantId, "METRIC"))
                    .thenReturn(Optional.empty());
            when(policyRepository.save(any(TenantDataRetentionPolicy.class)))
                    .thenReturn(samplePolicy);

            // When - 기본 보관 정책을 생성하는 경우
            TenantDataRetentionPolicy result = retentionService.createDefaultRetentionPolicy(testTenantId);

            // Then - 30일 기본 보관 정책이 성공적으로 생성되어야 함
            assertThat(result).isNotNull();
            assertThat(result.getTenantId()).isEqualTo(testTenantId);
            assertThat(result.getDataType()).isEqualTo(testDataType);
            assertThat(result.getRetentionDays()).isEqualTo(30); // 기본 30일
            assertThat(result.getIsEnabled()).isTrue();
            assertThat(result.getDeletionStrategy()).isEqualTo(TenantDataRetentionPolicy.DeletionStrategy.DELETE);

            verify(policyRepository).findByTenantIdAndDataType(testTenantId, "METRIC");
            verify(policyRepository).save(any(TenantDataRetentionPolicy.class));
        }

        @Test
        @DisplayName("이미 존재하는 보관 정책이 있으면 기존 정책 반환")
        void createDefaultRetentionPolicy_WhenPolicyExists_ReturnsExistingPolicy() {
            // Given - 테넌트에 이미 기본 보관 정책이 존재하는 상황
            when(policyRepository.findByTenantIdAndDataType(testTenantId, "METRIC"))
                    .thenReturn(Optional.of(samplePolicy));

            // When - 기본 보관 정책을 생성하려고 하는 경우
            TenantDataRetentionPolicy result = retentionService.createDefaultRetentionPolicy(testTenantId);

            // Then - 기존 정책을 반환하고 새로운 저장은 하지 않아야 함
            assertThat(result).isEqualTo(samplePolicy);

            verify(policyRepository, times(2)).findByTenantIdAndDataType(testTenantId, "METRIC");
            verify(policyRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("보관 정책 조회 테스트")
    class GetPolicyTest {

        @Test
        @DisplayName("보관 정책 조회 성공")
        void getRetentionPolicy_WhenPolicyExists_ReturnsPolicy() {
            // Given - 테넌트와 데이터 타입에 해당하는 보관 정책이 존재하는 상황
            when(policyRepository.findByTenantIdAndDataType(testTenantId, testDataType))
                    .thenReturn(Optional.of(samplePolicy));

            // When - 특정 테넌트의 보관 정책을 조회하는 경우
            TenantDataRetentionPolicy result = retentionService.getRetentionPolicy(testTenantId, testDataType);

            // Then - 해당 보관 정책이 정확히 반환되어야 함
            assertThat(result).isNotNull();
            assertThat(result.getTenantId()).isEqualTo(testTenantId);
            assertThat(result.getDataType()).isEqualTo(testDataType);

            verify(policyRepository).findByTenantIdAndDataType(testTenantId, testDataType);
        }

        @Test
        @DisplayName("보관 정책이 없으면 null 반환")
        void getRetentionPolicy_WhenPolicyNotExists_ReturnsNull() {
            // Given - 테넌트와 데이터 타입에 해당하는 보관 정책이 존재하지 않는 상황
            when(policyRepository.findByTenantIdAndDataType(testTenantId, testDataType))
                    .thenReturn(Optional.empty());

            // When - 존재하지 않는 보관 정책을 조회하는 경우
            TenantDataRetentionPolicy result = retentionService.getRetentionPolicy(testTenantId, testDataType);

            // Then - null이 반환되어야 함
            assertThat(result).isNull();

            verify(policyRepository).findByTenantIdAndDataType(testTenantId, testDataType);
        }

        @Test
        @DisplayName("테넌트별 보관 정책 목록 조회")
        void getRetentionPolicies_WhenCalled_ReturnsPolicyList() {
            // Given
            List<TenantDataRetentionPolicy> policies = Arrays.asList(
                    samplePolicy,
                    TenantDataRetentionPolicy.builder()
                            .tenantId(testTenantId)
                            .dataType("logs")
                            .retentionDays(60)
                            .build()
            );
            when(policyRepository.findByTenantId(testTenantId))
                    .thenReturn(policies);

            // When
            List<TenantDataRetentionPolicy> result = retentionService.getRetentionPolicies(testTenantId);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getDataType()).isEqualTo("METRIC");
            assertThat(result.get(1).getDataType()).isEqualTo("logs");

            verify(policyRepository).findByTenantId(testTenantId);
        }

        @Test
        @DisplayName("활성화된 보관 정책 목록 조회")
        void getEnabledRetentionPolicies_WhenCalled_ReturnsEnabledPolicyList() {
            // Given
            List<TenantDataRetentionPolicy> enabledPolicies = Arrays.asList(samplePolicy);
            when(policyRepository.findEnabledByTenantId(testTenantId))
                    .thenReturn(enabledPolicies);

            // When
            List<TenantDataRetentionPolicy> result = retentionService.getEnabledRetentionPolicies(testTenantId);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getIsEnabled()).isTrue();

            verify(policyRepository).findEnabledByTenantId(testTenantId);
        }
    }

    @Nested
    @DisplayName("보관 정책 업데이트 테스트")
    class UpdatePolicyTest {

        @Test
        @DisplayName("보관 정책 업데이트 성공")
        void updateRetentionPolicy_WhenPolicyExists_ReturnsUpdatedPolicy() {
            // Given
            Integer newRetentionDays = 60;
            TenantDataRetentionPolicy.DeletionStrategy newStrategy = TenantDataRetentionPolicy.DeletionStrategy.ARCHIVE;
            String newDescription = "업데이트된 정책";

            when(policyRepository.findByTenantIdAndDataType(testTenantId, testDataType))
                    .thenReturn(Optional.of(samplePolicy));
            when(policyRepository.save(any(TenantDataRetentionPolicy.class)))
                    .thenReturn(samplePolicy);

            // When
            TenantDataRetentionPolicy result = retentionService.updateRetentionPolicy(
                    testTenantId, testDataType, newRetentionDays, newStrategy, newDescription);

            // Then
            assertThat(result).isNotNull();
            verify(policyRepository).findByTenantIdAndDataType(testTenantId, testDataType);
            verify(policyRepository).save(any(TenantDataRetentionPolicy.class));
        }

        @Test
        @DisplayName("존재하지 않는 보관 정책 업데이트 시 예외 발생")
        void updateRetentionPolicy_WhenPolicyNotExists_ThrowsResourceNotFoundException() {
            // Given
            when(policyRepository.findByTenantIdAndDataType(testTenantId, testDataType))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> retentionService.updateRetentionPolicy(
                    testTenantId, testDataType, 60, TenantDataRetentionPolicy.DeletionStrategy.ARCHIVE, "설명"))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(policyRepository).findByTenantIdAndDataType(testTenantId, testDataType);
            verify(policyRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("보관 정책 토글 테스트")
    class TogglePolicyTest {

        @Test
        @DisplayName("보관 정책 활성화 성공")
        void toggleRetentionPolicy_WhenEnabled_UpdatesPolicy() {
            // Given
            TenantDataRetentionPolicy disabledPolicy = TenantDataRetentionPolicy.builder()
                    .tenantId(testTenantId)
                    .dataType(testDataType)
                    .isEnabled(false)
                    .build();

            when(policyRepository.findByTenantIdAndDataType(testTenantId, testDataType))
                    .thenReturn(Optional.of(disabledPolicy));
            when(policyRepository.save(any(TenantDataRetentionPolicy.class)))
                    .thenReturn(disabledPolicy);

            // When
            retentionService.toggleRetentionPolicy(testTenantId, testDataType, true);

            // Then
            verify(policyRepository).findByTenantIdAndDataType(testTenantId, testDataType);
            verify(policyRepository).save(any(TenantDataRetentionPolicy.class));
        }

        @Test
        @DisplayName("보관 정책 비활성화 성공")
        void toggleRetentionPolicy_WhenDisabled_UpdatesPolicy() {
            // Given
            when(policyRepository.findByTenantIdAndDataType(testTenantId, testDataType))
                    .thenReturn(Optional.of(samplePolicy));
            when(policyRepository.save(any(TenantDataRetentionPolicy.class)))
                    .thenReturn(samplePolicy);

            // When
            retentionService.toggleRetentionPolicy(testTenantId, testDataType, false);

            // Then
            verify(policyRepository).findByTenantIdAndDataType(testTenantId, testDataType);
            verify(policyRepository).save(any(TenantDataRetentionPolicy.class));
        }

        @Test
        @DisplayName("존재하지 않는 보관 정책 토글 시 예외 발생")
        void toggleRetentionPolicy_WhenPolicyNotExists_ThrowsResourceNotFoundException() {
            // Given
            when(policyRepository.findByTenantIdAndDataType(testTenantId, testDataType))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> retentionService.toggleRetentionPolicy(testTenantId, testDataType, true))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(policyRepository).findByTenantIdAndDataType(testTenantId, testDataType);
            verify(policyRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("수동 데이터 정리 테스트")
    class ManualCleanupTest {

        @Test
        @DisplayName("수동 데이터 정리 성공")
        void manualCleanupTenantData_WhenPolicyEnabled_ReturnsCleanedCount() {
            // Given
            when(policyRepository.findByTenantIdAndDataType(testTenantId, testDataType))
                    .thenReturn(Optional.of(samplePolicy));
            when(metricRepository.deleteOldMetrics(eq(testTenantId), any(LocalDateTime.class)))
                    .thenReturn(1000);
            when(policyRepository.save(any(TenantDataRetentionPolicy.class)))
                    .thenReturn(samplePolicy);

            // When
            int result = retentionService.manualCleanupTenantData(testTenantId, testDataType);

            // Then
            assertThat(result).isEqualTo(1000);
            verify(policyRepository).findByTenantIdAndDataType(testTenantId, testDataType);
            verify(metricRepository).deleteOldMetrics(eq(testTenantId), any(LocalDateTime.class));
            verify(policyRepository).save(any(TenantDataRetentionPolicy.class));
        }

        @Test
        @DisplayName("보관 정책이 비활성화된 경우 예외 발생")
        void manualCleanupTenantData_WhenPolicyDisabled_ThrowsBusinessException() {
            // Given
            TenantDataRetentionPolicy disabledPolicy = TenantDataRetentionPolicy.builder()
                    .tenantId(testTenantId)
                    .dataType(testDataType)
                    .isEnabled(false)
                    .retentionDays(30)
                    .build();

            when(policyRepository.findByTenantIdAndDataType(testTenantId, testDataType))
                    .thenReturn(Optional.of(disabledPolicy));

            // When & Then
            assertThatThrownBy(() -> retentionService.manualCleanupTenantData(testTenantId, testDataType))
                    .isInstanceOf(BusinessException.class);

            verify(policyRepository).findByTenantIdAndDataType(testTenantId, testDataType);
            verify(metricRepository, never()).deleteOldMetrics(any(), any());
            verify(policyRepository, never()).save(any());
        }

        @Test
        @DisplayName("존재하지 않는 보관 정책으로 수동 정리 시 예외 발생")
        void manualCleanupTenantData_WhenPolicyNotExists_ThrowsResourceNotFoundException() {
            // Given
            when(policyRepository.findByTenantIdAndDataType(testTenantId, testDataType))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> retentionService.manualCleanupTenantData(testTenantId, testDataType))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(policyRepository).findByTenantIdAndDataType(testTenantId, testDataType);
            verify(metricRepository, never()).deleteOldMetrics(any(), any());
        }
    }

    @Nested
    @DisplayName("스케줄러 테스트")
    class ScheduledCleanupTest {

        @Test
        @DisplayName("스케줄된 데이터 정리 실행")
        void cleanupExpiredData_WhenCalled_CleansExpiredData() {
            // Given
            List<TenantDataRetentionPolicy> policies = Arrays.asList(
                    TenantDataRetentionPolicy.builder()
                            .tenantId("tenant-001")
                            .dataType("METRIC")
                            .retentionDays(30)
                            .isEnabled(true)
                            .build(),
                    TenantDataRetentionPolicy.builder()
                            .tenantId("tenant-002")
                            .dataType("METRIC")
                            .retentionDays(60)
                            .isEnabled(true)
                            .build()
            );

            when(policyRepository.findPoliciesNeedingCleanup(any(LocalDateTime.class))).thenReturn(policies);
            when(metricRepository.deleteOldMetrics(anyString(), any(LocalDateTime.class)))
                    .thenReturn(500, 300);
            when(policyRepository.save(any(TenantDataRetentionPolicy.class)))
                    .thenReturn(policies.get(0), policies.get(1));

            // When
            retentionService.cleanupExpiredData();

            // Then
            verify(policyRepository).findPoliciesNeedingCleanup(any(LocalDateTime.class));
            verify(metricRepository, times(2)).deleteOldMetrics(anyString(), any(LocalDateTime.class));
            verify(policyRepository, times(2)).save(any(TenantDataRetentionPolicy.class));
        }

        @Test
        @DisplayName("비활성화된 정책은 스케줄러에서 제외")
        void cleanupExpiredData_WhenPolicyDisabled_SkipsCleanup() {
            // Given
            List<TenantDataRetentionPolicy> policies = Arrays.asList(
                    TenantDataRetentionPolicy.builder()
                            .tenantId("tenant-001")
                            .dataType("METRIC")
                            .isEnabled(false)
                            .build()
            );

            when(policyRepository.findPoliciesNeedingCleanup(any(LocalDateTime.class))).thenReturn(policies);

            // When
            retentionService.cleanupExpiredData();

            // Then
            verify(policyRepository).findPoliciesNeedingCleanup(any(LocalDateTime.class));
            verify(metricRepository).deleteOldMetrics(anyString(), any(LocalDateTime.class));
            verify(policyRepository).save(any());
        }
    }

    @Nested
    @DisplayName("통계 조회 테스트")
    class StatisticsTest {

        @Test
        @DisplayName("보관 정책 통계 조회")
        void getRetentionPolicyStatistics_WhenCalled_ReturnsStatistics() {
            // Given
            when(policyRepository.count()).thenReturn(10L);
            when(policyRepository.countEnabledPolicies()).thenReturn(8L);

            // When
            TenantDataRetentionService.RetentionPolicyStatistics result = 
                    retentionService.getRetentionPolicyStatistics();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalPolicies()).isEqualTo(10L);
            assertThat(result.getEnabledPolicies()).isEqualTo(8L);
            assertThat(result.getDisabledPolicies()).isEqualTo(2L);

            verify(policyRepository).count();
            verify(policyRepository).countEnabledPolicies();
        }
    }
}
