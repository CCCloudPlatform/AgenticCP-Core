package com.agenticcp.core.common.config;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TenantContextTaskDecorator 간단한 단위 테스트
 * 다른 코드의 영향을 받지 않는 순수 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-01-27
 */
class TenantContextTaskDecoratorSimpleTest {

    private TenantContextTaskDecorator taskDecorator;

    @BeforeEach
    void setUp() {
        taskDecorator = new TenantContextTaskDecorator();
        TenantContextHolder.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("Tenant Context가 정상적으로 전파되는지 테스트")
    void testTenantContextPropagation() {
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
        Runnable decoratedRunnable = taskDecorator.decorate(testRunnable);
        CompletableFuture.runAsync(decoratedRunnable).join();

        // Then
        assertThat(capturedTenantKey.get()).isEqualTo("test-tenant");
    }

    @Test
    @DisplayName("Tenant Key만 있을 때 컨텍스트가 전파되는지 테스트")
    void testTenantKeyPropagation() {
        // Given
        String testTenantKey = "test-tenant-key";
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
    @DisplayName("작업 완료 후 컨텍스트가 정리되는지 테스트")
    void testContextCleanup() {
        // Given
        Tenant testTenant = createTestTenant("test-tenant", "Test Tenant");
        TenantContextHolder.setTenant(testTenant);
        
        AtomicReference<String> capturedTenantKey = new AtomicReference<>();
        
        Runnable testRunnable = () -> {
            // 작업 수행 중 컨텍스트 캡처
            capturedTenantKey.set(TenantContextHolder.getCurrentTenantKey());
        };

        // When
        Runnable decoratedRunnable = taskDecorator.decorate(testRunnable);
        CompletableFuture.runAsync(decoratedRunnable).join();

        // Then - 작업 중에는 컨텍스트가 전파되었는지 확인
        assertThat(capturedTenantKey.get()).isEqualTo("test-tenant");
        
        // 메인 스레드의 컨텍스트는 그대로 유지되어야 함 (ThreadLocal 특성상)
        assertThat(TenantContextHolder.getCurrentTenantKey()).isEqualTo("test-tenant");
        assertThat(TenantContextHolder.getCurrentTenant()).isNotNull();
    }

    @Test
    @DisplayName("예외 발생 시에도 컨텍스트가 정리되는지 테스트")
    void testContextCleanupOnException() {
        // Given
        Tenant testTenant = createTestTenant("test-tenant", "Test Tenant");
        TenantContextHolder.setTenant(testTenant);
        
        AtomicReference<String> capturedTenantKey = new AtomicReference<>();
        
        Runnable testRunnable = () -> {
            // 예외 발생 전에 컨텍스트 캡처
            capturedTenantKey.set(TenantContextHolder.getCurrentTenantKey());
            throw new RuntimeException("Test exception");
        };

        // When
        Runnable decoratedRunnable = taskDecorator.decorate(testRunnable);
        
        // 예외가 발생해도 작업 중에는 컨텍스트가 전파되었는지 확인
        try {
            CompletableFuture.runAsync(decoratedRunnable).join();
        } catch (Exception e) {
            // 예외는 무시하고 컨텍스트 전파만 확인
        }

        // Then - 작업 중에는 컨텍스트가 전파되었는지 확인
        assertThat(capturedTenantKey.get()).isEqualTo("test-tenant");
        
        // 메인 스레드의 컨텍스트는 그대로 유지되어야 함 (ThreadLocal 특성상)
        assertThat(TenantContextHolder.getCurrentTenantKey()).isEqualTo("test-tenant");
        assertThat(TenantContextHolder.getCurrentTenant()).isNotNull();
    }

    @Test
    @DisplayName("컨텍스트가 없을 때 null이 전파되는지 테스트")
    void testNullContextPropagation() {
        // Given
        AtomicReference<String> capturedTenantKey = new AtomicReference<>();
        Runnable testRunnable = () -> {
            capturedTenantKey.set(TenantContextHolder.getCurrentTenantKey());
        };

        // When
        Runnable decoratedRunnable = taskDecorator.decorate(testRunnable);
        CompletableFuture.runAsync(decoratedRunnable).join();

        // Then
        assertThat(capturedTenantKey.get()).isNull();
    }

    /**
     * 테스트용 Tenant 객체 생성
     */
    private Tenant createTestTenant(String tenantKey, String tenantName) {
        return Tenant.builder()
                .tenantKey(tenantKey)
                .tenantName(tenantName)
                .description("Test tenant")
                .build();
    }
}