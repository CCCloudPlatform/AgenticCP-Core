package com.agenticcp.core.domain.platform.service;

import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.platform.cache.service.FeatureFlagSyncService;
import com.agenticcp.core.domain.platform.entity.FeatureFlag;
import com.agenticcp.core.domain.platform.repository.FeatureFlagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FeatureFlagService 단위 테스트
 *
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-17
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FeatureFlagService 단위 테스트")
class FeatureFlagServiceTest {

    @Mock
    private FeatureFlagRepository featureFlagRepository;

    @Mock
    private FeatureFlagAuditService auditService;

    @Mock
    private FeatureFlagPolicyValidator policyValidator;

    @Mock
    private FeatureFlagApprovalService approvalService;

    @Mock
    private FeatureFlagSyncService syncService;

    private FeatureFlagService featureFlagService;

    private FeatureFlag testFlag;

    @BeforeEach
    void setUp() {
        testFlag = FeatureFlag.builder()
                .flagKey("test-feature")
                .flagName("Test Feature")
                .description("Test Description")
                .isEnabled(true)
                .status(Status.ACTIVE)
                .build();
        
        // Optional<FeatureFlagSyncService>로 래핑하여 생성자 주입
        featureFlagService = new FeatureFlagService(
                featureFlagRepository, 
                Optional.of(syncService),
                auditService,
                policyValidator,
                approvalService);

        // Mock 서비스들이 아무것도 하지 않도록 설정 (lenient 모드)
        lenient().doNothing().when(auditService).logFlagChange(any(), any(), anyString(), anyString());
        lenient().doNothing().when(policyValidator).validateFlagChange(any(), anyBoolean());
        lenient().when(approvalService.hasApproval(any(FeatureFlag.class))).thenReturn(false);
    }

    @Nested
    @DisplayName("플래그 생성 테스트")
    class CreateFlagTest {

        @Test
        @DisplayName("플래그 생성 성공 - Redis 활성화 시 이벤트 발행")
        void createFlag_WithRedis_PublishesEvent() {
            // Given
            when(featureFlagRepository.save(any(FeatureFlag.class))).thenReturn(testFlag);
            doNothing().when(syncService).publishCreated(anyString());
            doNothing().when(auditService).logFlagChange(any(), any(), anyString(), anyString());

            // When
            FeatureFlag result = featureFlagService.createFlag(testFlag, "system");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getFlagKey()).isEqualTo("test-feature");
            verify(featureFlagRepository).save(testFlag);
            verify(auditService).logFlagChange(any(), any(), eq("CREATE"), eq("system"));
            verify(syncService).publishCreated("test-feature");
        }

        @Test
        @DisplayName("플래그 생성 성공 - Redis 비활성화 시 이벤트 미발행")
        void createFlag_WithoutRedis_NoEvent() {
            // Given
            when(featureFlagRepository.save(any(FeatureFlag.class))).thenReturn(testFlag);

            // syncService가 Optional.empty()인 경우를 시뮬레이션하기 위해 별도로 서비스 생성
            FeatureFlagService serviceWithoutRedis = new FeatureFlagService(
                    featureFlagRepository, 
                    Optional.empty(),
                    auditService,
                    policyValidator,
                    approvalService);

            doNothing().when(auditService).logFlagChange(any(), any(), anyString(), anyString());

            // When
            FeatureFlag result = serviceWithoutRedis.createFlag(testFlag, "system");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getFlagKey()).isEqualTo("test-feature");
            verify(featureFlagRepository).save(testFlag);
            // syncService가 Optional.empty()이므로 이벤트 발행되지 않음
            verify(syncService, never()).publishCreated(anyString());
        }

        @Test
        @DisplayName("플래그 생성 시 이벤트 발행 실패해도 정상 진행")
        void createFlag_EventPublishFailure_StillSucceeds() {
            // Given
            when(featureFlagRepository.save(any(FeatureFlag.class))).thenReturn(testFlag);
            doThrow(new RuntimeException("Redis connection failed")).when(syncService).publishCreated(anyString());
            doNothing().when(auditService).logFlagChange(any(), any(), anyString(), anyString());

            // When
            FeatureFlag result = featureFlagService.createFlag(testFlag, "system");

            // Then
            assertThat(result).isNotNull();
            verify(featureFlagRepository).save(testFlag);
            verify(auditService).logFlagChange(any(), any(), eq("CREATE"), eq("system"));
            verify(syncService).publishCreated("test-feature");
        }
    }

    @Nested
    @DisplayName("플래그 수정 테스트")
    class UpdateFlagTest {

        @Test
        @DisplayName("플래그 수정 성공 - Redis 활성화 시 이벤트 발행")
        void updateFlag_WithRedis_PublishesEvent() {
            // Given
            FeatureFlag existingFlag = FeatureFlag.builder()
                    .flagKey("test-feature")
                    .flagName("Old Name")
                    .isEnabled(false)
                    .status(Status.ACTIVE)
                    .build();

            FeatureFlag updatedFlag = FeatureFlag.builder()
                    .flagName("New Name")
                    .description("New Description")
                    .isEnabled(true)
                    .status(Status.ACTIVE)
                    .build();

            when(featureFlagRepository.findByFlagKey("test-feature")).thenReturn(Optional.of(existingFlag));
            when(featureFlagRepository.save(any(FeatureFlag.class))).thenReturn(existingFlag);
            doNothing().when(syncService).publishUpdated(anyString());
            when(approvalService.hasApproval(any(FeatureFlag.class))).thenReturn(false);
            doNothing().when(policyValidator).validateFlagChange(any(), anyBoolean());
            doNothing().when(auditService).logFlagChange(any(), any(), anyString(), anyString());

            // When
            FeatureFlag result = featureFlagService.updateFlag("test-feature", updatedFlag, "system");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getFlagName()).isEqualTo("New Name");
            verify(featureFlagRepository).findByFlagKey("test-feature");
            verify(featureFlagRepository).save(existingFlag);
            verify(syncService).publishUpdated("test-feature");
        }

        @Test
        @DisplayName("존재하지 않는 플래그 수정 시 예외 발생")
        void updateFlag_NonExistingFlag_ThrowsException() {
            // Given
            when(featureFlagRepository.findByFlagKey("non-existing")).thenReturn(Optional.empty());

            FeatureFlag updatedFlag = FeatureFlag.builder().flagName("New Name").build();

            // When & Then
            assertThatThrownBy(() -> featureFlagService.updateFlag("non-existing", updatedFlag))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(featureFlagRepository).findByFlagKey("non-existing");
            verify(featureFlagRepository, never()).save(any());
            verify(syncService, never()).publishUpdated(anyString());
        }
    }

    @Nested
    @DisplayName("플래그 토글 테스트")
    class ToggleFlagTest {

        @Test
        @DisplayName("플래그 토글 성공 - Redis 활성화 시 이벤트 발행")
        void toggleFlag_WithRedis_PublishesEvent() {
            // Given
            FeatureFlag flag = FeatureFlag.builder()
                    .flagKey("test-feature")
                    .isEnabled(false)
                    .status(Status.ACTIVE)
                    .build();

            when(featureFlagRepository.findByFlagKey("test-feature")).thenReturn(Optional.of(flag));
            when(featureFlagRepository.save(any(FeatureFlag.class))).thenReturn(flag);
            doNothing().when(syncService).publishToggled(anyString());
            when(approvalService.hasApproval(any(FeatureFlag.class))).thenReturn(false);
            doNothing().when(policyValidator).validateFlagChange(any(), anyBoolean());
            doNothing().when(auditService).logFlagChange(any(), any(), anyString(), anyString());

            // When
            FeatureFlag result = featureFlagService.toggleFlag("test-feature", true, "system");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getIsEnabled()).isTrue();
            verify(featureFlagRepository).findByFlagKey("test-feature");
            verify(featureFlagRepository).save(flag);
            verify(syncService).publishToggled("test-feature");
        }

        @Test
        @DisplayName("존재하지 않는 플래그 토글 시 예외 발생")
        void toggleFlag_NonExistingFlag_ThrowsException() {
            // Given
            when(featureFlagRepository.findByFlagKey("non-existing")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> featureFlagService.toggleFlag("non-existing", true))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(featureFlagRepository).findByFlagKey("non-existing");
            verify(syncService, never()).publishToggled(anyString());
        }
    }

    @Nested
    @DisplayName("플래그 삭제 테스트")
    class DeleteFlagTest {

        @Test
        @DisplayName("플래그 삭제 성공 - Redis 활성화 시 이벤트 발행")
        void deleteFlag_WithRedis_PublishesEvent() {
            // Given
            FeatureFlag flag = FeatureFlag.builder()
                    .flagKey("test-feature")
                    .build();
            flag.setIsDeleted(false);

            when(featureFlagRepository.findByFlagKey("test-feature")).thenReturn(Optional.of(flag));
            when(featureFlagRepository.save(any(FeatureFlag.class))).thenReturn(flag);
            doNothing().when(syncService).publishDeleted(anyString());
            when(approvalService.hasApproval(any(FeatureFlag.class))).thenReturn(false);
            doNothing().when(policyValidator).validateFlagChange(any(), anyBoolean());
            doNothing().when(auditService).logFlagChange(any(), any(), anyString(), anyString());

            // When
            featureFlagService.deleteFlag("test-feature", "system");

            // Then
            assertThat(flag.getIsDeleted()).isTrue();
            verify(featureFlagRepository).findByFlagKey("test-feature");
            verify(featureFlagRepository).save(flag);
            verify(syncService).publishDeleted("test-feature");
        }

        @Test
        @DisplayName("존재하지 않는 플래그 삭제 시 예외 발생")
        void deleteFlag_NonExistingFlag_ThrowsException() {
            // Given
            when(featureFlagRepository.findByFlagKey("non-existing")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> featureFlagService.deleteFlag("non-existing"))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(featureFlagRepository).findByFlagKey("non-existing");
            verify(syncService, never()).publishDeleted(anyString());
        }
    }
}

