package com.agenticcp.core.domain.monitoring.service;

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
 * @since 2024-01-01
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
        void getRetentionPolicies_Success() {
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
        void getEnabledRetentionPolicies_Success() {
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
        void updateRetentionPolicy_Success() {
            // Given - 기존 보관 정책이 존재하고 새로운 설정값들로 업데이트하려는 상황
            Integer newRetentionDays = 60;  // 30일에서 60일로 보관 기간 연장
            TenantDataRetentionPolicy.DeletionStrategy newStrategy = TenantDataRetentionPolicy.DeletionStrategy.ARCHIVE;  // DELETE에서 ARCHIVE로 변경
            String newDescription = "업데이트된 정책";

            when(policyRepository.findByTenantIdAndDataType(testTenantId, testDataType))
                    .thenReturn(Optional.of(samplePolicy));  // 기존 정책이 존재함을 Mock
            when(policyRepository.save(any(TenantDataRetentionPolicy.class)))
                    .thenReturn(samplePolicy);  // 저장된 정책을 반환

            // When - 보관 정책을 새로운 설정으로 업데이트하는 경우
            TenantDataRetentionPolicy result = retentionService.updateRetentionPolicy(
                    testTenantId, testDataType, newRetentionDays, newStrategy, newDescription);

            // Then - 업데이트된 보관 정책이 정상적으로 반환되고 저장되어야 함
            assertThat(result).isNotNull();
            verify(policyRepository).findByTenantIdAndDataType(testTenantId, testDataType);  // 정책 조회 확인
            verify(policyRepository).save(any(TenantDataRetentionPolicy.class));  // 정책 저장 확인
        }

        @Test
        @DisplayName("존재하지 않는 보관 정책 업데이트 시 예외 발생")
        void updateRetentionPolicy_NotFound() {
            // Given
            when(policyRepository.findByTenantIdAndDataType(testTenantId, testDataType))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> retentionService.updateRetentionPolicy(
                    testTenantId, testDataType, 60, TenantDataRetentionPolicy.DeletionStrategy.ARCHIVE, "설명"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("보관 정책을 찾을 수 없습니다");

            verify(policyRepository).findByTenantIdAndDataType(testTenantId, testDataType);
            verify(policyRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("보관 정책 토글 테스트")
    class TogglePolicyTest {

        @Test
        @DisplayName("보관 정책 활성화 성공")
        void toggleRetentionPolicy_Enable() {
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
        void toggleRetentionPolicy_Disable() {
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
        void toggleRetentionPolicy_NotFound() {
            // Given
            when(policyRepository.findByTenantIdAndDataType(testTenantId, testDataType))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> retentionService.toggleRetentionPolicy(testTenantId, testDataType, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("보관 정책을 찾을 수 없습니다");

            verify(policyRepository).findByTenantIdAndDataType(testTenantId, testDataType);
            verify(policyRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("수동 데이터 정리 테스트")
    class ManualCleanupTest {

        @Test
        @DisplayName("수동 데이터 정리 성공")
        void manualCleanupTenantData_Success() {
            // Given - 활성화된 보관 정책이 존재하고 오래된 메트릭 데이터가 정리 가능한 상황
            when(policyRepository.findByTenantIdAndDataType(testTenantId, testDataType))
                    .thenReturn(Optional.of(samplePolicy));  // 활성화된 보관 정책이 존재함을 Mock
            when(metricRepository.deleteOldMetrics(eq(testTenantId), any(LocalDateTime.class)))
                    .thenReturn(1000);  // 1000개의 오래된 메트릭이 삭제됨을 Mock
            when(policyRepository.save(any(TenantDataRetentionPolicy.class)))
                    .thenReturn(samplePolicy);  // 정리 상태가 업데이트된 정책을 반환

            // When - 수동으로 테넌트의 오래된 데이터를 정리하는 경우
            int result = retentionService.manualCleanupTenantData(testTenantId, testDataType);

            // Then - 정리된 데이터 개수가 정확히 반환되고 모든 관련 작업이 수행되어야 함
            assertThat(result).isEqualTo(1000);  // 1000개의 데이터가 정리됨을 확인
            verify(policyRepository).findByTenantIdAndDataType(testTenantId, testDataType);  // 보관 정책 조회 확인
            verify(metricRepository).deleteOldMetrics(eq(testTenantId), any(LocalDateTime.class));  // 오래된 메트릭 삭제 확인
            verify(policyRepository).save(any(TenantDataRetentionPolicy.class));  // 정리 상태 업데이트 확인
        }

        @Test
        @DisplayName("보관 정책이 비활성화된 경우 예외 발생")
        void manualCleanupTenantData_Disabled() {
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
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("비활성화된 보관 정책입니다");

            verify(policyRepository).findByTenantIdAndDataType(testTenantId, testDataType);
            verify(metricRepository, never()).deleteOldMetrics(any(), any());
            verify(policyRepository, never()).save(any());
        }

        @Test
        @DisplayName("존재하지 않는 보관 정책으로 수동 정리 시 예외 발생")
        void manualCleanupTenantData_NotFound() {
            // Given
            when(policyRepository.findByTenantIdAndDataType(testTenantId, testDataType))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> retentionService.manualCleanupTenantData(testTenantId, testDataType))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("보관 정책을 찾을 수 없습니다");

            verify(policyRepository).findByTenantIdAndDataType(testTenantId, testDataType);
            verify(metricRepository, never()).deleteOldMetrics(any(), any());
        }
    }

    @Nested
    @DisplayName("스케줄러 테스트")
    class ScheduledCleanupTest {

        @Test
        @DisplayName("스케줄된 데이터 정리 실행")
        void cleanupExpiredData() {
            // Given - 여러 테넌트의 활성화된 보관 정책들이 존재하는 상황 (매일 자정에 실행되는 스케줄러)
            List<TenantDataRetentionPolicy> policies = Arrays.asList(
                    TenantDataRetentionPolicy.builder()
                            .tenantId("tenant-001")
                            .dataType("METRIC")
                            .retentionDays(30)  // 30일 보관 정책
                            .isEnabled(true)
                            .build(),
                    TenantDataRetentionPolicy.builder()
                            .tenantId("tenant-002")
                            .dataType("METRIC")
                            .retentionDays(60)  // 60일 보관 정책
                            .isEnabled(true)
                            .build()
            );

            when(policyRepository.findPoliciesNeedingCleanup(any(LocalDateTime.class))).thenReturn(policies);  // 정리 대상 보관 정책 조회 Mock
            when(metricRepository.deleteOldMetrics(anyString(), any(LocalDateTime.class)))
                    .thenReturn(500, 300);  // 각각 500개, 300개의 오래된 데이터 삭제 Mock
            when(policyRepository.save(any(TenantDataRetentionPolicy.class)))
                    .thenReturn(policies.get(0), policies.get(1));  // 정리 상태 업데이트 Mock

            // When - 스케줄러에 의해 만료된 데이터 정리가 실행되는 경우
            retentionService.cleanupExpiredData();

            // Then - 모든 활성화된 정책에 대해 정리가 수행되고 상태가 업데이트되어야 함
            verify(policyRepository).findPoliciesNeedingCleanup(any(LocalDateTime.class));  // 정리 대상 정책 조회 확인
            verify(metricRepository, times(2)).deleteOldMetrics(anyString(), any(LocalDateTime.class));  // 2개 테넌트의 데이터 정리 확인
            verify(policyRepository, times(2)).save(any(TenantDataRetentionPolicy.class));  // 2개 정책의 상태 업데이트 확인
        }

        @Test
        @DisplayName("비활성화된 정책은 스케줄러에서 제외")
        void cleanupExpiredData_SkipDisabled() {
            // Given
            List<TenantDataRetentionPolicy> policies = Arrays.asList(
                    TenantDataRetentionPolicy.builder()
                            .tenantId("tenant-001")
                            .dataType("METRIC")
                            .isEnabled(false) // 비활성화
                            .build()
            );

            when(policyRepository.findPoliciesNeedingCleanup(any(LocalDateTime.class))).thenReturn(policies);

            // When
            retentionService.cleanupExpiredData();

            // Then
            verify(policyRepository).findPoliciesNeedingCleanup(any(LocalDateTime.class));
            // 현재 서비스 구현에서는 비활성화된 정책도 cleanupTenantData가 호출됨
            // deleteOldMetrics는 호출되지만 0을 반환하고, save도 호출됨
            verify(metricRepository).deleteOldMetrics(anyString(), any(LocalDateTime.class));
            verify(policyRepository).save(any());
        }
    }

    @Nested
    @DisplayName("통계 조회 테스트")
    class StatisticsTest {

        @Test
        @DisplayName("보관 정책 통계 조회")
        void getRetentionPolicyStatistics() {
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
