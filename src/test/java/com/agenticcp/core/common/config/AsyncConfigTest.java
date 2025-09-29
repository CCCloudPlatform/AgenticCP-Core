package com.agenticcp.core.common.config;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AsyncConfig 단위 테스트
 * TaskExecutor 설정과 TenantContextTaskDecorator 사용을 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-01-27
 */
class AsyncConfigTest {

    private AsyncConfig asyncConfig;
    private TaskExecutor tenantTaskExecutor;

    @BeforeEach
    void setUp() {
        asyncConfig = new AsyncConfig();
        tenantTaskExecutor = asyncConfig.tenantTaskExecutor();
        TenantContextHolder.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("tenantTaskExecutor 빈이 올바르게 생성되는지 테스트")
    void tenantTaskExecutor_ShouldBeCreatedCorrectly() {
        // Then
        assertThat(tenantTaskExecutor).isNotNull();
        assertThat(tenantTaskExecutor).isInstanceOf(ThreadPoolTaskExecutor.class);
        
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) tenantTaskExecutor;
        assertThat(executor.getCorePoolSize()).isEqualTo(5);
        assertThat(executor.getMaxPoolSize()).isEqualTo(10);
        assertThat(executor.getQueueCapacity()).isEqualTo(100);
        assertThat(executor.getThreadNamePrefix()).isEqualTo("tenant-async-");
    }

    @Test
    @DisplayName("tenantTaskExecutor에서 Tenant Context가 전파되는지 테스트")
    void tenantTaskExecutor_ShouldPropagateTenantContext() throws Exception {
        // Given
        Tenant testTenant = createTestTenant("test-tenant", "Test Tenant");
        TenantContextHolder.setTenant(testTenant);
        
        AtomicReference<String> capturedTenantKey = new AtomicReference<>();
        
        Runnable testRunnable = () -> {
            Tenant currentTenant = TenantContextHolder.getCurrentTenant();
            if (currentTenant != null) {
                capturedTenantKey.set(currentTenant.getTenantKey());
            }
        };

        // When
        CompletableFuture<Void> future = CompletableFuture.runAsync(testRunnable, tenantTaskExecutor);
        future.get(5, TimeUnit.SECONDS);

        // Then
        assertThat(capturedTenantKey.get()).isEqualTo("test-tenant");
    }

    @Test
    @DisplayName("tenantTaskExecutor에서 Tenant Key만 있을 때 컨텍스트가 전파되는지 테스트")
    void tenantTaskExecutor_WithTenantKeyOnly_ShouldPropagateContext() throws Exception {
        // Given
        String testTenantKey = "test-tenant-key-only";
        TenantContextHolder.setTenantKey(testTenantKey);
        
        AtomicReference<String> capturedTenantKey = new AtomicReference<>();
        
        Runnable testRunnable = () -> {
            capturedTenantKey.set(TenantContextHolder.getCurrentTenantKey());
        };

        // When
        CompletableFuture<Void> future = CompletableFuture.runAsync(testRunnable, tenantTaskExecutor);
        future.get(5, TimeUnit.SECONDS);

        // Then
        assertThat(capturedTenantKey.get()).isEqualTo(testTenantKey);
    }

    @Test
    @DisplayName("tenantTaskExecutor에서 컨텍스트가 없을 때 null이 전파되는지 테스트")
    void tenantTaskExecutor_WithoutTenantContext_ShouldPropagateNull() throws Exception {
        // Given
        AtomicReference<String> capturedTenantKey = new AtomicReference<>();
        AtomicReference<Tenant> capturedTenant = new AtomicReference<>();
        
        Runnable testRunnable = () -> {
            capturedTenantKey.set(TenantContextHolder.getCurrentTenantKey());
            capturedTenant.set(TenantContextHolder.getCurrentTenant());
        };

        // When
        CompletableFuture<Void> future = CompletableFuture.runAsync(testRunnable, tenantTaskExecutor);
        future.get(5, TimeUnit.SECONDS);

        // Then
        assertThat(capturedTenantKey.get()).isNull();
        assertThat(capturedTenant.get()).isNull();
    }

    @Test
    @DisplayName("여러 작업이 동시에 실행될 때 각각의 컨텍스트가 독립적으로 전파되는지 테스트")
    void tenantTaskExecutor_MultipleConcurrentTasks_ShouldPropagateIndependentContexts() throws Exception {
        // Given
        Tenant tenant1 = createTestTenant("tenant-1", "Tenant 1");
        Tenant tenant2 = createTestTenant("tenant-2", "Tenant 2");
        
        AtomicReference<String> capturedTenant1Key = new AtomicReference<>();
        AtomicReference<String> capturedTenant2Key = new AtomicReference<>();
        
        Runnable runnable1 = () -> {
            Tenant currentTenant = TenantContextHolder.getCurrentTenant();
            if (currentTenant != null) {
                capturedTenant1Key.set(currentTenant.getTenantKey());
            }
        };
        
        Runnable runnable2 = () -> {
            Tenant currentTenant = TenantContextHolder.getCurrentTenant();
            if (currentTenant != null) {
                capturedTenant2Key.set(currentTenant.getTenantKey());
            }
        };

        // When
        TenantContextHolder.setTenant(tenant1);
        CompletableFuture<Void> future1 = CompletableFuture.runAsync(runnable1, tenantTaskExecutor);
        
        TenantContextHolder.setTenant(tenant2);
        CompletableFuture<Void> future2 = CompletableFuture.runAsync(runnable2, tenantTaskExecutor);
        
        CompletableFuture.allOf(future1, future2).get(5, TimeUnit.SECONDS);

        // Then
        assertThat(capturedTenant1Key.get()).isEqualTo("tenant-1");
        assertThat(capturedTenant2Key.get()).isEqualTo("tenant-2");
    }

    @Test
    @DisplayName("작업 중 예외 발생 시에도 컨텍스트가 정리되는지 테스트")
    void tenantTaskExecutor_WithException_ShouldClearContext() throws Exception {
        // Given
        Tenant testTenant = createTestTenant("test-tenant-key", "Test Tenant");
        TenantContextHolder.setTenant(testTenant);
        
        AtomicReference<String> capturedTenantKey = new AtomicReference<>();
        
        Runnable testRunnable = () -> {
            // 예외 발생 전에 컨텍스트 캡처
            capturedTenantKey.set(TenantContextHolder.getCurrentTenantKey());
            throw new RuntimeException("Test exception");
        };

        // When & Then
        CompletableFuture<Void> future = CompletableFuture.runAsync(testRunnable, tenantTaskExecutor);
        
        // 예외가 발생해도 작업 중에는 컨텍스트가 전파되었는지 확인
        assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS))
                .isInstanceOf(Exception.class);
        
        // 작업 중에는 컨텍스트가 전파되었는지 확인
        assertThat(capturedTenantKey.get()).isEqualTo("test-tenant-key");
        
        // 메인 스레드의 컨텍스트는 그대로 유지되어야 함 (ThreadLocal 특성상)
        assertThat(TenantContextHolder.getCurrentTenantKey()).isEqualTo("test-tenant-key");
        assertThat(TenantContextHolder.getCurrentTenant()).isNotNull();
    }

    @Test
    @DisplayName("TaskExecutor 스레드 풀 설정이 올바른지 테스트")
    void tenantTaskExecutor_ShouldHaveCorrectThreadPoolSettings() {
        // Given
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) tenantTaskExecutor;

        // When & Then
        assertThat(executor.getCorePoolSize()).isEqualTo(5);
        assertThat(executor.getMaxPoolSize()).isEqualTo(10);
        assertThat(executor.getQueueCapacity()).isEqualTo(100);
        assertThat(executor.getThreadNamePrefix()).isEqualTo("tenant-async-");
    }

    /**
     * 테스트용 Tenant 객체 생성
     */
    private Tenant createTestTenant(String tenantKey, String tenantName) {
        return Tenant.builder()
                .tenantKey(tenantKey)
                .tenantName(tenantName)
                .description("Test tenant for AsyncConfig testing")
                .build();
    }
}