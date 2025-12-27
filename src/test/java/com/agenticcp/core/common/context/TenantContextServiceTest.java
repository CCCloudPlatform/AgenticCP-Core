package com.agenticcp.core.common.context;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.user.entity.User;
import com.agenticcp.core.domain.user.entity.Worker;
import com.agenticcp.core.domain.user.repository.WorkerRepository;
import com.agenticcp.core.domain.tenant.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * TenantContextService 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TenantContextService 단위 테스트")
class TenantContextServiceTest {

    @Mock
    private WorkerRepository workerRepository;

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private TenantContextService tenantContextService;

    private User testUser;
    private Tenant testTenant1;
    private Tenant testTenant2;
    private Worker testWorker1;
    private Worker testWorker2;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .username("testuser")
                .build();
        testUser.setId(1L);

        testTenant1 = Tenant.builder()
                .tenantKey("tenant-1")
                .tenantName("Tenant 1")
                .build();
        testTenant1.setId(100L);
        testTenant1.setIsDeleted(false);

        testTenant2 = Tenant.builder()
                .tenantKey("tenant-2")
                .tenantName("Tenant 2")
                .build();
        testTenant2.setId(200L);
        testTenant2.setIsDeleted(false);

        testWorker1 = Worker.builder()
                .workerKey("worker-1")
                .user(testUser)
                .tenant(testTenant1)
                .build();
        testWorker1.setId(10L);
        testWorker1.setIsDeleted(false);

        testWorker2 = Worker.builder()
                .workerKey("worker-2")
                .user(testUser)
                .tenant(testTenant2)
                .build();
        testWorker2.setId(20L);
        testWorker2.setIsDeleted(false);
    }

    @Nested
    @DisplayName("getAvailableTenantIds 테스트")
    class GetAvailableTenantIdsTest {

        @Test
        @DisplayName("User가 속한 모든 Tenant ID 목록을 반환해야 한다")
        void getAvailableTenantIds_WithMultipleTenants_ReturnsAllTenantIds() {
            // Given
            when(workerRepository.findTenantIdsByUserId(1L))
                    .thenReturn(Arrays.asList(100L, 200L));

            // When
            List<Long> result = tenantContextService.getAvailableTenantIds(1L);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactlyInAnyOrder(100L, 200L);
            verify(workerRepository).findTenantIdsByUserId(1L);
        }

        @Test
        @DisplayName("User가 속한 Tenant가 없으면 빈 목록을 반환해야 한다")
        void getAvailableTenantIds_WithNoTenants_ReturnsEmptyList() {
            // Given
            when(workerRepository.findTenantIdsByUserId(1L))
                    .thenReturn(Collections.emptyList());

            // When
            List<Long> result = tenantContextService.getAvailableTenantIds(1L);

            // Then
            assertThat(result).isEmpty();
            verify(workerRepository).findTenantIdsByUserId(1L);
        }
    }

    @Nested
    @DisplayName("validateTenantAccess 테스트")
    class ValidateTenantAccessTest {

        @Test
        @DisplayName("User가 Tenant에 속하면 Worker를 반환해야 한다")
        void validateTenantAccess_WithValidAccess_ReturnsWorker() {
            // Given
            when(tenantRepository.findById(100L))
                    .thenReturn(Optional.of(testTenant1));
            when(workerRepository.findByUserIdAndTenantIdAndIsDeletedFalse(1L, 100L))
                    .thenReturn(Optional.of(testWorker1));

            // When
            Optional<Worker> result = tenantContextService.validateTenantAccess(1L, 100L);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(10L);
            assertThat(result.get().getTenant().getId()).isEqualTo(100L);
            verify(tenantRepository).findById(100L);
            verify(workerRepository).findByUserIdAndTenantIdAndIsDeletedFalse(1L, 100L);
        }

        @Test
        @DisplayName("Tenant가 존재하지 않으면 빈 Optional을 반환해야 한다")
        void validateTenantAccess_WithNonExistentTenant_ReturnsEmpty() {
            // Given
            when(tenantRepository.findById(999L))
                    .thenReturn(Optional.empty());

            // When
            Optional<Worker> result = tenantContextService.validateTenantAccess(1L, 999L);

            // Then
            assertThat(result).isEmpty();
            verify(tenantRepository).findById(999L);
            verify(workerRepository, never()).findByUserIdAndTenantIdAndIsDeletedFalse(anyLong(), anyLong());
        }

        @Test
        @DisplayName("Tenant가 삭제되었으면 빈 Optional을 반환해야 한다")
        void validateTenantAccess_WithDeletedTenant_ReturnsEmpty() {
            // Given
            Tenant deletedTenant = Tenant.builder()
                    .tenantKey("deleted-tenant")
                    .build();
            deletedTenant.setId(300L);
            deletedTenant.setIsDeleted(true);
            when(tenantRepository.findById(300L))
                    .thenReturn(Optional.of(deletedTenant));

            // When
            Optional<Worker> result = tenantContextService.validateTenantAccess(1L, 300L);

            // Then
            assertThat(result).isEmpty();
            verify(tenantRepository).findById(300L);
            verify(workerRepository, never()).findByUserIdAndTenantIdAndIsDeletedFalse(anyLong(), anyLong());
        }

        @Test
        @DisplayName("User가 Tenant에 속하지 않으면 빈 Optional을 반환해야 한다")
        void validateTenantAccess_WithNoAccess_ReturnsEmpty() {
            // Given
            when(tenantRepository.findById(100L))
                    .thenReturn(Optional.of(testTenant1));
            when(workerRepository.findByUserIdAndTenantIdAndIsDeletedFalse(1L, 100L))
                    .thenReturn(Optional.empty());

            // When
            Optional<Worker> result = tenantContextService.validateTenantAccess(1L, 100L);

            // Then
            assertThat(result).isEmpty();
            verify(tenantRepository).findById(100L);
            verify(workerRepository).findByUserIdAndTenantIdAndIsDeletedFalse(1L, 100L);
        }
    }

    @Nested
    @DisplayName("validateTenantAccessOrThrow 테스트")
    class ValidateTenantAccessOrThrowTest {

        @Test
        @DisplayName("User가 Tenant에 속하면 Worker를 반환해야 한다")
        void validateTenantAccessOrThrow_WithValidAccess_ReturnsWorker() {
            // Given
            when(tenantRepository.findById(100L))
                    .thenReturn(Optional.of(testTenant1));
            when(workerRepository.findByUserIdAndTenantIdAndIsDeletedFalse(1L, 100L))
                    .thenReturn(Optional.of(testWorker1));

            // When
            Worker result = tenantContextService.validateTenantAccessOrThrow(1L, 100L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result.getTenant().getId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("User가 Tenant에 속하지 않으면 예외를 발생시켜야 한다")
        void validateTenantAccessOrThrow_WithNoAccess_ThrowsException() {
            // Given
            when(tenantRepository.findById(100L))
                    .thenReturn(Optional.of(testTenant1));
            when(workerRepository.findByUserIdAndTenantIdAndIsDeletedFalse(1L, 100L))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> tenantContextService.validateTenantAccessOrThrow(1L, 100L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("User is not a member of this tenant");
        }
    }
}

