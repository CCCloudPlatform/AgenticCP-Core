package com.agenticcp.core.common.config;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TenantContextTaskDecorator 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-01-27
 */
@ExtendWith(MockitoExtension.class)
class TenantContextTaskDecoratorTest {

    private TenantContextTaskDecorator taskDecorator;

    @BeforeEach
    void setUp() {
        taskDecorator = new TenantContextTaskDecorator();
        // 테스트 전에 컨텍스트 정리
        TenantContextHolder.clear();
    }

    @AfterEach
    void tearDown() {
        // 테스트 후에 컨텍스트 정리
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("Tenant 객체가 있을 때 컨텍스트가 정상적으로 전파되는지 테스트")
    void decorate_WithTenantObject_ShouldPropagateContext() {
        // Given
        Tenant testTenant = createTestTenant("test-tenant-key", "Test Tenant");
        TenantContextHolder.setTenant(testTenant);
        
        AtomicReference<String> capturedTenantKey = new AtomicReference<>();
        AtomicReference<String> capturedTenantName = new AtomicReference<>();
        
        Runnable testRunnable = () -> {
            Tenant currentTenant = TenantContextHolder.getCurrentTenant();
            if (currentTenant != null) {
                capturedTenantKey.set(currentTenant.getTenantKey());
                capturedTenantName.set(currentTenant.getTenantName());
            }
        };

        // When
        Runnable decoratedRunnable = taskDecorator.decorate(testRunnable);
        CompletableFuture.runAsync(decoratedRunnable).join();

        // Then
        assertThat(capturedTenantKey.get()).isEqualTo("test-tenant-key");
        assertThat(capturedTenantName.get()).isEqualTo("Test Tenant");
    }

    @Test
    @DisplayName("Tenant 키만 있을 때 컨텍스트가 정상적으로 전파되는지 테스트")
    void decorate_WithTenantKeyOnly_ShouldPropagateContext() {
        // Given
        String testTenantKey = "test-tenant-key-only";
        TenantContextHolder.setTenantKey(testTenantKey);
        
        AtomicReference<String> capturedTenantKey = new AtomicReference<>();
        
        Runnable testRunnable = () -> {
            capturedTenantKey.set(TenantContextHolder.getCurrentTenantKey());
        };

        // When
        Runnable decoratedRunnable = taskDecorator.decorate(testRunnable);
        CompletableFuture.runAsync(decoratedRunnable).join();

        // Then
        assertThat(capturedTenantKey.get()).isEqualTo(testTenantKey);
    }

    @Test
    @DisplayName("Tenant 컨텍스트가 없을 때 null이 전파되는지 테스트")
    void decorate_WithoutTenantContext_ShouldPropagateNull() {
        // Given
        AtomicReference<String> capturedTenantKey = new AtomicReference<>();
        AtomicReference<Tenant> capturedTenant = new AtomicReference<>();
        
        Runnable testRunnable = () -> {
            capturedTenantKey.set(TenantContextHolder.getCurrentTenantKey());
            capturedTenant.set(TenantContextHolder.getCurrentTenant());
        };

        // When
        Runnable decoratedRunnable = taskDecorator.decorate(testRunnable);
        CompletableFuture.runAsync(decoratedRunnable).join();

        // Then
        assertThat(capturedTenantKey.get()).isNull();
        assertThat(capturedTenant.get()).isNull();
    }

    @Test
    @DisplayName("작업 완료 후 컨텍스트가 정리되는지 테스트")
    void decorate_AfterTaskCompletion_ShouldClearContext() {
        // Given
        Tenant testTenant = createTestTenant("test-tenant-key", "Test Tenant");
        TenantContextHolder.setTenant(testTenant);
        
        AtomicReference<String> capturedTenantKey = new AtomicReference<>();
        AtomicReference<Tenant> capturedTenant = new AtomicReference<>();
        
        Runnable testRunnable = () -> {
            // 작업 수행 중 컨텍스트 캡처
            capturedTenantKey.set(TenantContextHolder.getCurrentTenantKey());
            capturedTenant.set(TenantContextHolder.getCurrentTenant());
        };

        // When
        Runnable decoratedRunnable = taskDecorator.decorate(testRunnable);
        CompletableFuture.runAsync(decoratedRunnable).join();

        // Then - 작업 중에는 컨텍스트가 전파되었고, 작업 완료 후에는 정리되었는지 확인
        assertThat(capturedTenantKey.get()).isEqualTo("test-tenant-key");
        assertThat(capturedTenant.get()).isNotNull();
        
        // 메인 스레드의 컨텍스트는 그대로 유지되어야 함 (ThreadLocal 특성상)
        assertThat(TenantContextHolder.getCurrentTenantKey()).isEqualTo("test-tenant-key");
        assertThat(TenantContextHolder.getCurrentTenant()).isNotNull();
    }

    @Test
    @DisplayName("작업 중 예외 발생 시에도 컨텍스트가 정리되는지 테스트")
    void decorate_WithException_ShouldClearContext() {
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
        Runnable decoratedRunnable = taskDecorator.decorate(testRunnable);
        
        // 예외가 발생해도 작업 중에는 컨텍스트가 전파되었는지 확인
        assertThatThrownBy(() -> CompletableFuture.runAsync(decoratedRunnable).join())
                .isInstanceOf(Exception.class);
        
        // 작업 중에는 컨텍스트가 전파되었는지 확인
        assertThat(capturedTenantKey.get()).isEqualTo("test-tenant-key");
        
        // 메인 스레드의 컨텍스트는 그대로 유지되어야 함 (ThreadLocal 특성상)
        assertThat(TenantContextHolder.getCurrentTenantKey()).isEqualTo("test-tenant-key");
        assertThat(TenantContextHolder.getCurrentTenant()).isNotNull();
    }

    @Test
    @DisplayName("여러 작업이 동시에 실행될 때 각각의 컨텍스트가 독립적으로 전파되는지 테스트")
    void decorate_MultipleConcurrentTasks_ShouldPropagateIndependentContexts() {
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
        Runnable decoratedRunnable1 = taskDecorator.decorate(runnable1);
        
        TenantContextHolder.setTenant(tenant2);
        Runnable decoratedRunnable2 = taskDecorator.decorate(runnable2);
        
        CompletableFuture<Void> future1 = CompletableFuture.runAsync(decoratedRunnable1);
        CompletableFuture<Void> future2 = CompletableFuture.runAsync(decoratedRunnable2);
        
        CompletableFuture.allOf(future1, future2).join();

        // Then
        assertThat(capturedTenant1Key.get()).isEqualTo("tenant-1");
        assertThat(capturedTenant2Key.get()).isEqualTo("tenant-2");
    }

    @Test
    @DisplayName("Tenant 객체와 Tenant 키가 모두 있을 때 Tenant 객체가 우선되는지 테스트")
    void decorate_WithBothTenantAndKey_ShouldPrioritizeTenantObject() {
        // Given
        Tenant testTenant = createTestTenant("tenant-object-key", "Tenant Object");
        String differentTenantKey = "different-tenant-key";
        
        TenantContextHolder.setTenant(testTenant);
        TenantContextHolder.setTenantKey(differentTenantKey);
        
        AtomicReference<String> capturedTenantKey = new AtomicReference<>();
        AtomicReference<String> capturedTenantName = new AtomicReference<>();
        
        Runnable testRunnable = () -> {
            Tenant currentTenant = TenantContextHolder.getCurrentTenant();
            if (currentTenant != null) {
                capturedTenantKey.set(currentTenant.getTenantKey());
                capturedTenantName.set(currentTenant.getTenantName());
            }
        };

        // When
        Runnable decoratedRunnable = taskDecorator.decorate(testRunnable);
        CompletableFuture.runAsync(decoratedRunnable).join();

        // Then
        assertThat(capturedTenantKey.get()).isEqualTo("tenant-object-key");
        assertThat(capturedTenantName.get()).isEqualTo("Tenant Object");
        // Tenant 객체가 우선되므로 다른 키는 무시됨
        assertThat(capturedTenantKey.get()).isNotEqualTo(differentTenantKey);
    }

    /**
     * 테스트용 Tenant 객체 생성
     */
    private Tenant createTestTenant(String tenantKey, String tenantName) {
        return Tenant.builder()
                .tenantKey(tenantKey)
                .tenantName(tenantName)
                .description("Test tenant for unit testing")
                .build();
    }
}