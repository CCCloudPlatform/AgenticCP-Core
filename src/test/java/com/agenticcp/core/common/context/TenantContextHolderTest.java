package com.agenticcp.core.common.context;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.user.entity.User;
import com.agenticcp.core.domain.user.entity.Worker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * TenantContextHolder 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@DisplayName("TenantContextHolder 단위 테스트")
class TenantContextHolderTest {

    private Tenant testTenant;
    private Worker testWorker;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .username("testuser")
                .build();
        testUser.setId(1L);

        testTenant = Tenant.builder()
                .tenantKey("tenant-1")
                .tenantName("Tenant 1")
                .build();
        testTenant.setId(100L);
        testTenant.setIsDeleted(false);

        testWorker = Worker.builder()
                .workerKey("worker-1")
                .user(testUser)
                .tenant(testTenant)
                .build();
        testWorker.setId(10L);
        testWorker.setIsDeleted(false);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Nested
    @DisplayName("Tenant 컨텍스트 관리 테스트")
    class TenantContextTest {

        @Test
        @DisplayName("Tenant를 설정하고 조회할 수 있어야 한다")
        void setTenant_ThenGetCurrentTenant_ReturnsTenant() {
            // When
            TenantContextHolder.setTenant(testTenant);
            Tenant result = TenantContextHolder.getCurrentTenant();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getTenantKey()).isEqualTo("tenant-1");
        }

        @Test
        @DisplayName("Tenant Key를 설정하고 조회할 수 있어야 한다")
        void setTenantKey_ThenGetCurrentTenantKey_ReturnsTenantKey() {
            // When
            TenantContextHolder.setTenantKey("tenant-1");
            String result = TenantContextHolder.getCurrentTenantKey();

            // Then
            assertThat(result).isEqualTo("tenant-1");
        }

        @Test
        @DisplayName("Tenant가 설정되지 않았으면 getCurrentTenantOrThrow는 예외를 발생시켜야 한다")
        void getCurrentTenantOrThrow_WithoutTenant_ThrowsException() {
            // When & Then
            assertThatThrownBy(TenantContextHolder::getCurrentTenantOrThrow)
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Tenant Key가 설정되지 않았으면 getCurrentTenantKeyOrThrow는 예외를 발생시켜야 한다")
        void getCurrentTenantKeyOrThrow_WithoutTenantKey_ThrowsException() {
            // When & Then
            assertThatThrownBy(TenantContextHolder::getCurrentTenantKeyOrThrow)
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("clear()를 호출하면 Tenant 컨텍스트가 제거되어야 한다")
        void clear_RemovesTenantContext() {
            // Given
            TenantContextHolder.setTenant(testTenant);

            // When
            TenantContextHolder.clear();

            // Then
            assertThat(TenantContextHolder.getCurrentTenant()).isNull();
            assertThat(TenantContextHolder.getCurrentTenantKey()).isNull();
        }

        @Test
        @DisplayName("hasTenantContext()는 Tenant Key가 설정되어 있으면 true를 반환해야 한다")
        void hasTenantContext_WithTenantKey_ReturnsTrue() {
            // Given
            TenantContextHolder.setTenantKey("tenant-1");

            // When
            boolean result = TenantContextHolder.hasTenantContext();

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("hasTenantContext()는 Tenant Key가 설정되지 않았으면 false를 반환해야 한다")
        void hasTenantContext_WithoutTenantKey_ReturnsFalse() {
            // When
            boolean result = TenantContextHolder.hasTenantContext();

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Worker 컨텍스트 관리 테스트")
    class WorkerContextTest {

        @Test
        @DisplayName("Tenant와 Worker를 함께 설정하고 조회할 수 있어야 한다")
        void setCurrentTenantAndWorker_ThenGetCurrentWorker_ReturnsWorker() {
            // When
            TenantContextHolder.setCurrentTenantAndWorker(testTenant, testWorker);
            Worker result = TenantContextHolder.getCurrentWorker();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result.getWorkerKey()).isEqualTo("worker-1");
            assertThat(TenantContextHolder.getCurrentTenant().getId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("Worker가 설정되지 않았으면 getCurrentWorkerOrThrow는 예외를 발생시켜야 한다")
        void getCurrentWorkerOrThrow_WithoutWorker_ThrowsException() {
            // When & Then
            assertThatThrownBy(TenantContextHolder::getCurrentWorkerOrThrow)
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Tenant와 Worker를 함께 조회할 수 있어야 한다")
        void getCurrentTenantAndWorker_ReturnsBoth() {
            // Given
            TenantContextHolder.setCurrentTenantAndWorker(testTenant, testWorker);

            // When
            TenantContextHolder.TenantWorkerContext context = TenantContextHolder.getCurrentTenantAndWorker();

            // Then
            assertThat(context).isNotNull();
            assertThat(context.getTenant()).isNotNull();
            assertThat(context.getWorker()).isNotNull();
            assertThat(context.getTenantId()).isEqualTo(100L);
            assertThat(context.getWorkerId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("Tenant나 Worker가 설정되지 않았으면 getCurrentTenantAndWorkerOrThrow는 예외를 발생시켜야 한다")
        void getCurrentTenantAndWorkerOrThrow_WithoutContext_ThrowsException() {
            // When & Then
            assertThatThrownBy(TenantContextHolder::getCurrentTenantAndWorkerOrThrow)
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("clear()를 호출하면 Worker 컨텍스트도 제거되어야 한다")
        void clear_RemovesWorkerContext() {
            // Given
            TenantContextHolder.setCurrentTenantAndWorker(testTenant, testWorker);

            // When
            TenantContextHolder.clear();

            // Then
            assertThat(TenantContextHolder.getCurrentWorker()).isNull();
        }
    }

    @Nested
    @DisplayName("ThreadLocal 격리 테스트")
    class ThreadLocalIsolationTest {

        @Test
        @DisplayName("다른 스레드에서 설정한 컨텍스트는 현재 스레드에 영향을 주지 않아야 한다")
        void contextIsolation_BetweenThreads_IsIsolated() throws InterruptedException {
            // Given
            TenantContextHolder.setCurrentTenantAndWorker(testTenant, testWorker);

            // When
            Thread otherThread = new Thread(() -> {
                Tenant otherTenant = Tenant.builder()
                        .tenantKey("tenant-2")
                        .build();
                otherTenant.setId(200L);
                TenantContextHolder.setTenant(otherTenant);
            });
            otherThread.start();
            otherThread.join();

            // Then
            Tenant currentTenant = TenantContextHolder.getCurrentTenant();
            assertThat(currentTenant).isNotNull();
            assertThat(currentTenant.getId()).isEqualTo(100L); // 원래 스레드의 값 유지
        }
    }
}

